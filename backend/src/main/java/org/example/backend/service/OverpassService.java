package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverpassService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Overpass API endpoints for fallback
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    };

    // Tétouan bounding box
    private static final String TETOUAN_BBOX = "35.52,-5.42,35.62,-5.28";

    private static final String TETOUAN_PARKING_QUERY = """
            [out:json][timeout:30];
            (
              node["amenity"="parking"](%s);
              way["amenity"="parking"](%s);
              relation["amenity"="parking"](%s);
            );
            out geom;
            """.formatted(TETOUAN_BBOX, TETOUAN_BBOX, TETOUAN_BBOX);

    public List<ParkingData> fetchTetouanParkings() {
        JsonNode response = fetchWithRetry();
        return parseOverpassResponse(response);
    }

    private JsonNode fetchWithRetry() {
        Exception lastError = null;

        for (String endpoint : OVERPASS_ENDPOINTS) {
            for (int attempt = 0; attempt <= 2; attempt++) {
                try {
                    log.info("Fetching from {}, attempt {}", endpoint, attempt + 1);

                    String body = "data=" + java.net.URLEncoder.encode(TETOUAN_PARKING_QUERY, "UTF-8");

                    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                    headers.set("Content-Type", "application/x-www-form-urlencoded");

                    org.springframework.http.HttpEntity<String> request =
                        new org.springframework.http.HttpEntity<>(body, headers);

                    org.springframework.http.ResponseEntity<String> response =
                        restTemplate.postForEntity(endpoint, request, String.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode jsonNode = objectMapper.readTree(response.getBody());
                        log.info("Success! Found {} parking elements",
                            jsonNode.has("elements") ? jsonNode.get("elements").size() : 0);
                        return jsonNode;
                    }
                } catch (Exception e) {
                    lastError = e;
                    log.warn("Attempt failed: {}", e.getMessage());

                    if (attempt < 2) {
                        try {
                            Thread.sleep(1000L * (attempt + 1));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }

        throw new RuntimeException("All Overpass API endpoints failed", lastError);
    }

    private List<ParkingData> parseOverpassResponse(JsonNode response) {
        List<ParkingData> parkings = new ArrayList<>();

        if (!response.has("elements")) {
            return parkings;
        }

        JsonNode elements = response.get("elements");
        for (JsonNode element : elements) {
            try {
                ParkingData data = parseSingleElement(element);
                if (data != null) {
                    parkings.add(data);
                }
            } catch (Exception e) {
                log.warn("Failed to parse element {}: {}", element.get("id"), e.getMessage());
            }
        }

        log.info("Parsed {} parking areas from Overpass API", parkings.size());
        return parkings;
    }

    private ParkingData parseSingleElement(JsonNode element) {
        JsonNode tags = element.get("tags");
        if (tags == null) {
            return null;
        }

        String name = tags.has("name") ? tags.get("name").asText() : null;
        if (name == null || name.isBlank()) {
            // Generate name from ID if no name is provided
            long id = element.get("id").asLong();
            name = "Parking OSM-" + id;
        }

        // Calculate centroid (lat, lon)
        double[] centroid = calculateCentroid(element);
        if (centroid == null) {
            return null; // Skip if we can't determine location
        }

        // Extract capacity if available
        Integer capacity = null;
        if (tags.has("capacity")) {
            try {
                capacity = Integer.parseInt(tags.get("capacity").asText());
            } catch (NumberFormatException e) {
                // Ignore invalid capacity
            }
        }

        // Extract hourly rate if available in tags (unlikely, but check)
        BigDecimal hourlyRate = null;
        if (tags.has("fee") && tags.get("fee").asText().contains("yes")) {
            // Could parse fee info, but for now we'll use defaults
        }

        return ParkingData.builder()
                .name(name)
                .latitude(centroid[0])
                .longitude(centroid[1])
                .capacity(capacity)
                .hourlyRate(hourlyRate)
                .osmId(element.get("id").asLong())
                .osmType(element.get("type").asText())
                .surface(tags.has("surface") ? tags.get("surface").asText() : null)
                .access(tags.has("access") ? tags.get("access").asText() : null)
                .parkingType(tags.has("parking") ? tags.get("parking").asText() : null)
                .build();
    }

    private double[] calculateCentroid(JsonNode element) {
        String type = element.get("type").asText();

        if ("node".equals(type)) {
            // For nodes, use lat/lon directly
            if (element.has("lat") && element.has("lon")) {
                return new double[]{
                        element.get("lat").asDouble(),
                        element.get("lon").asDouble()
                };
            }
        } else if ("way".equals(type) || "relation".equals(type)) {
            // For ways and relations, calculate centroid from geometry
            JsonNode geometry = element.get("geometry");
            if (geometry != null && geometry.isArray() && geometry.size() > 0) {
                double latSum = 0;
                double lonSum = 0;
                int count = 0;

                for (JsonNode point : geometry) {
                    if (point.has("lat") && point.has("lon")) {
                        latSum += point.get("lat").asDouble();
                        lonSum += point.get("lon").asDouble();
                        count++;
                    }
                }

                if (count > 0) {
                    return new double[]{latSum / count, lonSum / count};
                }
            }

            // Fallback: use bounds if geometry not available
            if (element.has("bounds")) {
                JsonNode bounds = element.get("bounds");
                double centerLat = (bounds.get("minlat").asDouble() + bounds.get("maxlat").asDouble()) / 2;
                double centerLon = (bounds.get("minlon").asDouble() + bounds.get("maxlon").asDouble()) / 2;
                return new double[]{centerLat, centerLon};
            }
        }

        return null;
    }

    @lombok.Data
    @lombok.Builder
    public static class ParkingData {
        private String name;
        private Double latitude;
        private Double longitude;
        private Integer capacity;
        private BigDecimal hourlyRate;
        private Long osmId;
        private String osmType;
        private String surface;
        private String access;
        private String parkingType;
    }
}
