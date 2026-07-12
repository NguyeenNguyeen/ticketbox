# 12 AI Press Kit Language Fix

## Objective
Fix an issue where the AI Press Kit generation feature frequently returned artist biographies in English, even though the application is designed for Vietnamese users. The goal was to ensure all generated biographies are written in fluent, natural Vietnamese, regardless of the source press kit language.

## Decisions Made
- Investigated the AI prompt generation pipeline (`AiPromptBuilder.java`) and found that the prompt and system instructions were entirely in English without specifying a target output language.
- Decided to solve this via prompt engineering rather than introducing a separate, costly translation step. Large Language Models possess native translation capabilities.
- Added a critical explicit instruction (`CRITICAL: The generated 'biography' text MUST be written in fluent, natural Vietnamese, regardless of the original language of the raw text.`) to the AI prompt template.

## Files Modified
- `apps/backend/src/main/java/com/ticketbox/backend/worker/ai/AiPromptBuilder.java`

## Dependencies
- Affects the payload sent to the external AI provider via `AiProviderClient`.

## Remaining Work
- None.

## Open Questions
- None.
