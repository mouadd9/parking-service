package org.example.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED) // 401
public class WrongPasswordException extends RuntimeException {
    public WrongPasswordException() {
        super("Mot de passe incorrect");
    }
}
