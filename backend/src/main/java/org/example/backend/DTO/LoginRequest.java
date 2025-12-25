package org.example.backend.DTO;


import lombok.Getter;
import lombok.Setter;



public class LoginRequest {


    private String email;


    private String password;

    // Boolean pour accepter "absent" dans JSON

    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }
}
