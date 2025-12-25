package org.example.backend.service;


import org.example.backend.DTO.LoginRequest;
import org.example.backend.DTO.LoginResponse;
import org.example.backend.entities.Utilisateur;
import org.example.backend.repository.UtilisateurRepository;
import org.example.backend.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.example.backend.exception.*;

@Service
public class AuthService {

    private final UtilisateurRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtil;

    private static final long DEFAULT_EXPIRATION = 60 * 60 * 1000; // 1h
    private static final long REMEMBER_ME_EXPIRATION = 7 * 24 * 60 * 60 * 1000; // 7 jours

    public AuthService(UtilisateurRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }



    public LoginResponse login(LoginRequest request) {

        Utilisateur user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getMotDePasse())) {
            throw new InvalidCredentialsException();
        }

        // 3) gérer rememberMe (évite NullPointerException)

        // 4) générer token JWT
        String token = jwtUtil.generateToken(user.getEmail());

        // 5) renvoyer token + rôle
        return new LoginResponse(token, user.getRole().toString());
    }


    public void changePassword(Long userId, String oldPassword, String newPassword) {
        Utilisateur user = userRepository.findById(userId)  // <-- utiliser 'users' (avec s) ici
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(oldPassword, user.getMotDePasse())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }

        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("Le nouveau mot de passe est trop court");
        }

        user.setMotDePasse(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

}