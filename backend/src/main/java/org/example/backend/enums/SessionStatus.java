package org.example.backend.enums;

public enum SessionStatus {
    ACTIVE,             // Voiture garée en ce moment
    COMPLETED_PENDING_PAYMENT, // Voiture partie, en attente paiement
    PAID                // Session close et payée
}
