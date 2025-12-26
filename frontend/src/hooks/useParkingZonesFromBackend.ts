import { useState, useEffect } from 'react';
import { parkingApi, ParkingZoneResponse } from '@/services/parkingApi';
import { GeoJSONFeatureCollection, GeoJSONFeature } from '@/utils/overpassToGeoJSON';

// Convert backend parking zones to GeoJSON format for the map
const convertToGeoJSON = (zones: ParkingZoneResponse[]): GeoJSONFeatureCollection => {
  return {
    type: 'FeatureCollection',
    features: zones.map(zone => ({
      type: 'Feature',
      id: String(zone.id),
      geometry: {
        type: 'Point',
        coordinates: [zone.longitude, zone.latitude],
      },
      properties: {
        id: zone.id,
        name: zone.name,
        capacity: String(zone.capacity),
        centroid: [zone.latitude, zone.longitude],
        hourlyRate: zone.hourlyRate,
        // Additional properties that might be useful
        access: 'public',
        parking: 'surface',
      },
    })),
  };
};

export const useParkingZonesFromBackend = () => {
  const [geojson, setGeojson] = useState<GeoJSONFeatureCollection | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [zones, setZones] = useState<ParkingZoneResponse[]>([]);

  useEffect(() => {
    const fetchParkingZones = async () => {
      try {
        setLoading(true);
        setError(null);

        // Fetch parking zones from backend
        let zonesData = await parkingApi.getParkingZones();

        // Auto-sync if no zones exist
        if (zonesData.length === 0) {
          console.log('No parking zones found. Auto-syncing from Overpass API...');
          try {
            const syncResult = await parkingApi.syncParkingZones();
            console.log('Auto-sync completed:', syncResult);

            // Fetch again after sync
            zonesData = await parkingApi.getParkingZones();
          } catch (syncErr) {
            console.error('Auto-sync failed:', syncErr);
            // Continue with empty data
          }
        }

        setZones(zonesData);

        const geoJsonData = convertToGeoJSON(zonesData);
        setGeojson(geoJsonData);
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : 'Failed to fetch parking zones';
        setError(errorMessage);
        console.error('Error fetching parking zones:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchParkingZones();
  }, []);

  // Function to manually refetch zones (useful after sync)
  const refetch = async () => {
    try {
      setLoading(true);
      setError(null);

      const zonesData = await parkingApi.getParkingZones();
      setZones(zonesData);

      const geoJsonData = convertToGeoJSON(zonesData);
      setGeojson(geoJsonData);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to fetch parking zones';
      setError(errorMessage);
      console.error('Error fetching parking zones:', err);
    } finally {
      setLoading(false);
    }
  };

  return {
    geojson,
    zones,
    loading,
    error,
    refetch,
  };
};
