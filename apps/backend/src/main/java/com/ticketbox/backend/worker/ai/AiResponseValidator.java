package com.ticketbox.backend.worker.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiResponseValidator {

    private final ObjectMapper objectMapper;

    public record AiParsedBio(String name, String biography) {}

    public AiParsedBio validateAndExtractBiography(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new AiValidationException("AI response is empty");
        }

        String cleanedJson = stripMarkdownFences(rawResponse);

        try {
            JsonNode rootNode = objectMapper.readTree(cleanedJson);
            
            if (!rootNode.has("biography")) {
                throw new AiValidationException("JSON response is missing the 'biography' field");
            }

            String biography = rootNode.get("biography").asText();
            String name = rootNode.has("name") ? rootNode.get("name").asText() : "Nghệ sĩ chính";
            
            if (biography == null || biography.trim().isEmpty()) {
                throw new AiValidationException("Extracted biography is empty");
            }
            
            if (biography.split("\\s+").length < 30) {
                log.warn("Extracted biography is suspiciously short.");
            }

            return new AiParsedBio(name.trim(), biography.trim());

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("Failed to parse JSON from AI response. Raw: {}", rawResponse);
            throw new AiValidationException("Invalid JSON format from AI provider", e);
        }
    }

    private String stripMarkdownFences(String response) {
        String cleaned = response.trim();
        
        int jsonStart = cleaned.indexOf("```json");
        if (jsonStart != -1) {
            int blockStart = jsonStart + "```json".length();
            int blockEnd = cleaned.lastIndexOf("```");
            if (blockEnd > blockStart) {
                return cleaned.substring(blockStart, blockEnd).trim();
            }
        }
        
        int blockStart = cleaned.indexOf("```");
        if (blockStart != -1) {
            blockStart += "```".length();
            int blockEnd = cleaned.lastIndexOf("```");
            if (blockEnd > blockStart) {
                return cleaned.substring(blockStart, blockEnd).trim();
            }
        }
        
        int firstBrace = cleaned.indexOf("{");
        int lastBrace = cleaned.lastIndexOf("}");
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }
}
