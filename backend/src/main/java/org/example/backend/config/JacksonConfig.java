package org.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This file is dedicated to configuring Jackson for proper serialization and deserialization

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register module for Java 8 date/time types
        mapper.registerModule(new JavaTimeModule());

        // Configure to write dates as strings (ISO-8601 format)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Optional: pretty print JSON
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper;
    }
}