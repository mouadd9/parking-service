package org.example.backend.enums;

public enum SessionStatus {
    PENDING,            // User reserved, waiting for sensor to detect entry
    ACTIVE,             // Driver is currently parking (sensor detected entry)
    COMPLETED,          // Driver left, session closed
    CANCELLED
}
