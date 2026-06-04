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
@ConditionalOnProperty(name = "ticketbox.ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiClient implements AiProviderClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    public OpenAiClient(RestTemplate restTemplate,
                        @Value("${ticketbox.ai.openai.key:}") String apiKey,
                        @Value("${ticketbox.ai.openai.url:https://api.openai.com/v1/chat/completions}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    @Override
    public String generateBio(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = Map.of(
            "model", "gpt-3.5-turbo",
            "messages", List.of(
                Map.of("role", "user", "content", prompt)
            )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("Sending request to OpenAI API (URL: {})", apiUrl);
        Map response = restTemplate.postForObject(apiUrl, entity, Map.class);
        log.info("Received response from OpenAI API");
        return extractTextFromResponse(response);
    }

    private String extractTextFromResponse(Map response) {
        try {
            List choices = (List) response.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                return (String) message.get("content");
            }
        } catch (Exception e) {
            // Fallthrough
        }
        throw new AiValidationException("Could not extract text from OpenAI response");
    }
}
