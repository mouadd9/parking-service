package org.example.backend.enums;

public enum SessionStatus {
    ACTIVE,             // Driver is currently parking
    COMPLETED,          // Driver left, session closed
    PENDING_PAYMENT     // (Optional) If you add payment logic later
}
