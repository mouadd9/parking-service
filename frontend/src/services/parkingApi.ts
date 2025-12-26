import { ParkingZone, ParkingSpot } from '@/types/parking';

const API_BASE_URL = "http://localhost:8080/api";


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

/**
 * Helper: fetch qui gère
 * - Authorization Bearer token si présent
 * - réponse HTML vs JSON (évite Unexpected token '<')
 * - erreurs HTTP avec message lisible
 */
async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  withAuth: boolean = true
): Promise<T> {
  const token = localStorage.getItem('token');

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(withAuth && token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {}),
    },
  });

  const contentType = response.headers.get('content-type') || '';
  const text = await response.text(); // on lit toujours en text d'abord

  // Si status != 2xx
  if (!response.ok) {
    // si backend renvoie du JSON d'erreur
    if (contentType.includes('application/json')) {
      try {
        const errJson = JSON.parse(text);
        const msg =
          errJson.message ||
          errJson.error ||
          errJson.details ||
          JSON.stringify(errJson);
        throw new Error(msg);
      } catch {
        // fallback en bas
      }
    }

    // sinon HTML/texte brut
    throw new Error(`HTTP ${response.status}: ${text.slice(0, 300)}`);
  }

  // Si pas JSON (ex: HTML) => erreur explicite
  if (!contentType.includes('application/json')) {
    throw new Error(
      `Expected JSON but got "${contentType}". Response starts with: ${text.slice(0, 60)}`
    );
  }

  return JSON.parse(text) as T;
}

export const parkingApi = {
  // Fetch all parking zones from the database
  async getParkingZones(): Promise<ParkingZoneResponse[]> {
    return apiFetch<ParkingZoneResponse[]>('/zones', { method: 'GET' });
  },

  // Get spots for a specific zone
  async getZoneSpots(zoneId: number): Promise<ParkingSpotResponse[]> {
    return apiFetch<ParkingSpotResponse[]>(`/zones/${zoneId}/spots`, { method: 'GET' });
  },

  // Sync parking zones from Overpass API (admin action)
  async syncParkingZones(): Promise<{ success: boolean; created: number; updated: number; skipped: number; total: number }> {
    return apiFetch(`/parking-zones/sync`, { method: 'POST' });
  },

  // Create a new reservation (PENDING status - waiting for entry)
  async createReservation(request: ReservationRequest): Promise<ReservationResponse> {
    return apiFetch<ReservationResponse>(`/reservations/create`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  // Get reservation status
  async getReservationStatus(reservationId: number): Promise<ReservationResponse> {
    return apiFetch<ReservationResponse>(`/reservations/${reservationId}/status`, { method: 'GET' });
  },

  // Get user's reservations
  async getUserReservations(driverId: string): Promise<ReservationResponse[]> {
    return apiFetch<ReservationResponse[]>(`/reservations/user/${driverId}`, { method: 'GET' });
  },

  // Cancel a reservation
  async cancelReservation(reservationId: number): Promise<void> {
    await apiFetch<void>(`/reservations/${reservationId}/cancel`, { method: 'POST' });
  },
};
