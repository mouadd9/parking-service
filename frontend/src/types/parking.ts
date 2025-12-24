export interface ParkingZone {
  id: string;
  name: string;
  hourlyRate: number;
  availableSpots: number;
  totalSpots: number;
  latitude: number;
  longitude: number;
}

export interface ParkingSpot {
  id: string;
  spotNumber: string;
  status: 'free' | 'occupied' | 'booked';
  zoneId: string;
}

export interface ParkingSession {
  id: string;
  zoneId: string;
  zoneName: string;
  spotNumber: string;
  startTime?: Date; // Only set when sensor detects entry
  endTime?: Date;
  totalCost?: number;
  status: 'reserved' | 'active' | 'completed'; // reserved = waiting for entry, active = timer running
}
