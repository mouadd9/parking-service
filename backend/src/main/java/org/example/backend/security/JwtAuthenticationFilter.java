package org.example.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtil,
                                   UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("═══════════════════════════════════════");
        System.out.println("🔍 JWT Filter - Request: " + request.getMethod() + " " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        System.out.println("🔍 Authorization Header: " + (authHeader != null ? "Present (Bearer...)" : "❌ Missing"));

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                System.out.println("🔍 Token extracted (length: " + token.length() + ")");

                String username = jwtUtil.extractUsername(token);
                System.out.println("✅ Username from token: " + username);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    System.out.println("🔍 Loading user details for: " + username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("✅ UserDetails loaded: " + userDetails.getUsername());
                    System.out.println("   - Authorities: " + userDetails.getAuthorities());

                    // ⚠️ VALIDATION DU TOKEN
                    if (jwtUtil.validateToken(token, userDetails)) {
                        System.out.println("✅ Token is VALID - Setting authentication");

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        System.out.println("✅ Authentication set successfully in SecurityContext");
                    } else {
                        System.out.println("❌ Token validation FAILED");
                    }
                } else {
                    if (username == null) {
                        System.out.println("⚠️ Username extracted is NULL");
                    }
                    if (SecurityContextHolder.getContext().getAuthentication() != null) {
                        System.out.println("⚠️ Authentication already exists in context");
                    }
                }
            } catch (Exception e) {
                System.out.println("❌ JWT Filter Exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ No Bearer token in request");
        }

        System.out.println("═══════════════════════════════════════");
        filterChain.doFilter(request, response);
    }
}