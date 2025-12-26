import { useState, useMemo } from 'react';
import LeafletMap from '@/components/parking/LeafletMap';
import MapOverlay from '@/components/parking/MapOverlay';
import MapTypeToggle from '@/components/parking/MapTypeToggle';
import ActiveSessionBanner from '@/components/parking/ActiveSessionBanner';
import ParkingModal from '@/components/parking/ParkingModal';
import StopSessionModal from '@/components/parking/StopSessionModal';
import PaymentSummaryModal from '@/components/parking/PaymentSummaryModal';
import SearchInput from '@/components/parking/SearchInput';
import NavigationTabs from '@/components/parking/NavigationTabs';
import SessionHistoryList from '@/components/parking/SessionHistoryList';
import { useParkingData } from '@/hooks/useParkingData';
import { useParkingZonesFromBackend } from '@/hooks/useParkingZonesFromBackend';
import { GeoJSONFeature } from '@/utils/overpassToGeoJSON';
import { ParkingSpot, ParkingSession } from '@/types/parking';
import { useToast } from '@/hooks/use-toast';
import { parkingApi } from '@/services/parkingApi';

// TODO: Replace with real auth when Clerk is set up
const MOCK_USER_ID = "test-user-1";

// Get hourly rate from parking properties (now from backend)
const getHourlyRate = (parking: GeoJSONFeature | null): number => {
  if (!parking) return 10.00;
  // Use the hourly rate from the backend if available
  if (parking.properties.hourlyRate) {
    return Number(parking.properties.hourlyRate);
  }
  // Fallback to access-based pricing
  const access = parking.properties.access;
  if (access === 'private') return 12.00;
  if (access === 'customers') return 8.00;
  return 10.00;
};

const Index = () => {
  const [activeTab, setActiveTab] = useState<'home' | 'history'>('home');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedParking, setSelectedParking] = useState<GeoJSONFeature | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isStopModalOpen, setIsStopModalOpen] = useState(false);
  const [isStoppingSession, setIsStoppingSession] = useState(false);
  const [completedSession, setCompletedSession] = useState<ParkingSession | null>(null);
  const [spotsCache, setSpotsCache] = useState<Record<string, ParkingSpot[]>>({});
  const [currentSessionRate, setCurrentSessionRate] = useState(5.00);
  const [mapType, setMapType] = useState<'normal' | 'satellite'>('normal');
  const [isBooking, setIsBooking] = useState(false);
  const [loadingSpots, setLoadingSpots] = useState(false);

  // State for sensor-based workflow
  // When user selects a spot, we set it as "pending" until sensor detects the vehicle
  const [pendingSpot, setPendingSpot] = useState<{ spot: ParkingSpot; parkingId: string; hourlyRate: number } | null>(null);

  const { toast } = useToast();
  const { geojson, loading, error } = useParkingZonesFromBackend();
  const {
    sessions,
    activeSession,
    setActiveSession,
    addSession,
  } = useParkingData();

  // Filter parkings based on search
  const filteredGeojson = useMemo(() => {
    if (!geojson || !searchQuery.trim()) return geojson;
    
    return {
      ...geojson,
      features: geojson.features.filter(f => 
        f.properties.name.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    };
  }, [geojson, searchQuery]);

  // Get spots for selected parking from backend
  const selectedParkingSpots = useMemo(() => {
    if (!selectedParking) return [];

    // Return cached spots if available
    if (spotsCache[selectedParking.id]) {
      return spotsCache[selectedParking.id];
    }

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

        // Convert backend spots to frontend format
        const frontendSpots: ParkingSpot[] = backendSpots.map(spot => ({
          id: String(spot.id),
          spotNumber: spot.spotNumber,
          status: spot.status ? 'free' : 'occupied',
          zoneId: String(spot.zone.id),
        }));

        setSpotsCache(prev => ({ ...prev, [parking.id]: frontendSpots }));
      } catch (error) {
        console.error('Failed to fetch spots:', error);
        toast({
          title: "⚠️ No Spots Available",
          description: "This parking zone has no spots defined yet. Please add spots in the database first.",
          variant: "destructive",
        });
        // Set empty array to prevent infinite loading
        setSpotsCache(prev => ({ ...prev, [parking.id]: [] }));
      } finally {
        setLoadingSpots(false);
      }
    }
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setTimeout(() => setSelectedParking(null), 200);
  };

  /**
   * SENSOR INTEGRATION POINT - ENTRY DETECTION
   * 
   * Current flow (mock): User selects spot → Session starts immediately
   * 
   * Production flow with sensors:
   * 1. User selects spot → Set pendingSpot state (spot is now "reserved")
   * 2. Wait for sensor API to detect vehicle matricule at that spot
   * 3. Sensor API endpoint: POST /api/sensors/entry-detection
   *    Request body: { spotId: string, matricule: string, parkingId: string }
   * 4. On sensor detection → Call handleSensorEntryDetected() to start timer
   * 
   * Example WebSocket listener for real-time sensor updates:
   * useEffect(() => {
   *   const ws = new WebSocket('wss://your-api.com/sensors');
   *   ws.onmessage = (event) => {
   *     const data = JSON.parse(event.data);
   *     if (data.type === 'ENTRY_DETECTED' && data.spotId === pendingSpot?.spot.id) {
   *       handleSensorEntryDetected(data.matricule);
   *     }
   *   };
   *   return () => ws.close();
   * }, [pendingSpot]);
   */
  const handleBookSpot = async (spot: ParkingSpot, hourlyRate: number) => {
    if (!selectedParking) return;

    setIsBooking(true);

    try {
      /**
       * BACKEND INTEGRATION - CREATE RESERVATION
       *
       * Creates a reservation in PENDING status.
       * Timer does NOT start yet - only when Node-RED sensor detects entry.
       *
       * POST /api/reservations/create
       * Creates reservation with status: PENDING
       */

      const now = new Date();
      const endTime = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 2 hours from now

      const reservation = await parkingApi.createReservation({
        spotId: Number(spot.id), // Use real spot ID from backend
        driverId: MOCK_USER_ID, // Using mock user ID until auth is set up
        startTime: now.toISOString(),
        endTime: endTime.toISOString(),
      });

      // Create frontend session in "reserved" state - timer NOT started yet
      const newSession: ParkingSession = {
        id: `session-${Date.now()}`,
        zoneId: selectedParking.id,
        zoneName: selectedParking.properties.name,
        spotNumber: spot.spotNumber,
        spotId: reservation.spotId,
        reservationId: reservation.id,
        status: 'reserved', // Waiting for Node-RED sensor to detect entry
      };

      setActiveSession(newSession);
      setCurrentSessionRate(hourlyRate);

      // Update spot status to booked (reserved)
      setSpotsCache(prev => ({
        ...prev,
        [selectedParking.id]: prev[selectedParking.id]?.map(s =>
          s.id === spot.id ? { ...s, status: 'booked' as const } : s
        ) || [],
      }));

      handleCloseModal();

      toast({
        title: "✅ Spot Reserved",
        description: `Spot ${spot.spotNumber} - Drive to the parking. Timer will start when Node-RED detects your entry.`,
      });

    } catch (error) {
      console.error('Failed to create reservation:', error);
      toast({
        title: "❌ Reservation Failed",
        description: error instanceof Error ? error.message : "Could not reserve spot. Please try again.",
        variant: "destructive",
      });
    } finally {
      setIsBooking(false);
    }
  };

  /**
   * SENSOR INTEGRATION POINT - ENTRY DETECTION CALLBACK
   * 
   * This function should be called when the parking sensor detects the vehicle entering.
   * It transitions the session from "reserved" to "active" and starts the timer.
   * 
   * Backend API endpoint that triggers this:
   * POST /api/sensors/entry-detected
   * Request body: { spotId: string, matricule: string, parkingId: string, sessionId: string }
   * 
   * WebSocket integration example:
   * useEffect(() => {
   *   if (!activeSession || activeSession.status !== 'reserved') return;
   *   
   *   const ws = new WebSocket('wss://your-api.com/sensors/events');
   *   ws.onmessage = (event) => {
   *     const data = JSON.parse(event.data);
   *     if (data.type === 'ENTRY_DETECTED' && data.sessionId === activeSession.id) {
   *       handleSensorEntryDetected(data.matricule);
   *     }
   *   };
   *   return () => ws.close();
   * }, [activeSession]);
   * 
   * @param matricule - The vehicle's license plate detected by the sensor
   */
  const handleSensorEntryDetected = (matricule?: string) => {
    if (!activeSession || activeSession.status !== 'reserved') return;
    
    // Start the timer NOW - sensor has detected the vehicle
    setActiveSession({
      ...activeSession,
      startTime: new Date(),
      status: 'active',
    });
    
    toast({
      title: "Timer Started",
      description: matricule 
        ? `Vehicle ${matricule} detected - parking timer started`
        : "Vehicle detected - parking timer started",
    });
  };

  // TODO: Remove this mock - For demo only, simulate entry after 3 seconds
  // In production, this would be triggered by the sensor WebSocket
  // useEffect(() => {
  //   if (activeSession?.status === 'reserved') {
  //     const timeout = setTimeout(() => handleSensorEntryDetected(), 3000);
  //     return () => clearTimeout(timeout);
  //   }
  // }, [activeSession?.status]);

  const handleStopClick = () => {
    setIsStopModalOpen(true);
  };

  /**
   * SENSOR INTEGRATION POINT - EXIT DETECTION
   * 
   * Current flow (mock): User clicks "Stop" → Session ends immediately
   * 
   * Production flow with sensors:
   * 1. Parking sensor detects vehicle leaving the spot
   * 2. Sensor API endpoint: POST /api/sensors/exit-detection
   *    Request body: { spotId: string, matricule: string, parkingId: string }
   * 3. Backend validates and calls our webhook/callback
   * 4. Frontend receives WebSocket message or polls for status change
   * 5. handleSensorExitDetected() is called automatically → Stops timer & processes payment
   * 
   * Example WebSocket listener for real-time sensor updates:
   * useEffect(() => {
   *   if (!activeSession) return;
   *   const ws = new WebSocket('wss://your-api.com/sensors');
   *   ws.onmessage = (event) => {
   *     const data = JSON.parse(event.data);
   *     if (data.type === 'EXIT_DETECTED' && data.spotId === activeSession.spotId) {
   *       handleSensorExitDetected();
   *     }
   *   };
   *   return () => ws.close();
   * }, [activeSession]);
   */
  const handleConfirmStop = async () => {
    if (!activeSession) return;
    
    setIsStoppingSession(true);
    
    // Handle cancellation of reserved spot (user hasn't entered yet)
    if (activeSession.status === 'reserved' || !activeSession.startTime) {
      /**
       * SENSOR INTEGRATION NOTE:
       * User is canceling their reservation before entering the parking.
       * No charges apply.
       * Backend API: POST /api/spots/cancel-reservation
       * Request body: { sessionId: string, spotId: string }
       */
      
      try {
        if (!activeSession.reservationId) {
          throw new Error('Missing reservation id. Please refresh and try again.');
        }

        await parkingApi.cancelReservation(activeSession.reservationId);

        // Update spot status back to free
        setSpotsCache(prev => ({
          ...prev,
          [activeSession.zoneId]: prev[activeSession.zoneId]?.map(s =>
            s.spotNumber === activeSession.spotNumber ? { ...s, status: 'free' as const } : s
          ) || [],
        }));

        setActiveSession(null);
        setIsStopModalOpen(false);

        toast({
          title: "Reservation Canceled",
          description: "Your spot reservation has been canceled. No charges applied.",
        });
      } catch (error) {
        console.error('Failed to cancel reservation:', error);
        toast({
          title: "❌ Cancel Failed",
          description: error instanceof Error ? error.message : "Could not cancel reservation. Please try again.",
          variant: "destructive",
        });
      } finally {
        setIsStoppingSession(false);
      }

      return;
    }
    
    /**
     * SENSOR INTEGRATION NOTE:
     * In production, this is triggered when the sensor detects the vehicle leaving.
     * The endTime comes from the sensor detection timestamp.
     * Backend API: POST /api/sensors/exit-detected
     */
    const endTime = new Date(); // In production: timestamp from sensor detection
    const durationHours = (endTime.getTime() - activeSession.startTime.getTime()) / 3600000;
    const totalCost = Math.max(durationHours * currentSessionRate, currentSessionRate);
    
    const completed: ParkingSession = {
      ...activeSession,
      endTime,
      totalCost,
      status: 'completed',
    };
    
    // Update spot status back to free
    setSpotsCache(prev => ({
      ...prev,
      [activeSession.zoneId]: prev[activeSession.zoneId]?.map(s =>
        s.spotNumber === activeSession.spotNumber ? { ...s, status: 'free' as const } : s
      ) || [],
    }));
    
    /**
     * SENSOR INTEGRATION NOTE:
     * In production, payment is processed automatically here.
     * Backend API: POST /api/payments/charge
     * Request body: { sessionId: string, amount: number, userId: string }
     */
    
    addSession(completed);
    setActiveSession(null);
    setIsStopModalOpen(false);
    setIsStoppingSession(false);
    setCompletedSession(completed);
  };

  /**
   * SENSOR INTEGRATION POINT - EXIT DETECTION CALLBACK
   * 
   * This function is called when the parking sensor detects the vehicle leaving.
   * In production, this automatically triggers the payment process.
   * 
   * Backend API endpoint that triggers this:
   * POST /api/sensors/exit-detected
   * Request body: { spotId: string, matricule: string, parkingId: string, sessionId: string }
   * 
   * WebSocket integration example:
   * useEffect(() => {
   *   if (!activeSession || activeSession.status !== 'active') return;
   *   
   *   const ws = new WebSocket('wss://your-api.com/sensors/events');
   *   ws.onmessage = (event) => {
   *     const data = JSON.parse(event.data);
   *     if (data.type === 'EXIT_DETECTED' && data.sessionId === activeSession.id) {
   *       handleSensorExitDetected();
   *     }
   *   };
   *   return () => ws.close();
   * }, [activeSession]);
   */
  const handleSensorExitDetected = async () => {
    // Automatically stop the session and process payment when sensor detects vehicle leaving
    await handleConfirmStop();
  };

  if (activeTab === 'history') {
    return (
      <div className="min-h-screen bg-background">
        <div className="max-w-2xl mx-auto px-4 py-6">
          <div className="mb-6">
            <NavigationTabs activeTab={activeTab} onTabChange={setActiveTab} />
          </div>
          <SessionHistoryList sessions={sessions} />
        </div>
      </div>
    );
  }

  return (
    <div className="h-screen w-screen overflow-hidden relative">
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

      {/* Parking Slots Modal */}
      <ParkingModal
        parking={selectedParking}
        spots={selectedParkingSpots}
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        onBookSpot={handleBookSpot}
        activeSessionSpotId={
          activeSession?.zoneId === selectedParking?.id
            ? selectedParkingSpots.find(s => s.spotNumber === activeSession.spotNumber)?.id
            : undefined
        }
        hasActiveSession={!!activeSession}
        isBooking={isBooking || loadingSpots}
      />

      {/* Stop Confirmation Modal */}
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

      {/* Payment Summary Modal */}
      {completedSession && (
        <PaymentSummaryModal
          session={completedSession}
          isOpen={!!completedSession}
          onClose={() => setCompletedSession(null)}
        />
      )}
    </div>
  );
};

export default Index;
