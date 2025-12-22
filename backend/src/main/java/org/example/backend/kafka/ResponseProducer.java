package org.example.backend.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResponseProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendResponse(String jsonMessage) {
        // topic : reclamation-responses
        kafkaTemplate.send("reclamation-responses", jsonMessage);
        System.out.println("Sent response to topic: " + jsonMessage);
    }
}
