# AI Worker Review

## Review Scope
This review evaluates the AI Worker implementation against `17_ai_worker_design.md` focusing on architecture, PDF safety, provider isolation, prompt formatting, JSON parsing, retry resilience, rate limiting, and test coverage.

## Architecture Findings
- **Separation of Concerns:** Excellent. The listener is thin, delegating to `AiBioService`, which effectively orchestrates the isolated PDF, Prompt, Provider, and Validation layers.
- **Provider Abstraction:** The `AiProviderClient` effectively shields the rest of the app from vendor-specific payload structures.

## PDF Processing Findings
- **Memory Safety:** `PdfExtractionService` uses `Loader.loadPDF()` wrapped in a `try-with-resources` block, ensuring `PDDocument` is safely closed, preventing memory leaks.
- **Error Handling:** Correctly detects empty or short PDFs (image-only) and fails-fast with a custom `UnprocessablePdfException`.

## Provider Abstraction Findings
- Both `GeminiAiClient` and `OpenAiClient` cleanly construct custom JSON payloads via `RestTemplate` instead of importing heavy vendor SDKs.
- **Flaw:** Neither client implements any form of internal logging. Given the unreliability and cost of LLMs, missing telemetry on request generation and API latency is a significant observability gap.

## Prompt Findings
- The prompt is strictly formatted requesting JSON output without markdown fences.
- It correctly enforces the 150-word marketing biography requirement.

## Response Validation Findings
- **Flaw:** The markdown stripping logic in `AiResponseValidator` uses `cleaned.startsWith("```json")`. If the LLM produces introductory text (e.g., "Here is your output:\n```json..."), the check fails, the parser crashes, and the job is marked `FAILED`. This makes the worker brittle. It must use a regex or substring extraction approach.

## Retry Findings
- The service correctly defers transient HTTP errors (`RestClientException`) to the Spring AMQP `RetryInterceptorBuilder`.
- Unrecoverable business errors (`AiValidationException`, `UnprocessablePdfException`) are caught and logged, gracefully updating the job state to `FAILED` without triggering a needless retry loop.

## Storage Findings
- `AiBioService` performs a pre-flight check on `concert.getArtistBiography()` and only proceeds if it is null/empty or if an `overwrite` flag is explicitly passed in the message metadata. Data safety is guaranteed.

## Observability Findings
- **Flaw:** Missing logs for actual AI requests/responses inside the provider implementations.
- Other layers log start/skip/fail/complete events appropriately.

## Test Findings
- `AiWorkerTest` tests the golden path, empty PDF path, and API timeout DLQ path.
- **Flaw:** There is no test coverage for malformed JSON parsing failures (e.g., triggering `AiValidationException`).

## Rate Limit Findings
- **Flaw:** `RabbitMQConfig` and `application.yml` do not explicitly set the listener prefetch count. Spring AMQP defaults to a prefetch of 250. If an organizer uploads 200 PDFs, the worker will aggressively pull all 200 into memory and saturate the configured threads, leading to immediate HTTP 429 quota exhaustion from the AI provider. Prefetch must be set to 1.

## Risks
**Critical Risks:**
- Provider Rate Limit exhaustion due to default AMQP prefetch count of 250.

**Important Risks:**
- High failure rate for valid AI responses due to brittle `startsWith` markdown fence parsing.

**Minor Risks:**
- Poor observability during production debugging due to missing API logs.
- Missing test coverage for invalid JSON.

## Recommendations
1. Update `application.yml` to set `spring.rabbitmq.listener.simple.prefetch=1`.
2. Refactor `AiResponseValidator.stripMarkdownFences` to use Regex or `indexOf` to locate the JSON block within surrounding text.
3. Add `log.info` telemetry inside `GeminiAiClient` and `OpenAiClient`.
4. Add `testInvalidJsonResponse()` to `AiWorkerTest`.

## Final Assessment
The implementation is architecturally sound but contains configuration and parsing flaws that jeopardize reliability in a production environment. 

NOT READY FOR AI WORKER VALIDATION
