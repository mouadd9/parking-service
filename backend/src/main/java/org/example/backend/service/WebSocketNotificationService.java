package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 📡 Notify frontend when a reservation becomes ACTIVE (entry detected)
     */
    public void notifyReservationActivated(Long reservationId, String driverId,
                                           String spotNumber, String startTime) {
        log.info("📡 WebSocket: Notifying reservation {} ACTIVATED", reservationId);

        Map<String, Object> message = new HashMap<>();
        message.put("event", "ENTRY_DETECTED");
        message.put("reservationId", reservationId);
        message.put("driverId", driverId);
        message.put("spotNumber", spotNumber);
        message.put("startTime", startTime);
        message.put("status", "ACTIVE");

        // Send to specific driver channel
        messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId,
                message
        );

        log.info("✅ WebSocket notification sent to driver: {}", driverId);
    }

    /**
     * 📡 Notify frontend when a reservation is COMPLETED (exit detected)
     */
    public void notifyReservationCompleted(Long reservationId, String driverId,
                                           String spotNumber, String endTime,
                                           Double totalCost) {
        log.info("📡 WebSocket: Notifying reservation {} COMPLETED", reservationId);

        Map<String, Object> message = new HashMap<>();
        message.put("event", "EXIT_DETECTED");
        message.put("reservationId", reservationId);
        message.put("driverId", driverId);
        message.put("spotNumber", spotNumber);
        message.put("endTime", endTime);
        message.put("totalCost", totalCost);
        message.put("status", "COMPLETED");

        // Send to specific driver channel
        messagingTemplate.convertAndSend(
                "/topic/driver/" + driverId,
                message
        );

        log.info("✅ WebSocket notification sent to driver: {}", driverId);
    }
}