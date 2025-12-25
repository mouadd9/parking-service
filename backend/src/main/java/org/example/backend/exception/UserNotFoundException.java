package org.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED) // ou NOT_FOUND si tu veux
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Identifiants invalides");
    }
}

