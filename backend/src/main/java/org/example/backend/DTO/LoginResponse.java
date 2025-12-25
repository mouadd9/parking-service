package org.example.backend.DTO;

import org.example.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;


public class LoginResponse {
    private String token;
    private String role; // ou Role enum

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getRole() { return role; }
}
