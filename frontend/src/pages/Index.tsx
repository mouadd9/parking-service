// pages/Index.tsx - FICHIER COMPLET CORRIGÉ
import { useState, useMemo, useEffect, useRef, useCallback } from "react";
import LeafletMap from "@/components/parking/LeafletMap";
import MapOverlay from "@/components/parking/MapOverlay";
import MapTypeToggle from "@/components/parking/MapTypeToggle";
import ActiveSessionBanner from "@/components/parking/ActiveSessionBanner";
import ParkingModal from "@/components/parking/ParkingModal";
import PaymentSummaryModal from "@/components/parking/PaymentSummaryModal";
import SearchInput from "@/components/parking/SearchInput";
import NavigationTabs from "@/components/parking/NavigationTabs";
import SessionHistoryList from "@/components/parking/SessionHistoryList";
import { useParkingData } from "@/hooks/useParkingData";
import { useParkingZonesFromBackend } from "@/hooks/useParkingZonesFromBackend";
import { GeoJSONFeature } from "@/utils/overpassToGeoJSON";
import { ParkingSpot, ParkingSession } from "@/types/parking";
import { useToast } from "@/hooks/use-toast";
import { parkingApi } from "@/services/parkingApi";
import { Header_Conducteur } from "@/components/Header_Conducteur";
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";

// TODO: Replace with real auth when Clerk is set up
const MOCK_USER_ID = "test-user-1";

// Get hourly rate from parking properties
const getHourlyRate = (parking: GeoJSONFeature | null): number => {
  if (!parking) return 10.0;
  if (parking.properties.hourlyRate) {
    return Number(parking.properties.hourlyRate);
  }
  const access = parking.properties.access;
  if (access === "private") return 12.0;
  if (access === "customers") return 8.0;
  return 10.0;
};

// Types pour les événements WebSocket
interface WebSocketEvent {
  event: string;
  reservationId?: number;
  driverId?: string;
  spotNumber?: string;
  startTime?: string;
  endTime?: string;
  totalCost?: number;
  message?: string;
  status?: string;
}

const Index = () => {
  // ========================================================================
  // STATE MANAGEMENT
  // ========================================================================

  const [activeTab, setActiveTab] = useState<"home" | "history">("home");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedParking, setSelectedParking] = useState<GeoJSONFeature | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [completedSession, setCompletedSession] = useState<ParkingSession | null>(null);
  const [spotsCache, setSpotsCache] = useState<Record<string, ParkingSpot[]>>({});
  const [currentSessionRate, setCurrentSessionRate] = useState(5.0);
  const [mapType, setMapType] = useState<"normal" | "satellite">("normal");
  const [isBooking, setIsBooking] = useState(false);
  const [loadingSpots, setLoadingSpots] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);

  const stompClientRef = useRef<Client | null>(null);
  const { toast } = useToast();
  const { geojson, loading, error } = useParkingZonesFromBackend();
  const { sessions, activeSession, setActiveSession, addSession } = useParkingData();

  // ========================================================================
  // 🔥 SOCKJS/STOMP WEBSOCKET CONNECTION
  // ========================================================================

  const connectWebSocket = useCallback(() => {
    // Seulement si on est dans l'onglet home
    if (activeTab !== "home") return;

    // Nettoyer l'ancienne connexion
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    console.log("🔌 Connecting to SockJS WebSocket...");

    const stompClient = new Client({
      // URL du backend Spring Boot avec SockJS
      webSocketFactory: () => new SockJS("http://localhost:8080/ws"),

      debug: (str) => {
        if (str.includes("ERROR") || str.includes("error")) {
          console.error("🔍 STOMP Error:", str);
        } else if (str.includes("CONNECT") || str.includes("CONNECTED")) {
          console.log("🔍 STOMP Debug:", str);
        }
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        console.log("✅ SockJS WebSocket connected!");
        setWsConnected(true);

        // S'abonner au topic général pour les événements de parking
        stompClient.subscribe("/topic/parking-events", (message) => {
          try {
            const event: WebSocketEvent = JSON.parse(message.body);
            console.log("📡 Parking event received:", event);
            handleWebSocketEvent(event);
          } catch (error) {
            console.error("❌ Error parsing WebSocket message:", error);
          }
        });

        // S'abonner au topic spécifique pour le driver
        stompClient.subscribe(`/topic/driver/${MOCK_USER_ID}`, (message) => {
          try {
            const event: WebSocketEvent = JSON.parse(message.body);
            console.log("📡 Driver-specific event:", event);
            handleWebSocketEvent(event);
          } catch (error) {
            console.error("❌ Error parsing driver message:", error);
          }
        });

        console.log("📡 Subscribed to WebSocket topics");

        toast({
          title: "✅ Connecté en temps réel",
          description: "Notifications des capteurs activées",
          duration: 3000,
        });
      },

      onStompError: (frame) => {
        console.error("❌ STOMP error:", frame.headers["message"]);
        console.error("❌ STOMP body:", frame.body);
        setWsConnected(false);
      },

      onWebSocketError: (error) => {
        console.error("❌ WebSocket error:", error);
        setWsConnected(false);
      },

      onDisconnect: () => {
        console.log("⚠️ WebSocket disconnected");
        setWsConnected(false);
      },
    });

    stompClient.activate();
    stompClientRef.current = stompClient;

    return stompClient;
  }, [activeTab, toast]);

  useEffect(() => {
    // Se connecter au WebSocket
    const client = connectWebSocket();

    // Nettoyer à la déconnexion
    return () => {
      if (client) {
        client.deactivate();
      }
      setWsConnected(false);
    };
  }, [connectWebSocket]);

  // ========================================================================
  // DEBUG: Log activeSession changes
  // ========================================================================

  useEffect(() => {
    console.log("🔄 activeSession changed:", {
      id: activeSession?.id,
      status: activeSession?.status,
      reservationId: activeSession?.reservationId,
      reservationIdType: typeof activeSession?.reservationId,
      startTime: activeSession?.startTime,
      spotNumber: activeSession?.spotNumber
    });
  }, [activeSession]);

  // ========================================================================
  // GESTION DES ÉVÉNEMENTS WEBSOCKET
  // ========================================================================

  const handleWebSocketEvent = useCallback((event: WebSocketEvent) => {
    console.log("🎯 Handling WebSocket event:", event);

    switch (event.event) {
      case "ENTRY_DETECTED":
        handleEntryDetected(event);
        break;
      case "EXIT_DETECTED":
        handleExitDetected(event);
        break;
      default:
        console.log("Unknown event type:", event.event);
    }
  }, [activeSession]);

  const handleEntryDetected = useCallback(async (event: WebSocketEvent) => {
    console.log("🚗 ENTRY DETECTED event received:", event);

    // Debug: afficher tous les détails
    console.log("🔍 Event details:", {
      eventReservationId: event.reservationId,
      eventReservationIdType: typeof event.reservationId,
      eventDriverId: event.driverId,
      eventSpotNumber: event.spotNumber,
      activeSession: activeSession,
      activeSessionStatus: activeSession?.status,
      activeSessionReservationId: activeSession?.reservationId,
      activeSessionReservationIdType: typeof activeSession?.reservationId,
    });

    // Vérifier si nous avons une session active
    if (!activeSession) {
      console.log("❌ No active session found");
      return;
    }

    // Convertir les IDs en nombres pour la comparaison
    const eventReservationId = Number(event.reservationId);
    const sessionReservationId = Number(activeSession.reservationId);

    console.log("🔍 Comparing reservation IDs:", {
      eventReservationId,
      sessionReservationId,
      areEqual: eventReservationId === sessionReservationId,
      activeSessionStatus: activeSession.status
    });

    // Vérifier si l'événement correspond à notre session
    if (activeSession.status === "reserved" &&
        eventReservationId === sessionReservationId) {

      console.log("✅ MATCH! Starting timer...");

      // Utiliser le timestamp de l'événement ou maintenant
      const startTime = event.startTime
        ? new Date(event.startTime)
        : new Date();

      // Créer la session mise à jour
      const updatedSession: ParkingSession = {
        ...activeSession,
        startTime: startTime,
        status: "active" as const,
      };

      console.log("⏱️ Updated session for timer:", updatedSession);

      // Mettre à jour l'état
      setActiveSession(updatedSession);

      // Optionnel: confirmer avec le backend
      if (activeSession.reservationId) {
        try {
          await parkingApi.confirmReservationEntry(
            activeSession.reservationId,
            event.driverId || "UNKNOWN"
          );
          console.log("✅ Entry confirmed with backend");
        } catch (error) {
          console.error("⚠️ Error confirming entry with backend:", error);
        }
      }

      // Afficher la notification
      toast({
        title: "✅ Véhicule détecté",
        description: `Timer démarré pour le spot ${event.spotNumber || activeSession.spotNumber}`,
        duration: 5000,
      });

      // Mettre à jour le statut du spot dans le cache
      if (activeSession.zoneId && activeSession.spotNumber) {
        setSpotsCache(prev => {
          const zoneSpots = prev[activeSession.zoneId] || [];
          const updatedSpots = zoneSpots.map(spot =>
            spot.spotNumber === activeSession.spotNumber
              ? { ...spot, status: "occupied" as const }
              : spot
          );

          return {
            ...prev,
            [activeSession.zoneId]: updatedSpots
          };
        });
      }

    } else if (activeSession.status === "active") {
      console.log("ℹ️ Session already active");
    } else {
      console.log("❌ No match:", {
        reason: activeSession.status !== "reserved"
          ? `Session status is ${activeSession.status}, expected 'reserved'`
          : "Reservation IDs don't match",
        eventReservationId,
        sessionReservationId
      });
    }
  }, [activeSession, setActiveSession, setSpotsCache, toast]);

  const handleExitDetected = useCallback((event: WebSocketEvent) => {
    console.log("🚪 EXIT DETECTED:", event);

    // Vérifier si c'est pour notre session
    if (activeSession &&
        activeSession.status === "active" &&
        Number(event.reservationId) === Number(activeSession.reservationId)) {

      // Compléter la session
      const completed: ParkingSession = {
        ...activeSession,
        endTime: new Date(event.endTime || new Date().toISOString()),
        totalCost: event.totalCost || 0,
        status: "completed" as const,
      };

      // Libérer le spot
      setSpotsCache((prev) => ({
        ...prev,
        [activeSession.zoneId]:
          prev[activeSession.zoneId]?.map((s) =>
            s.spotNumber === activeSession.spotNumber
              ? { ...s, status: "free" as const }
              : s
          ) || [],
      }));

      // Mettre à jour l'état
      addSession(completed);
      setActiveSession(null);
      setCompletedSession(completed);

      toast({
        title: "🎉 Stationnement terminé",
        description: `Coût total: ${(event.totalCost || 0).toFixed(2)}€`,
        duration: 8000,
      });
    } else {
      console.log("❌ Exit event doesn't match current session:", {
        hasActiveSession: !!activeSession,
        sessionStatus: activeSession?.status,
        eventReservationId: event.reservationId,
        sessionReservationId: activeSession?.reservationId,
        match: Number(event.reservationId) === Number(activeSession?.reservationId)
      });
    }
  }, [activeSession, setActiveSession, addSession, setSpotsCache, toast]);

  // ========================================================================
  // SIMULATION POUR LE DÉVELOPPEMENT
  // ========================================================================

  useEffect(() => {
    // Pour le développement : simuler l'entrée avec la touche F8
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.key === 'F8' && activeSession && activeSession.status === 'reserved') {
        console.log("🛠️ DEV: Simulating entry detection");

        const testEvent: WebSocketEvent = {
          event: 'ENTRY_DETECTED',
          reservationId: Number(activeSession.reservationId),
          driverId: MOCK_USER_ID,
          spotNumber: activeSession.spotNumber,
          startTime: new Date().toISOString(),
          status: 'ACTIVE'
        };

        handleEntryDetected(testEvent);
      }

      // Simuler la sortie avec F9
      if (e.key === 'F9' && activeSession && activeSession.status === 'active') {
        console.log("🛠️ DEV: Simulating exit detection");

        const testEvent: WebSocketEvent = {
          event: 'EXIT_DETECTED',
          reservationId: Number(activeSession.reservationId),
          driverId: MOCK_USER_ID,
          spotNumber: activeSession.spotNumber,
          endTime: new Date().toISOString(),
          totalCost: 15.50,
          status: 'COMPLETED'
        };

        handleExitDetected(testEvent);
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [activeSession, handleEntryDetected, handleExitDetected]);

  // ========================================================================
  // FONCTIONS EXISTANTES
  // ========================================================================

  // Filter parkings based on search
  const filteredGeojson = useMemo(() => {
    if (!geojson || !searchQuery.trim()) return geojson;

    return {
      ...geojson,
      features: geojson.features.filter((f) =>
        f.properties.name.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    };
  }, [geojson, searchQuery]);

  // Get spots for selected parking
  const selectedParkingSpots = useMemo(() => {
    if (!selectedParking) return [];
    if (spotsCache[selectedParking.id]) return spotsCache[selectedParking.id];
    return [];
  }, [selectedParking, spotsCache]);

  const handleParkingClick = async (parking: GeoJSONFeature) => {
    setSelectedParking(parking);
    setIsModalOpen(true);

    if (!spotsCache[parking.id]) {
      setLoadingSpots(true);
      try {
        const backendSpots = await parkingApi.getZoneSpots(Number(parking.id));

        const frontendSpots: ParkingSpot[] = backendSpots.map((spot) => ({
          id: String(spot.id),
          spotNumber: spot.spotNumber,
          status: spot.status ? "free" : "occupied",
          zoneId: String(spot.zone.id),
        }));

        setSpotsCache((prev) => ({ ...prev, [parking.id]: frontendSpots }));
      } catch (e) {
        console.error("Failed to fetch spots:", e);
        toast({
          title: "⚠️ No Spots Available",
          description:
            "This parking zone has no spots defined yet. Please add spots in the database first.",
          variant: "destructive",
        });
        setSpotsCache((prev) => ({ ...prev, [parking.id]: [] }));
      } finally {
        setLoadingSpots(false);
      }
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setTimeout(() => setSelectedParking(null), 200);
  };

  const handleBookSpot = async (spot: ParkingSpot, hourlyRate: number) => {
    if (!selectedParking) return;

    setIsBooking(true);

    try {
      const now = new Date();
      const endTime = new Date(now.getTime() + 2 * 60 * 60 * 1000);

      console.log("📝 Creating reservation...");

      const reservation = await parkingApi.createReservation({
        spotId: Number(spot.id),
        driverId: MOCK_USER_ID,
        startTime: now.toISOString(),
        endTime: endTime.toISOString(),
      });

      console.log("✅ Reservation created:", reservation);

      // IMPORTANT: Convertir reservation.id en string pour la cohérence
      const newSession: ParkingSession = {
        id: `session-${Date.now()}`,
        zoneId: selectedParking.id,
        zoneName: selectedParking.properties.name,
        spotNumber: spot.spotNumber,
        spotId: reservation.spotId.toString(),
        reservationId: reservation.id.toString(), // 🔥 Convertir en string
        status: "reserved" as const,
      };

      console.log("📱 New session created:", newSession);

      setActiveSession(newSession);
      setCurrentSessionRate(hourlyRate);

      setSpotsCache((prev) => ({
        ...prev,
        [selectedParking.id]:
          prev[selectedParking.id]?.map((s) =>
            s.id === spot.id ? { ...s, status: "booked" as const } : s
          ) || [],
      }));

      handleCloseModal();

      toast({
        title: "✅ Spot Réservé",
        description: `Spot ${spot.spotNumber} - Roulez jusqu'au parking. Le timer démarrera automatiquement à l'entrée.`,
        duration: 7000,
      });

      // Auto-annulation après 10 minutes
      setTimeout(() => {
        if (activeSession?.reservationId === reservation.id.toString() &&
            activeSession.status === "reserved") {
          handleAutoCancelReservation(reservation.id.toString(), spot.id);
        }
      }, 10 * 60 * 1000);

    } catch (e) {
      console.error("Failed to create reservation:", e);
      toast({
        title: "❌ Échec de réservation",
        description: e instanceof Error ? e.message : "Impossible de réserver le spot. Veuillez réessayer.",
        variant: "destructive",
      });
    } finally {
      setIsBooking(false);
    }
  };

  const handleAutoCancelReservation = async (reservationId: string, spotId: string) => {
    try {
      if (activeSession?.status === "reserved" && activeSession.reservationId === reservationId) {
        await parkingApi.cancelReservation(reservationId);

        setSpotsCache((prev) => ({
          ...prev,
          [selectedParking!.id]: prev[selectedParking!.id]?.map((s) =>
            s.id === spotId ? { ...s, status: "free" as const } : s
          ) || [],
        }));

        setActiveSession(null);

        toast({
          title: "⏰ Réservation expirée",
          description: "Réservation annulée automatiquement après 10 minutes (aucun véhicule détecté).",
        });
      }
    } catch (error) {
      console.error("Failed to auto-cancel reservation:", error);
    }
  };

  const handleCancelReservation = async () => {
    if (!activeSession || !activeSession.reservationId || activeSession.status !== "reserved") return;

    try {
      await parkingApi.cancelReservation(activeSession.reservationId);

      setSpotsCache((prev) => ({
        ...prev,
        [activeSession.zoneId]:
          prev[activeSession.zoneId]?.map((s) =>
            s.spotNumber === activeSession.spotNumber ? { ...s, status: "free" as const } : s
          ) || [],
      }));

      setActiveSession(null);

      toast({
        title: "✅ Réservation annulée",
        description: "Votre réservation a été annulée. Aucun frais appliqué.",
      });
    } catch (e) {
      console.error("Failed to cancel reservation:", e);
      toast({
        title: "❌ Échec d'annulation",
        description: "Impossible d'annuler la réservation. Veuillez réessayer.",
        variant: "destructive",
      });
    }
  };

  // HISTORY TAB
  if (activeTab === "history") {
    return (
      <div className="h-screen w-screen flex flex-col overflow-hidden">
        <Header_Conducteur />
        <div className="flex-1 overflow-auto bg-background">
          <div className="max-w-2xl mx-auto px-4 py-6">
            <div className="mb-6">
              <NavigationTabs activeTab={activeTab} onTabChange={setActiveTab} />
            </div>
            <SessionHistoryList sessions={sessions} />
          </div>
        </div>
      </div>
    );
  }

  // HOME TAB
  return (
    <div className="h-screen w-screen flex flex-col overflow-hidden">
      <Header_Conducteur />

      {/* Indicateur de connexion WebSocket */}
      <div className="absolute top-20 right-4 z-50">
        <div className={`flex items-center gap-2 px-3 py-1 rounded-full text-xs shadow-lg ${
          wsConnected
            ? "bg-green-500/20 text-green-700 border border-green-500/30"
            : "bg-red-500/20 text-red-700 border border-red-500/30"
        }`}>
          <div className={`w-2 h-2 rounded-full ${wsConnected ? "bg-green-500 animate-pulse" : "bg-red-500"}`} />
          {wsConnected ? "Connecté" : "Déconnecté"}
        </div>
      </div>

      <div className="flex-1 relative">
        <LeafletMap
          geojson={filteredGeojson}
          selectedParkingId={selectedParking?.id || null}
          onParkingClick={handleParkingClick}
          loading={loading}
          mapType={mapType}
        />

        <MapOverlay position="top-right">
          <MapTypeToggle mapType={mapType} onToggle={setMapType} />
        </MapOverlay>

        <MapOverlay position="top-left">
          <div className="flex flex-col gap-3 w-72">
            <NavigationTabs activeTab={activeTab} onTabChange={setActiveTab} />
            <SearchInput value={searchQuery} onChange={setSearchQuery} />
            {error && (
              <div className="bg-destructive/10 text-destructive text-xs p-2 rounded">
                {error}
              </div>
            )}
            {geojson && (
              <div className="text-xs text-muted-foreground">
                {filteredGeojson?.features.length || 0} / {geojson.features.length} parkings
              </div>
            )}
          </div>
        </MapOverlay>

        {activeSession && (
          <MapOverlay position="bottom-left">
            <div className="w-80">
              <ActiveSessionBanner
                session={activeSession}
                hourlyRate={currentSessionRate}
                onCancelReservation={
                  activeSession.status === "reserved"
                    ? handleCancelReservation
                    : undefined
                }
              />
            </div>
          </MapOverlay>
        )}

        <ParkingModal
          parking={selectedParking}
          spots={selectedParkingSpots}
          isOpen={isModalOpen}
          onClose={handleCloseModal}
          onBookSpot={handleBookSpot}
          activeSessionSpotId={
            activeSession?.zoneId === selectedParking?.id
              ? selectedParkingSpots.find((s) => s.spotNumber === activeSession.spotNumber)?.id
              : undefined
          }
          hasActiveSession={!!activeSession}
          isBooking={isBooking || loadingSpots}
        />

        {completedSession && (
          <PaymentSummaryModal
            session={completedSession}
            isOpen={!!completedSession}
            onClose={() => setCompletedSession(null)}
          />
        )}

        {/* Instructions de test */}
        {process.env.NODE_ENV === 'development' && activeSession && (
          <div className="absolute bottom-4 right-4 z-50 bg-white/90 backdrop-blur-sm p-3 rounded-lg shadow-lg border max-w-sm">
            <h3 className="font-bold text-sm mb-1">🛠️ Dev Tools</h3>
            <p className="text-xs text-gray-600 mb-2">
              Simuler les événements de capteur:
            </p>
            <div className="space-y-1">
              {activeSession.status === "reserved" && (
                <p className="text-xs">
                  <span className="font-medium">F8</span> → Simuler entrée
                </p>
              )}
              {activeSession.status === "active" && (
                <p className="text-xs">
                  <span className="font-medium">F9</span> → Simuler sortie
                </p>
              )}
              <p className="text-xs text-gray-500 mt-1">
                Session: {activeSession.reservationId}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default Index;