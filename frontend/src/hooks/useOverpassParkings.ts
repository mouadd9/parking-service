import { useState, useEffect } from 'react';
import { fetchTetouanParkings } from '@/services/overpassApi';
import { overpassToGeoJSON, GeoJSONFeatureCollection, GeoJSONFeature } from '@/utils/overpassToGeoJSON';

export interface OSMParking {
  id: string;
  name: string;
  centroid: [number, number];
  geometry: GeoJSONFeature['geometry'];
  properties: GeoJSONFeature['properties'];
}

export function useOverpassParkings() {
  const [parkings, setParkings] = useState<OSMParking[]>([]);
  const [geojson, setGeojson] = useState<GeoJSONFeatureCollection | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadParkings() {
      try {
        setLoading(true);
        setError(null);
        
        const response = await fetchTetouanParkings();
        const geoJsonData = overpassToGeoJSON(response);
        
        setGeojson(geoJsonData);
        
        // Convert to simpler parking objects for the UI
        const parkingList: OSMParking[] = geoJsonData.features.map(feature => ({
          id: feature.id,
          name: feature.properties.name,
          centroid: feature.properties.centroid,
          geometry: feature.geometry,
          properties: feature.properties,
        }));
        
        setParkings(parkingList);
      } catch (err) {
        console.error('Failed to fetch parkings:', err);
        setError(err instanceof Error ? err.message : 'Failed to load parking data');
      } finally {
        setLoading(false);
      }
    }
    
    loadParkings();
  }, []);

  return { parkings, geojson, loading, error };
}
