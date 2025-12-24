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
import { useOverpassParkings } from '@/hooks/useOverpassParkings';
import { GeoJSONFeature } from '@/utils/overpassToGeoJSON';
import { ParkingSpot, ParkingSession } from '@/types/parking';
import { useToast } from '@/hooks/use-toast';

// Generate mock spots for OSM parkings - just numbers, no letters
const generateSpotsForParking = (parkingId: string, capacity?: string): ParkingSpot[] => {
  const count = capacity ? parseInt(capacity) : Math.floor(Math.random() * 20) + 10;
  return Array.from({ length: count }, (_, i) => ({
    id: `${parkingId}-${i + 1}`,
    spotNumber: String(i + 1), // Just the number, no letter prefix
    status: Math.random() > 0.6 ? 'free' : 'occupied' as const,
    zoneId: parkingId,
  }));
};

// Get hourly rate based on parking type
const getHourlyRate = (parking: GeoJSONFeature | null): number => {
  if (!parking) return 5.00;
  const access = parking.properties.access;
  if (access === 'private') return 8.00;
  if (access === 'customers') return 6.00;
  return 5.00;
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
  
  // State for sensor-based workflow
  // When user selects a spot, we set it as "pending" until sensor detects the vehicle
  const [pendingSpot, setPendingSpot] = useState<{ spot: ParkingSpot; parkingId: string; hourlyRate: number } | null>(null);
  
  const { toast } = useToast();
  const { geojson, loading, error } = useOverpassParkings();
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

  // Get spots for selected parking
  const selectedParkingSpots = useMemo(() => {
    if (!selectedParking) return [];
    
    if (spotsCache[selectedParking.id]) {
      return spotsCache[selectedParking.id];
    }
    
    const spots = generateSpotsForParking(
      selectedParking.id, 
      selectedParking.properties.capacity
    );
    setSpotsCache(prev => ({ ...prev, [selectedParking.id]: spots }));
    return spots;
  }, [selectedParking, spotsCache]);

  const handleParkingClick = (parking: GeoJSONFeature) => {
    setSelectedParking(parking);
    setIsModalOpen(true);
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
  const handleBookSpot = (spot: ParkingSpot, hourlyRate: number) => {
    if (!selectedParking) return;
    
    /**
     * SENSOR INTEGRATION POINT - SPOT RESERVATION
     * 
     * When user confirms a spot, we create a "reserved" session.
     * The timer does NOT start yet - it only starts when the sensor detects the vehicle entering.
     * 
     * Backend API call to reserve spot:
     * POST /api/spots/reserve
     * Request body: { spotId: string, parkingId: string, userId: string }
     * 
     * After this, listen for sensor entry detection via WebSocket or polling.
     */
    
    // Create session in "reserved" state - timer not started yet
    const newSession: ParkingSession = {
      id: `session-${Date.now()}`,
      zoneId: selectedParking.id,
      zoneName: selectedParking.properties.name,
      spotNumber: spot.spotNumber,
      // startTime is NOT set - will be set when sensor detects entry
      status: 'reserved', // Waiting for vehicle to enter
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

    toast({
      title: "Spot Reserved",
      description: `Spot ${spot.spotNumber} - Timer will start when you enter the parking`,
    });
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
    
    // Simulate API call
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    // Handle cancellation of reserved spot (user hasn't entered yet)
    if (activeSession.status === 'reserved' || !activeSession.startTime) {
      /**
       * SENSOR INTEGRATION NOTE:
       * User is canceling their reservation before entering the parking.
       * No charges apply.
       * Backend API: POST /api/spots/cancel-reservation
       * Request body: { sessionId: string, spotId: string }
       */
      
      // Update spot status back to free
      setSpotsCache(prev => ({
        ...prev,
        [activeSession.zoneId]: prev[activeSession.zoneId]?.map(s =>
          s.spotNumber === activeSession.spotNumber ? { ...s, status: 'free' as const } : s
        ) || [],
      }));
      
      setActiveSession(null);
      setIsStopModalOpen(false);
      setIsStoppingSession(false);
      
      toast({
        title: "Reservation Canceled",
        description: "Your spot reservation has been canceled. No charges applied.",
      });
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
