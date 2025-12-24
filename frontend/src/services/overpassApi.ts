/**
 * Overpass API Service
 * Fetches real parking data from OpenStreetMap for Tétouan
 */

// Multiple Overpass endpoints for fallback
const OVERPASS_ENDPOINTS = [
  'https://overpass-api.de/api/interpreter',
  'https://overpass.kumi.systems/api/interpreter',
  'https://maps.mail.ru/osm/tools/overpass/api/interpreter',
];

// Tétouan bounding box (faster than area query)
// [south, west, north, east]
const TETOUAN_BBOX = '35.52,-5.42,35.62,-5.28';

// Faster bounding box query instead of area query
const TETOUAN_PARKING_QUERY = `
[out:json][timeout:30];
(
  node["amenity"="parking"](${TETOUAN_BBOX});
  way["amenity"="parking"](${TETOUAN_BBOX});
  relation["amenity"="parking"](${TETOUAN_BBOX});
);
out geom;
`;

export interface OverpassElement {
  type: 'node' | 'way' | 'relation';
  id: number;
  lat?: number;
  lon?: number;
  geometry?: Array<{ lat: number; lon: number }>;
  bounds?: {
    minlat: number;
    minlon: number;
    maxlat: number;
    maxlon: number;
  };
  tags?: {
    name?: string;
    access?: string;
    surface?: string;
    capacity?: string;
    fee?: string;
    parking?: string;
    [key: string]: string | undefined;
  };
  members?: Array<{
    type: string;
    ref: number;
    role: string;
    geometry?: Array<{ lat: number; lon: number }>;
  }>;
}

export interface OverpassResponse {
  elements: OverpassElement[];
}

async function fetchWithRetry(
  endpoints: string[],
  query: string,
  retries = 2
): Promise<OverpassResponse> {
  let lastError: Error | null = null;

  for (const endpoint of endpoints) {
    for (let attempt = 0; attempt <= retries; attempt++) {
      try {
        console.log(`Fetching from ${endpoint}, attempt ${attempt + 1}`);
        
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 20000); // 20s timeout

        const response = await fetch(endpoint, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          body: `data=${encodeURIComponent(query)}`,
          signal: controller.signal,
        });

        clearTimeout(timeout);

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        console.log(`Success! Found ${data.elements?.length || 0} parking elements`);
        return data;
      } catch (error) {
        lastError = error instanceof Error ? error : new Error(String(error));
        console.warn(`Attempt failed: ${lastError.message}`);
        
        // Wait before retry (exponential backoff)
        if (attempt < retries) {
          await new Promise(resolve => setTimeout(resolve, 1000 * (attempt + 1)));
        }
      }
    }
  }

  throw lastError || new Error('All Overpass API endpoints failed');
}

export async function fetchTetouanParkings(): Promise<OverpassResponse> {
  return fetchWithRetry(OVERPASS_ENDPOINTS, TETOUAN_PARKING_QUERY);
}
