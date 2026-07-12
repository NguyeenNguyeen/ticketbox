package com.ticketbox.backend.worker.ai;

import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    private static final String SYSTEM_PROMPT = 
        "You are a professional concert marketing copywriter. Your job is to extract factual information from the provided raw text and write an engaging artist biography.";

    private static final String PROMPT_TEMPLATE = 
        "System: %s\n\n" +
        "Raw Text: %s\n\n" +
        "Instructions:\n" +
        "1. Write an engaging biography approximately 150 words long.\n" +
        "2. Tone must be professional and factual. Suitable for concert marketing.\n" +
        "3. Do not invent or hallucinate information not present in the text.\n" +
        "4. CRITICAL: The generated 'biography' text MUST be written in fluent, natural Vietnamese, regardless of the original language of the raw text.\n" +
        "5. Output EXACTLY in the following JSON format without any markdown code fences:\n" +
        "{\n" +
        "  \"name\": \"Name of the main artist/band\",\n" +
        "  \"biography\": \"The generated text here...\",\n" +
        "  \"keyGenres\": [\"Genre1\", \"Genre2\"]\n" +
        "}";

    public String buildPrompt(String extractedText) {
        return String.format(PROMPT_TEMPLATE, SYSTEM_PROMPT, extractedText);
    }
}
