import { ParkingZone, ParkingSpot } from '@/types/parking';

const API_BASE_URL = '/api';

export interface ParkingZoneResponse {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  hourlyRate: number;
  capacity: number;
}

export interface ParkingSpotResponse {
  id: number;
  spotNumber: string;
  status: boolean; // true = free, false = occupied
  sensorId: string;
  zone: {
    id: number;
    name: string;
    hourlyRate: number;
  };
}

export interface ReservationRequest {
  spotId: number;
  driverId: string;
  startTime: string; // ISO 8601 format
  endTime: string;   // ISO 8601 format
}

export interface ReservationResponse {
  id: number;
  spotId: number;
  spotNumber: string;
  driverId: string;
  startTime: string;
  endTime: string;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
}

export const parkingApi = {
  // Fetch all parking zones from the database
  async getParkingZones(): Promise<ParkingZoneResponse[]> {
    const response = await fetch(`${API_BASE_URL}/zones`);
    if (!response.ok) {
      throw new Error('Failed to fetch parking zones');
    }
    return response.json();
  },

  // Get spots for a specific zone
  async getZoneSpots(zoneId: number): Promise<ParkingSpotResponse[]> {
    const response = await fetch(`${API_BASE_URL}/zones/${zoneId}/spots`);
    if (!response.ok) {
      throw new Error('Failed to fetch zone spots');
    }
    return response.json();
  },

  // Sync parking zones from Overpass API (admin action)
  async syncParkingZones(): Promise<{ success: boolean; created: number; updated: number; skipped: number; total: number }> {
    const response = await fetch(`${API_BASE_URL}/parking-zones/sync`, {
      method: 'POST',
    });
    if (!response.ok) {
      throw new Error('Failed to sync parking zones');
    }
    return response.json();
  },

  // Create a new reservation (PENDING status - waiting for entry)
  async createReservation(request: ReservationRequest): Promise<ReservationResponse> {
    const response = await fetch(`${API_BASE_URL}/reservations/create`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to create reservation');
    }

    return response.json();
  },

  // Get reservation status
  async getReservationStatus(reservationId: number): Promise<ReservationResponse> {
    const response = await fetch(`${API_BASE_URL}/reservations/${reservationId}/status`);
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to fetch reservation status');
    }
    return response.json();
  },

  // Get user's reservations
  async getUserReservations(driverId: string): Promise<ReservationResponse[]> {
    const response = await fetch(`${API_BASE_URL}/reservations/user/${driverId}`);
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to fetch user reservations');
    }
    return response.json();
  },

  // Cancel a reservation
  async cancelReservation(reservationId: number): Promise<void> {
    const response = await fetch(`${API_BASE_URL}/reservations/${reservationId}/cancel`, {
      method: 'POST',
    });
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to cancel reservation');
    }
  },
};
