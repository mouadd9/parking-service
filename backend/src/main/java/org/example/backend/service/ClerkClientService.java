package org.example.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClerkClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${clerk.secretKey}")
    private String secretKey;

    @Value("${clerk.baseUrl:https://api.clerk.com/v1}")
    private String baseUrl;

    @PostConstruct
    public void checkClerkKey() {
        String k = secretKey == null ? "" : secretKey.trim();
        System.out.println("CLERK_SECRET_KEY loaded = " + (!k.isBlank() && k.startsWith("sk_")));
        System.out.println("CLERK_SECRET_KEY length = " + k.length());
    }

    public String createUserOnClerk(String firstName, String lastName, String email, String password) {
        String url = baseUrl + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey == null ? "" : secretKey.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "first_name", firstName,
                "last_name", lastName,
                "email_address", List.of(email),
                "password", password
        );

        ResponseEntity<Map> resp = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
        );
        Map json = resp.getBody();
        if (json == null || json.get("id") == null) {
            throw new IllegalStateException("Réponse Clerk invalide: pas de id");
        }
        return json.get("id").toString();
    }

    @SuppressWarnings("unchecked")
    public String fetchPrimaryEmailFromClerk(String clerkId) {
        String url = baseUrl + "/users/" + clerkId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(secretKey == null ? "" : secretKey.trim());
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        Map<String, Object> body = resp.getBody();
        if (body == null) return null;
        String primaryId = (String) body.get("primary_email_address_id");
        List<Map<String, Object>> emails = (List<Map<String, Object>>) body.get("email_addresses");

        if (primaryId == null || emails == null) return null;
        for (Map<String, Object> e : emails) {
            if (primaryId.equals(e.get("id"))) {return (String) e.get("email_address");}
        }
        return null;
    }
}
