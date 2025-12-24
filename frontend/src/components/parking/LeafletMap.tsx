import React, { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { GeoJSONFeatureCollection, GeoJSONFeature } from '@/utils/overpassToGeoJSON';

interface LeafletMapProps {
  geojson: GeoJSONFeatureCollection | null;
  selectedParkingId: string | null;
  onParkingClick: (parking: GeoJSONFeature) => void;
  loading?: boolean;
  mapType?: 'normal' | 'satellite';
}

// Tetouan city bounds
const TETOUAN_CENTER: [number, number] = [35.5785, -5.3684];
const TETOUAN_BOUNDS: L.LatLngBoundsExpression = [
  [35.52, -5.45],
  [35.65, -5.20]
];

// Tile layer configurations
const TILE_LAYERS = {
  normal: {
    url: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png',
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    subdomains: 'abcd',
  },
  satellite: {
    url: 'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    attribution: '&copy; Esri, Maxar, Earthstar Geographics',
  },
} as const;
const LeafletMap = ({ geojson, selectedParkingId, onParkingClick, loading, mapType = 'normal' }: LeafletMapProps) => {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const geoJsonLayerRef = useRef<L.GeoJSON | null>(null);
  const markersLayerRef = useRef<L.LayerGroup | null>(null);
  const tileLayerRef = useRef<L.TileLayer | null>(null);

  // Initialize map
  useEffect(() => {
    if (!mapContainer.current || mapRef.current) return;

    const map = L.map(mapContainer.current, {
      center: TETOUAN_CENTER,
      zoom: 14,
      minZoom: 12,
      maxZoom: 18,
      maxBounds: TETOUAN_BOUNDS,
      maxBoundsViscosity: 1.0,
    });

    const config = TILE_LAYERS.normal;
    tileLayerRef.current = L.tileLayer(config.url, {
      attribution: config.attribution,
      ...('subdomains' in config && { subdomains: config.subdomains }),
    }).addTo(map);

    // Create layers for markers and polygons
    markersLayerRef.current = L.layerGroup().addTo(map);
    
    mapRef.current = map;

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Update tile layer when mapType changes
  useEffect(() => {
    if (!mapRef.current || !tileLayerRef.current) return;

    const config = TILE_LAYERS[mapType];
    tileLayerRef.current.remove();
    tileLayerRef.current = L.tileLayer(config.url, {
      attribution: config.attribution,
      ...('subdomains' in config && { subdomains: config.subdomains }),
    }).addTo(mapRef.current);
    
    // Ensure tile layer is below other layers
    tileLayerRef.current.bringToBack();
  }, [mapType]);

  // Render GeoJSON data
  useEffect(() => {
    if (!mapRef.current || !geojson) return;

    // Clear existing layers
    if (geoJsonLayerRef.current) {
      geoJsonLayerRef.current.remove();
    }
    if (markersLayerRef.current) {
      markersLayerRef.current.clearLayers();
    }

    // Style function for polygons
    const getStyle = (feature: GeoJSONFeature | undefined): L.PathOptions => {
      const isSelected = feature?.id === selectedParkingId;
      return {
        fillColor: isSelected ? 'hsl(var(--primary))' : 'hsl(210, 100%, 50%)',
        fillOpacity: isSelected ? 0.5 : 0.3,
        color: isSelected ? 'hsl(var(--primary))' : 'hsl(210, 100%, 40%)',
        weight: isSelected ? 3 : 2,
      };
    };

    // Create GeoJSON layer for polygons
    geoJsonLayerRef.current = L.geoJSON(geojson as any, {
      style: (feature) => getStyle(feature as GeoJSONFeature),
      pointToLayer: (feature, latlng) => {
        // For point features (nodes), create a circle marker
        const isSelected = feature.id === selectedParkingId;
        return L.circleMarker(latlng, {
          radius: isSelected ? 12 : 8,
          fillColor: isSelected ? 'hsl(var(--primary))' : 'hsl(210, 100%, 50%)',
          fillOpacity: isSelected ? 0.8 : 0.6,
          color: isSelected ? 'hsl(var(--primary))' : 'hsl(210, 100%, 40%)',
          weight: 2,
        });
      },
      onEachFeature: (feature, layer) => {
        // Add click handler
        layer.on('click', () => {
          onParkingClick(feature as GeoJSONFeature);
        });

        // Add tooltip with parking name
        const props = (feature as GeoJSONFeature).properties;
        layer.bindTooltip(props.name, {
          permanent: false,
          direction: 'top',
          className: 'parking-tooltip',
        });
      },
    }).addTo(mapRef.current);

    // Also add markers at centroids for easier clicking
    geojson.features.forEach(feature => {
      const isSelected = feature.id === selectedParkingId;
      const [lat, lon] = feature.properties.centroid;
      
      const markerIcon = L.divIcon({
        className: 'custom-div-icon',
        html: `<div class="parking-marker ${isSelected ? 'selected' : ''}">P</div>`,
        iconSize: [isSelected ? 32 : 24, isSelected ? 32 : 24],
        iconAnchor: [isSelected ? 16 : 12, isSelected ? 16 : 12],
      });

      const marker = L.marker([lat, lon], { icon: markerIcon });
      marker.on('click', () => onParkingClick(feature));
      markersLayerRef.current?.addLayer(marker);
    });

  }, [geojson, selectedParkingId, onParkingClick]);

  // Pan to selected parking
  useEffect(() => {
    if (!mapRef.current || !selectedParkingId || !geojson) return;
    
    const feature = geojson.features.find(f => f.id === selectedParkingId);
    if (feature) {
      const [lat, lon] = feature.properties.centroid;
      mapRef.current.setView([lat, lon], 17, { animate: true });
    }
  }, [selectedParkingId, geojson]);

  return (
    <div className="relative w-full h-full">
      <div 
        ref={mapContainer} 
        className="absolute inset-0"
        style={{ zIndex: 0 }}
      />
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center bg-background/50 z-10">
          <div className="flex flex-col items-center gap-2">
            <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            <span className="text-sm text-muted-foreground">Loading parking data...</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default LeafletMap;
