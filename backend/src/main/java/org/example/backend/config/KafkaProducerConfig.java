package org.example.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import java.util.HashMap;
import java.util.Map;

//this file is to configure Kafka Producer by specifying the broker address, key and value serializers, and topic creation

@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put("bootstrap.servers", "localhost:9092");
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // automatically create topics if not existing
    @Bean
    public NewTopic reclamationsTopic() {
        return new NewTopic("claims.spk", 1, (short) 1);
    }

    @Bean
    public NewTopic responseTopic() {
        return new NewTopic("reclamation-responses", 1, (short) 1);
    }

}