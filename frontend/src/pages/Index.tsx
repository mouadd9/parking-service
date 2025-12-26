import { useState, useMemo } from "react";
import LeafletMap from "@/components/parking/LeafletMap";
import MapOverlay from "@/components/parking/MapOverlay";
import MapTypeToggle from "@/components/parking/MapTypeToggle";
import ActiveSessionBanner from "@/components/parking/ActiveSessionBanner";
import ParkingModal from "@/components/parking/ParkingModal";
import StopSessionModal from "@/components/parking/StopSessionModal";
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

// TODO: Replace with real auth when Clerk is set up
const MOCK_USER_ID = "test-user-1";

// Get hourly rate from parking properties (now from backend)
const getHourlyRate = (parking: GeoJSONFeature | null): number => {
  if (!parking) return 10.0;
  // Use the hourly rate from the backend if available
  if (parking.properties.hourlyRate) {
    return Number(parking.properties.hourlyRate);
  }
  // Fallback to access-based pricing
  const access = parking.properties.access;
  if (access === "private") return 12.0;
  if (access === "customers") return 8.0;
  return 10.0;
};

const Index = () => {
  const [activeTab, setActiveTab] = useState<"home" | "history">("home");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedParking, setSelectedParking] = useState<GeoJSONFeature | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isStopModalOpen, setIsStopModalOpen] = useState(false);
  const [isStoppingSession, setIsStoppingSession] = useState(false);
  const [completedSession, setCompletedSession] = useState<ParkingSession | null>(null);
  const [spotsCache, setSpotsCache] = useState<Record<string, ParkingSpot[]>>({});
  const [currentSessionRate, setCurrentSessionRate] = useState(5.0);
  const [mapType, setMapType] = useState<"normal" | "satellite">("normal");
  const [isBooking, setIsBooking] = useState(false);
  const [loadingSpots, setLoadingSpots] = useState(false);

  // State for sensor-based workflow
  const [pendingSpot, setPendingSpot] = useState<{
    spot: ParkingSpot;
    parkingId: string;
    hourlyRate: number;
  } | null>(null);

  const { toast } = useToast();
  const { geojson, loading, error } = useParkingZonesFromBackend();
  const { sessions, activeSession, setActiveSession, addSession } = useParkingData();

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

  // Get spots for selected parking from backend
  const selectedParkingSpots = useMemo(() => {
    if (!selectedParking) return [];
    if (spotsCache[selectedParking.id]) return spotsCache[selectedParking.id];
    return [];
  }, [selectedParking, spotsCache]);

  const handleParkingClick = async (parking: GeoJSONFeature) => {
    setSelectedParking(parking);
    setIsModalOpen(true);

    // Fetch real spots from backend if not cached
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

      const reservation = await parkingApi.createReservation({
        spotId: Number(spot.id),
        driverId: MOCK_USER_ID,
        startTime: now.toISOString(),
        endTime: endTime.toISOString(),
      });

      const newSession: ParkingSession = {
        id: `session-${Date.now()}`,
        zoneId: selectedParking.id,
        zoneName: selectedParking.properties.name,
        spotNumber: spot.spotNumber,
        spotId: reservation.spotId,
        reservationId: reservation.id,
        status: "reserved",
      };

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
        title: "✅ Spot Reserved",
        description: `Spot ${spot.spotNumber} - Drive to the parking. Timer will start when Node-RED detects your entry.`,
      });
    } catch (e) {
      console.error("Failed to create reservation:", e);
      toast({
        title: "❌ Reservation Failed",
        description: e instanceof Error ? e.message : "Could not reserve spot. Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsBooking(false);
    }
  };

  const handleSensorEntryDetected = (matricule?: string) => {
    if (!activeSession || activeSession.status !== "reserved") return;

    setActiveSession({
      ...activeSession,
      startTime: new Date(),
      status: "active",
    });

    toast({
      title: "Timer Started",
      description: matricule
        ? `Vehicle ${matricule} detected - parking timer started`
        : "Vehicle detected - parking timer started",
    });
  };

  const handleStopClick = () => {
    setIsStopModalOpen(true);
  };

  const handleConfirmStop = async () => {
    if (!activeSession) return;

    setIsStoppingSession(true);

    // Cancel reserved session (no start)
    if (activeSession.status === "reserved" || !activeSession.startTime) {
      try {
        if (!activeSession.reservationId) {
          throw new Error("Missing reservation id. Please refresh and try again.");
        }

        await parkingApi.cancelReservation(activeSession.reservationId);

        setSpotsCache((prev) => ({
          ...prev,
          [activeSession.zoneId]:
            prev[activeSession.zoneId]?.map((s) =>
              s.spotNumber === activeSession.spotNumber ? { ...s, status: "free" as const } : s
            ) || [],
        }));

        setActiveSession(null);
        setIsStopModalOpen(false);

        toast({
          title: "Reservation Canceled",
          description: "Your spot reservation has been canceled. No charges applied.",
        });
      } catch (e) {
        console.error("Failed to cancel reservation:", e);
        toast({
          title: "❌ Cancel Failed",
          description: e instanceof Error ? e.message : "Could not cancel reservation. Please try again.",
          variant: "destructive",
        });
      } finally {
        setIsStoppingSession(false);
      }
      return;
    }

    const endTime = new Date();
    const durationHours = (endTime.getTime() - activeSession.startTime.getTime()) / 3600000;
    const totalCost = Math.max(durationHours * currentSessionRate, currentSessionRate);

    const completed: ParkingSession = {
      ...activeSession,
      endTime,
      totalCost,
      status: "completed",
    };

    setSpotsCache((prev) => ({
      ...prev,
      [activeSession.zoneId]:
        prev[activeSession.zoneId]?.map((s) =>
          s.spotNumber === activeSession.spotNumber ? { ...s, status: "free" as const } : s
        ) || [],
    }));

    addSession(completed);
    setActiveSession(null);
    setIsStopModalOpen(false);
    setIsStoppingSession(false);
    setCompletedSession(completed);
  };

  const handleSensorExitDetected = async () => {
    await handleConfirmStop();
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
                {geojson.features.length} parkings found
              </div>
            )}
          </div>
        </MapOverlay>

        {activeSession && (
          <MapOverlay position="bottom-left">
            <div className="w-80">
              <ActiveSessionBanner
                session={activeSession}
                onStopSession={handleStopClick}
                isStoppingSession={isStoppingSession}
                hourlyRate={currentSessionRate}
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

        {activeSession && (
          <StopSessionModal
            session={activeSession}
            isOpen={isStopModalOpen}
            onClose={() => setIsStopModalOpen(false)}
            onConfirm={handleConfirmStop}
            isLoading={isStoppingSession}
            hourlyRate={currentSessionRate}
          />
        )}

        {completedSession && (
          <PaymentSummaryModal
            session={completedSession}
            isOpen={!!completedSession}
            onClose={() => setCompletedSession(null)}
          />
        )}
      </div>
    </div>
  );
};

export default Index;
