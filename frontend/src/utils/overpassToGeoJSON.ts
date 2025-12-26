/**
 * Converts Overpass API response to GeoJSON format for Leaflet
 */

import { OverpassElement, OverpassResponse } from '@/services/overpassApi';

export interface GeoJSONFeature {
  type: 'Feature';
  id: string;
  geometry: {
    type: 'Point' | 'Polygon' | 'MultiPolygon';
    coordinates: number[] | number[][] | number[][][] | number[][][][];
  };
  properties: {
    id: string | number;
    osmId?: number;
    osmType?: string;
    name: string;
    access?: string;
    surface?: string;
    capacity?: string;
    fee?: string;
    parking?: string;
    centroid: [number, number]; // [lat, lon] for marker placement
    hourlyRate?: number; // Added for backend parking zones
  };
}

export interface GeoJSONFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJSONFeature[];
}

function calculateCentroid(coords: Array<{ lat: number; lon: number }>): [number, number] {
  if (coords.length === 0) return [0, 0];
  
  const sum = coords.reduce(
    (acc, coord) => ({
      lat: acc.lat + coord.lat,
      lon: acc.lon + coord.lon,
    }),
    { lat: 0, lon: 0 }
  );
  
  return [sum.lat / coords.length, sum.lon / coords.length];
}

function convertNodeToFeature(element: OverpassElement): GeoJSONFeature | null {
  if (element.lat === undefined || element.lon === undefined) return null;
  
  return {
    type: 'Feature',
    id: `node-${element.id}`,
    geometry: {
      type: 'Point',
      coordinates: [element.lon, element.lat], // GeoJSON uses [lon, lat]
    },
    properties: {
      id: `node-${element.id}`,
      osmId: element.id,
      osmType: 'node',
      name: element.tags?.name || `Parking ${element.id}`,
      access: element.tags?.access,
      surface: element.tags?.surface,
      capacity: element.tags?.capacity,
      fee: element.tags?.fee,
      parking: element.tags?.parking,
      centroid: [element.lat, element.lon],
    },
  };
}

function convertWayToFeature(element: OverpassElement): GeoJSONFeature | null {
  if (!element.geometry || element.geometry.length === 0) return null;
  
  // Convert to GeoJSON coordinates [lon, lat]
  const coordinates = element.geometry.map(coord => [coord.lon, coord.lat]);
  
  // Close the polygon if not already closed
  const first = coordinates[0];
  const last = coordinates[coordinates.length - 1];
  if (first[0] !== last[0] || first[1] !== last[1]) {
    coordinates.push([...first]);
  }
  
  const centroid = calculateCentroid(element.geometry);
  
  return {
    type: 'Feature',
    id: `way-${element.id}`,
    geometry: {
      type: 'Polygon',
      coordinates: [coordinates], // Polygon is array of rings
    },
    properties: {
      id: `way-${element.id}`,
      osmId: element.id,
      osmType: 'way',
      name: element.tags?.name || `Parking ${element.id}`,
      access: element.tags?.access,
      surface: element.tags?.surface,
      capacity: element.tags?.capacity,
      fee: element.tags?.fee,
      parking: element.tags?.parking,
      centroid,
    },
  };
}

function convertRelationToFeature(element: OverpassElement): GeoJSONFeature | null {
  if (!element.members || element.members.length === 0) return null;
  
  const outerRings: number[][][] = [];
  const allCoords: Array<{ lat: number; lon: number }> = [];
  
  for (const member of element.members) {
    if (member.role === 'outer' && member.geometry) {
      const coordinates = member.geometry.map(coord => [coord.lon, coord.lat]);
      
      // Close the ring if needed
      const first = coordinates[0];
      const last = coordinates[coordinates.length - 1];
      if (first && last && (first[0] !== last[0] || first[1] !== last[1])) {
        coordinates.push([...first]);
      }
      
      outerRings.push(coordinates);
      allCoords.push(...member.geometry);
    }
  }
  
  if (outerRings.length === 0) return null;
  
  const centroid = calculateCentroid(allCoords);
  
  // If single outer ring, use Polygon; otherwise MultiPolygon
  const geometry = outerRings.length === 1
    ? { type: 'Polygon' as const, coordinates: outerRings }
    : { type: 'MultiPolygon' as const, coordinates: outerRings.map(ring => [ring]) };
  
  return {
    type: 'Feature',
    id: `relation-${element.id}`,
    geometry,
    properties: {
      id: `relation-${element.id}`,
      osmId: element.id,
      osmType: 'relation',
      name: element.tags?.name || `Parking ${element.id}`,
      access: element.tags?.access,
      surface: element.tags?.surface,
      capacity: element.tags?.capacity,
      fee: element.tags?.fee,
      parking: element.tags?.parking,
      centroid,
    },
  };
}

export function overpassToGeoJSON(response: OverpassResponse): GeoJSONFeatureCollection {
  const features: GeoJSONFeature[] = [];
  
  for (const element of response.elements) {
    let feature: GeoJSONFeature | null = null;
    
    switch (element.type) {
      case 'node':
        feature = convertNodeToFeature(element);
        break;
      case 'way':
        feature = convertWayToFeature(element);
        break;
      case 'relation':
        feature = convertRelationToFeature(element);
        break;
    }
    
    if (feature) {
      features.push(feature);
    }
  }
  
  return {
    type: 'FeatureCollection',
    features,
  };
}
