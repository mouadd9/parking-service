import { useState, useCallback } from 'react';
import { ParkingZone, ParkingSpot, ParkingSession } from '@/types/parking';

/**
 * BACKEND API ENDPOINTS MAPPING:
 * 
 * A. Discovery & Navigation:
 *    - GET /api/zones → fetchZones() - Get all parking zones for map markers
 *    - GET /api/zones/{id}/spots → fetchSpotsForZone() - Get spots when clicking a zone
 * 
 * B. Parking Cycle (Driver Workflow):
 *    - POST /api/spots/{id}/check-in → bookSpot() - Reserve a spot and start parking
 *    - GET /api/my-active-session → Check if user has active session (on app load)
 *    - POST /api/check-out → stopSession() - End parking and process payment
 * 
 * C. History:
 *    - GET /api/my-history → Fetch completed sessions for history tab
 */

// TODO: Replace with API call to GET /api/my-history
const initialSessions: ParkingSession[] = [
  {
    id: 'session-1',
    zoneId: '1',
    zoneName: 'Place Moulay El Mehdi',
    spotNumber: 'A05',
    startTime: new Date(Date.now() - 3600000 * 24 * 2),
    endTime: new Date(Date.now() - 3600000 * 24 * 2 + 7200000),
    totalCost: 10.00,
    status: 'completed',
  },
  {
    id: 'session-2',
    zoneId: '2',
    zoneName: 'Avenue Mohammed V',
    spotNumber: 'B12',
    startTime: new Date(Date.now() - 3600000 * 24),
    endTime: new Date(Date.now() - 3600000 * 24 + 5400000),
    totalCost: 6.00,
    status: 'completed',
  },
  {
    id: 'session-3',
    zoneId: '3',
    zoneName: 'Feddan Park',
    spotNumber: 'A03',
    startTime: new Date(Date.now() - 3600000 * 5),
    endTime: new Date(Date.now() - 3600000 * 3),
    totalCost: 6.00,
    status: 'completed',
  },
];

export function useParkingData() {
  // TODO: Fetch history from GET /api/my-history on mount
  const [sessions, setSessions] = useState<ParkingSession[]>(initialSessions);
  // TODO: Check active session from GET /api/my-active-session on mount
  const [activeSession, setActiveSession] = useState<ParkingSession | null>(null);

  // Add a completed session to history
  const addSession = useCallback((session: ParkingSession) => {
    setSessions(prev => [session, ...prev]);
  }, []);

  return {
    sessions,
    activeSession,
    setActiveSession,
    addSession,
  };
}
