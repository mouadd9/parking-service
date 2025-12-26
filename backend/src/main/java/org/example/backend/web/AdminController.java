package org.example.backend.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.service.AdminStatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        try {
            log.info("GET /api/admin/statistics appelé");

            Map<String, Object> statistics = adminStatisticsService.getStatistics();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", statistics,
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur dans /api/admin/statistics", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Erreur lors de la récupération des statistiques: " + e.getMessage()
            ));
        }
    }
}