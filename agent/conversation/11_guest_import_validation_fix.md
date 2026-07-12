# 11 Guest Import Validation Fix

## Objective
Fix a regression in the Guest Import pipeline where invalid CSV rows (e.g. missing name, missing email, or invalid email format) were still being inserted into the database, generating tickets, and triggering RabbitMQ email messages. The objective was to strictly enforce validation so invalid rows are completely skipped.

## Decisions Made
- Discovered that the application has two CSV import pathways: an asynchronous worker (`CsvImportService`) which properly used `CsvRowValidator` but lacked ticket generation logic, and a synchronous endpoint (`GuestService.importGuestsFromCsv`) which handled the full upsert and ticket logic but bypassed `CsvRowValidator` entirely in favor of a weak `isEmpty()` check.
- Decided to centralize and reuse `CsvRowValidator` across both pipelines by refactoring its signature.
- Refactored `CsvRowValidator` to accept raw strings (`String fullName, String email`) so it can be invoked by `GuestService` (which parses columns by index) without requiring `CSVRecord` named mapping.
- Injected `CsvRowValidator` into `GuestService` and applied it directly to the parsing loop, ensuring that `continue;` is executed before any persistence logic if a row is invalid.

## Files Created
- `apps/backend/src/test/java/com/ticketbox/backend/service/GuestServiceTest.java` (Unit Test)

## Files Modified
- `apps/backend/src/main/java/com/ticketbox/backend/worker/csv/CsvRowValidator.java`
- `apps/backend/src/main/java/com/ticketbox/backend/service/GuestService.java`

## Dependencies
- `GuestService` now depends on `CsvRowValidator`.

## Remaining Work
- The asynchronous `CsvImportService` currently exists but does not perform upsert logic or generate tickets. It may need to be aligned with `GuestService` or removed if redundant.

## Open Questions
- None.
