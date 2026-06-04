# Phase 4: AI Artist Biography Worker Design

## Requirements
- **Goal:** Automatically generate a concise (150 words) artist biography from an uploaded PDF profile using AI.
- **Tone:** Professional, factual, concert-marketing friendly.
- **Workflow:** Asynchronous (non-blocking for HTTP requests).
- **Fault Tolerance:** Must gracefully handle invalid/empty PDFs, AI API timeouts, rate limits, malformed AI responses, and provider outages.

## Architecture Overview
The AI Worker will sit behind RabbitMQ on an `ai_queue`. When an Organizer uploads a PDF, the backend stores the file and publishes an `AiBioMessage`. The `AiBioConsumer` picks it up, extracts the text, builds a prompt, queries the AI provider via a resilient HTTP client (with rate-limiting/backoff), validates the JSON response, and stores the result in the database.

## Message Contract
```json
{
  "jobId": "uuid-1234",
  "concertId": 101,
  "organizerId": 55,
  "pdfStoragePath": "/var/ticketbox/uploads/profiles/artist_101.pdf",
  "correlationId": "corr-8888",
  "metadata": {}
}
```

## Component Design
- **`AiBioConsumer`**: The RabbitMQ `@RabbitListener`. Thin layer that bridges AMQP to the service.
- **`AiBioService`**: The orchestrator. Fetches the PDF, coordinates extraction, calls the AI provider, and persists the result.
- **`PdfExtractionService`**: Extracts raw text from the PDF.
- **`AiPromptBuilder`**: Constructs the deterministic prompt injecting the raw text.
- **`AiGenerationService`**: The interface for executing the AI generation.
- **`AiResponseValidator`**: Ensures the returned JSON matches the expected schema.
- **`AiJobTracker`**: Redis-backed tracker (similar to the CSV worker) to update the status to `PROCESSING`, `COMPLETED`, or `FAILED`.

## PDF Processing Strategy
- **Library:** `Apache PDFBox`.
- **Handling Normal PDFs:** Extract text using `PDFTextStripper`.
- **Handling Image-Heavy PDFs:** If `PDFBox` extracts less than 20 characters, the `PdfExtractionService` should throw an `UnprocessablePdfException`. (Note: Full OCR is out of scope for this phase).
- **Empty/Corrupted PDFs:** Handled via `try-catch` over the PDFLoader. Throws a business exception which stops the pipeline and logs the error, rather than triggering an endless retry loop.

## AI Provider Strategy
Use the **Strategy Pattern** to prevent vendor lock-in.
- **Interface:** `AiProviderClient`
- **Implementations:** `GeminiAiClient` and `OpenAiClient`.
- **Selection:** Controlled via application properties (e.g., `ticketbox.ai.provider=gemini`).
- **Data Transfer:** Both clients must normalize their responses into a standard internal DTO (`AiBioResult`).

## Prompt Strategy
**System Prompt:**
"You are a professional concert marketing copywriter. Your job is to extract factual information from the provided raw text and write an engaging artist biography."

**User Prompt Structure:**
```
Raw Text: [INSERT_EXTRACTED_TEXT]

Instructions:
1. Write a 150-word biography.
2. Tone must be professional and factual.
3. Do not invent or hallucinate information not present in the text.
4. Output EXACTLY in the following JSON format:
{
  "biography": "The generated text here...",
  "keyGenres": ["Genre1", "Genre2"]
}
```

## Response Validation
The `AiResponseValidator` will:
1. Parse the string into a JSON Object.
2. Check if the `biography` field exists and is not empty.
3. Check if the text length is reasonably close to the requirement (e.g., > 50 words).
4. If validation fails, throw an `AiValidationException`.

## Rate Limit Protection
- **RabbitMQ Prefetch:** Set `spring.rabbitmq.listener.simple.prefetch=1` to ensure the worker only processes one AI request at a time per thread, preventing sudden spikes.
- **Concurrency:** Limit worker concurrency to 2-4 threads depending on the AI provider's Tier quota.
- **Backoff Strategy:** Implement Spring Retry (`@Retryable`) on the `AiProviderClient.generate()` method with exponential backoff for HTTP 429 (Too Many Requests) errors. 

## Failure Recovery
- **Business Errors (Bad PDF, Invalid PDF Format):** Do NOT retry. Mark job as `FAILED` in Redis. Log the error. Acknowledge message to RabbitMQ (discard).
- **Transient Errors (HTTP 429, Timeout, HTTP 500):** Throw exception out to the AMQP container. Spring AMQP will retry 3 times (with backoff).
- **Fatal Transient (Provider Down):** If retries are exhausted, the message is routed to the `ai_queue.dlq` for manual inspection or replay when the provider recovers.

## Result Storage
- **Entity Update:** Add an `artistBiography` (TEXT) field to the `Concert` entity.
- **Persistence:** Upon success, the worker updates the `Concert` record via `ConcertRepository`.
- **Overwrite Protection:** If `concert.getArtistBiography()` is not null, the worker should log a warning and skip the overwrite to protect existing content, unless an `overwrite=true` flag is passed in the message metadata.

## Observability
- **Metrics/Logs:** 
  - `INFO`: Job Started, AI Provider selected, Output generated, Job Completed.
  - `WARN`: Rate limits hit, retry triggered, PDF was image-heavy.
  - `ERROR`: Unrecoverable errors, DLQ routing.
- **Job Tracker:** Use Redis to store real-time job status so the Organizer's UI can poll for completion.

## Risks
1. **Hallucinations:** AI might invent facts not in the PDF.
2. **JSON Parsing Failures:** AI might wrap JSON in markdown blocks (e.g., ` ```json `), breaking strict parsers.
3. **Provider Quotas:** Hitting hard rate limits can stall the queue.

## Recommendations
1. Use a JSON-mode API if supported by the provider (e.g., Gemini structured outputs) to guarantee schema adherence.
2. If JSON mode is unavailable, employ a regex cleaner in the validator to strip markdown fences before parsing.
3. Keep the RabbitMQ prefetch count low to naturally throttle outbound API requests.

## Final Assessment
The designed pipeline cleanly isolates the volatile aspects of AI integration (API limits, JSON instability) behind robust error handling and asynchronous queuing. The architecture is highly resilient and extensible.
