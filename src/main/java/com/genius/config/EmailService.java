package com.genius.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    public void sendVerification(String toEmail, String token) {
        String url = "https://api.brevo.com/v3/smtp/email";

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> body = new HashMap<>();

        Map<String, String> sender = new HashMap<>();
        sender.put("name", "Genius App");
        sender.put("email", "christopheradegoke4@gmail.com"); // Must be verified in Brevo dashboard
        body.put("sender", sender);

        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", toEmail);
        body.put("to", Collections.singletonList(recipient));

        body.put("subject", "Email Verification Code");
        body.put("htmlContent", "<p>Your verification code is: <b>" + token + "</b></p>");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to send email: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Brevo API Error: " + e.getMessage());
        }
    }
}