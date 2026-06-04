# Phase 4: AI Worker Implementation

## Files Created
- `AiBioMessage.java`: AMQP Message DTO.
- `AiJobProgress.java` & `AiJobTracker.java`: Redis status tracking mechanism.
- `UnprocessablePdfException.java` & `AiValidationException.java`: Domain exceptions for flow control.
- `PdfExtractionService.java`: PDF text extractor using Apache PDFBox v3.
- `AiPromptBuilder.java`: Template engine for deterministic JSON AI prompting.
- `AiProviderClient.java`, `GeminiAiClient.java`, `OpenAiClient.java`: Replaceable strategy pattern for LLMs via Spring `RestTemplate`.
- `AiResponseValidator.java`: Markdown stripper and JSON schema validator for AI responses.
- `AiBioService.java`: Core orchestration logic.
- `AiBioConsumer.java`: Async RabbitMQ consumer.
- `AiWorkerTest.java`: End-to-end integration test suite.
- `AppConfig.java`: Configures the `RestTemplate` bean.

## Files Modified
- `pom.xml`: Added `pdfbox` and `spring-retry`.
- `Application.java`: Added `@EnableRetry`.
- `Concert.java`: Added `artistBiography` column (TEXT).

## Architecture Alignment
The implementation aligns exactly with `17_ai_worker_design.md`. The pipeline gracefully handles PDFs, extracts text, safely queries the configured LLM API without heavy SDK lock-in, strips hallucinated markdown fences, and inserts the generated biography directly into the database. 

## PDF Processing Flow
`PdfExtractionService` relies on `Apache PDFBox 3.0.2`. It parses the local file and extracts its text via `PDFTextStripper`. If the output is empty or shorter than 20 characters (indicative of a scanned image), it throws an `UnprocessablePdfException`.

## AI Provider Integration
Integration uses Spring's native `RestTemplate`. Two implementations (`GeminiAiClient`, `OpenAiClient`) exist. Selection is strictly configuration-driven via `@ConditionalOnProperty("ticketbox.ai.provider")`. They wrap their native responses into a standard extracted text string, avoiding deep vendor lock-in.

## Validation Strategy
The `AiResponseValidator` strips common LLM JSON markdown fences (e.g., ` ```json `) to ensure Jackson can parse the raw string. It then validates the schema against the requested `biography` field and rejects abnormally short content with an `AiValidationException`.

## Retry Strategy
Business logic errors (Unprocessable PDF, JSON Validation failure) mark the Redis job as `FAILED` and gracefully drop the message. System errors (`RestClientException`, timeouts) are re-thrown to the listener. The Spring AMQP `RetryInterceptorBuilder` automatically catches these and attempts three back-to-back retries with exponential backoff (1s, 2s, 4s). 

## DLQ Strategy
If an AI provider remains offline and exhausts the 3 AMQP retries, the `RepublishMessageRecoverer` intercepts the failure and securely routes the original message to `ticketbox.dlx` with routing key `ai.generate`, landing it safely in `ai_queue.dlq`.

## Test Coverage
- **Successful Generation**: End-to-end validation including DB insertion and Redis `COMPLETED` tagging.
- **Empty PDF**: Asserts that `UnprocessablePdfException` skips DB insertion and marks Redis as `FAILED`.
- **Timeout and DLQ**: Mocks a continuous API timeout, asserts the AMQP retry loop completes, verifies the DB remains unchanged, and confirms the message physical arrival in `ai_queue.dlq`.

## Known Limitations
- The current prompt generation relies heavily on the AI following strict JSON output instructions. While the regex scraper mitigates markdown wrapping, extremely malformed AI responses will still trigger a validation exception and drop the message.
- OCR for scanned PDFs is unsupported. Image-only PDFs will fail fast.
