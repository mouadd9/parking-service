package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.DTO.ZoneRateDTO;
import org.example.backend.service.ZoneRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
@Slf4j
public class ZoneRateController {

    private final ZoneRateService zoneRateService;

    @GetMapping("/rates")
    public ResponseEntity<?> getAllZoneRates() {
        try {
            log.info("GET /api/zones/rates appelé");

            List<ZoneRateDTO> zoneRates = zoneRateService.getAllZoneRates();

            // Si pas de données, retourner des données mockées pour le développement
            if (zoneRates.isEmpty()) {
                log.warn("Aucune zone trouvée, retour de données de test");
                zoneRates = zoneRateService.getMockZoneRates();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", zoneRates,
                    "count", zoneRates.size(),
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur dans /api/zones/rates", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la récupération des tarifs: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/{zoneId}/rate")
    public ResponseEntity<?> updateZoneRate(
            @PathVariable Long zoneId,
            @RequestBody Map<String, Object> request) {

        try {
            log.info("PUT /api/zones/{}/rate appelé avec: {}", zoneId, request);

            if (!request.containsKey("hourlyRate")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Le champ 'hourlyRate' est requis"
                ));
            }

            BigDecimal newRate = new BigDecimal(request.get("hourlyRate").toString());

            if (newRate.compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Le tarif doit être positif"
                ));
            }

            boolean updated = zoneRateService.updateZoneRate(zoneId, newRate);

            if (updated) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Tarif mis à jour avec succès",
                        "zoneId", zoneId,
                        "newRate", newRate,
                        "timestamp", java.time.LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Zone non trouvée avec l'ID: " + zoneId
                ));
            }
        } catch (Exception e) {
            log.error("❌ Erreur dans /api/zones/{}/rate", zoneId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour du tarif: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{zoneId}/rate")
    public ResponseEntity<?> getZoneRate(@PathVariable Long zoneId) {
        try {
            log.info("GET /api/zones/{}/rate appelé", zoneId);

            ZoneRateDTO zoneRate = zoneRateService.getZoneRateById(zoneId);

            if (zoneRate != null) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "data", zoneRate,
                        "timestamp", java.time.LocalDateTime.now()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Zone non trouvée avec l'ID: " + zoneId
                ));
            }
        } catch (Exception e) {
            log.error("❌ Erreur dans /api/zones/{}/rate", zoneId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la récupération du tarif: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/rates/bulk-update")
    public ResponseEntity<?> bulkUpdateRates(@RequestBody List<Map<String, Object>> updates) {
        try {
            log.info("PUT /api/zones/rates/bulk-update appelé avec {} mises à jour", updates.size());

            int successCount = 0;
            List<Map<String, Object>> results = new java.util.ArrayList<>();

            for (Map<String, Object> update : updates) {
                try {
                    Long zoneId = Long.valueOf(update.get("zoneId").toString());
                    BigDecimal newRate = new BigDecimal(update.get("newRate").toString());

                    boolean updated = zoneRateService.updateZoneRate(zoneId, newRate);

                    Map<String, Object> result = new HashMap<>();
                    result.put("zoneId", zoneId);
                    result.put("success", updated);
                    result.put("newRate", newRate);

                    if (updated) successCount++;

                    results.add(result);
                } catch (Exception e) {
                    log.warn("Échec de la mise à jour pour une zone: {}", e.getMessage());
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", String.format("%d/%d tarifs mis à jour avec succès", successCount, updates.size()),
                    "results", results,
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur dans /api/zones/rates/bulk-update", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la mise à jour en masse: " + e.getMessage()
            ));
        }
    }
}