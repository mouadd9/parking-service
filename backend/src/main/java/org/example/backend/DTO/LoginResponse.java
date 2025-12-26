package org.example.backend.DTO;

import org.example.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;


public class LoginResponse {
    private long id;
    private String token;
    private String role; // ou Role enum
    private String Nom;


    public LoginResponse(long id,String token, String role,String Nom) {
        this.id = id;
        this.token = token;
        this.role = role;
        this.Nom = Nom;
    }

    public long getId() { return id; }
    public String getToken() { return token; }
    public String getRole() { return role; }
    public String getNom() { return Nom; }

}
