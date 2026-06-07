# AI Worker Remediation

## Overview
This document reconciles the findings reported in the `19_ai_worker_review.md` assessment with the fixes that were subsequently implemented to resolve them. The worker design and initial implementation were fundamentally sound, but required configuration tweaks and increased parser robustness to be production-ready.

## Issues Found & Root Causes

### 1. RabbitMQ Rate Limit Exhaustion (Critical)
**Issue:** The RabbitMQ listener was exposed to massive traffic spikes, potentially overwhelming the AI Provider API quotas.
**Root Cause:** Spring AMQP's default prefetch count is 250. This meant that the worker would aggressively pull up to 250 jobs simultaneously and attempt to process them, bypassing any intended concurrency limits.

### 2. Brittle JSON Parsing (Important)
**Issue:** Responses from the AI provider were frequently causing parsing errors and failing jobs.
**Root Cause:** The markdown fence stripping logic used a rigid `String.startsWith("```json")` approach. If the LLM returned conversational prefix text (e.g., "Here is your JSON response: \n```json"), the validation failed and the valid JSON was rejected.

### 3. Missing API Observability (Minor)
**Issue:** Lack of visibility into API request latency and connection health.
**Root Cause:** The `GeminiAiClient` and `OpenAiClient` implementations omitted telemetry logging, creating a blind spot between the internal orchestrator and the external API.

### 4. Missing Failure Test Coverage (Minor)
**Issue:** The validation logic for rejecting bad JSON was untested.
**Root Cause:** `AiWorkerTest` only verified the golden path (valid JSON) and the empty PDF path, but lacked a test simulating a structurally invalid AI response.

---

## Fixes Applied

### 1. RabbitMQ Prefetch Remediation
- **Action:** Enforced strict message throttling at the AMQP listener level.
- **File Modified:** `application.yml`
- **Change:** Configured `spring.rabbitmq.listener.simple.prefetch: 1`. This restricts the worker to pulling exactly 1 message at a time, ensuring processing rate remains controlled and AI quotas are protected.

### 2. JSON Parsing Improvements
- **Action:** Refactored the markdown extraction algorithm for maximum flexibility.
- **File Modified:** `AiResponseValidator.java`
- **Change:** Replaced the `startsWith` checks with a robust `indexOf` and `substring` extraction. It now intelligently locates the ````json` block anywhere in the string. Furthermore, if markdown fences are completely missing, it falls back to extracting the raw JSON string by isolating the bounds between the first `{` and the last `}`.
- **Change:** Corrected exception shadowing. Caught `JsonProcessingException` directly instead of a generic `Exception` to ensure the original validation message isn't swallowed.

### 3. Logging Improvements
- **Action:** Added request and response telemetry.
- **Files Modified:** `GeminiAiClient.java`, `OpenAiClient.java`
- **Change:** Annotated both classes with `@Slf4j`. Added `log.info` statements immediately before invoking `restTemplate.postForObject` and immediately upon receiving a response.

### 4. Test Additions
- **Action:** Verified safe handling of LLM hallucinations.
- **File Modified:** `AiWorkerTest.java`
- **Change:** Added `testInvalidJsonResponse()` which mocks an API response missing the required `biography` JSON key. 
- **Result:** The test successfully verified that the worker gracefully catches the validation error, aborts the DB save, and logs the job status as `FAILED` in Redis without entering a needless AMQP retry loop.
