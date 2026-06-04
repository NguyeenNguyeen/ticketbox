package com.ticketbox.backend.worker.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ticketbox.ai.provider", havingValue = "gemini", matchIfMissing = true)
@Slf4j
public class GeminiAiClient implements AiProviderClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    public GeminiAiClient(RestTemplate restTemplate, 
                          @Value("${ticketbox.ai.gemini.key:}") String apiKey,
                          @Value("${ticketbox.ai.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public String generateBio(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        String urlWithKey = apiUrl + "?key=" + apiKey;

        log.info("Sending request to Gemini API (URL: {})", apiUrl);
        Map response = restTemplate.postForObject(urlWithKey, entity, Map.class);
        log.info("Received response from Gemini API");
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(Map response) {
        try {
            List candidates = (List) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map firstCandidate = (Map) candidates.get(0);
                Map content = (Map) firstCandidate.get("content");
                List parts = (List) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    Map firstPart = (Map) parts.get(0);
                    return (String) firstPart.get("text");
                }
            }
        } catch (Exception e) {
            // Fallthrough
        }
        throw new AiValidationException("Could not extract text from Gemini response");
    }
}
