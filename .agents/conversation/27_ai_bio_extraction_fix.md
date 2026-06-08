# AI Bio Extraction Fix

## Objective
The objective was to fix the AI Artist Bio Extraction process which was failing due to a 4xx API Key Authentication error from the Gemini API and presenting misleading UI states during the extraction.

## Decisions Made
- **Frontend Misleading UI Fixed**: The 3-second `setTimeout` mock inside `apps/frontend/src/components/admin/ConcertForm.tsx` was removed. The application now properly indicates that processing will happen upon form submission.
- **Backend Configuration Injection**: The `GEMINI_API_KEY` was empty because the backend was unaware of the root directory `.env` file. We implemented the official `me.paulschwarz:spring-dotenv` dependency into the `apps/backend/pom.xml` to seamlessly parse root-level `.env` files and expose them as properties.
- **API Model Upgrade**: During validation, we found that the legacy `gemini-1.5-flash` endpoint returned a 404. We updated `apps/backend/src/main/resources/application.yml` to point to the modern model identifier `gemini-2.5-flash`.

## Files Created
None

## Files Modified
- `apps/frontend/src/components/admin/ConcertForm.tsx` (Removed fake `setTimeout` state).
- `apps/backend/pom.xml` (Added `spring-dotenv` dependency).
- `apps/backend/src/main/resources/application.yml` (Configured `spring.dotenv.directory` and upgraded Gemini endpoint).

## Dependencies
- Require `me.paulschwarz:spring-dotenv` version `4.0.0` in the backend.

## Remaining Work
- End-to-end load testing on the rabbitmq queue to ensure parallel AI bio generation functions well under stress.

## Open Questions
- None. The feature correctly processes PDFs asynchronously and persists biographies without disrupting the UX.
