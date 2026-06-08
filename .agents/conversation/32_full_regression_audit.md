# Full Regression & Production Readiness Audit

## 1. Flows Tested
1. **User Registration**: Tested missing email, invalid email, duplicate email, and successful registration.
2. **User Login**: Tested JWT generation, verified payload claims.
3. **Event Browsing**: Tested concert list and details endpoints.
4. **Ticket Purchase**: Tested idempotency, oversell protection, and successful Order creation.
5. **E-ticket Delivery**: Tested RabbitMQ queue publishing and Email Consumer processing.
6. **Admin Event Management**: Tested Concert creation DTO parsing.

## 2. Passed Flows
- **User Registration**: Operates flawlessly; email constraints are properly enforced.
- **Event Browsing**: Operates without issue.
- **Ticket Purchase**: Locks and idempotency correctly prevent race conditions.
- **E-ticket Delivery**: Correctly extracts real user email and passes it to the `ResendEmailClient`.
- **Admin Event Management**: Successfully parses and persists new Concert geometries when correctly formatted.

## 3. Failed Flows
- None. (An initial event creation test failed due to a misformatted API payload on my end, but passing standard ISO-8601 strings correctly fulfilled the contract).

## 4. Critical Regressions Found & Fixed
- **JWT Claim Regression**: I discovered that while `TicketPurchaseService` was correctly using `user.getEmail()`, the `JwtTokenProvider` was still injecting the hardcoded string (`user.getUsername() + "@ticketbox.vn"`) into the `email` claim of the JWT payload. This would have caused the frontend to misidentify user profiles. 
  - **Fix Implemented**: Injected `UserRepository` into `JwtTokenProvider` to query the real email address dynamically during login and embed it in the token. Tested and verified working.

## 5. API Contract Changes
- **Registration**: The `POST /api/auth/register` endpoint now strictly requires an `email` field. Frontend clients MUST be updated to pass this field or they will encounter `400 Bad Request`.

## 6. Production Blockers
- **Email Domain Verification**: The system is mechanically sound, but the external provider (Resend) will block emails with `403 Forbidden` until the DNS TXT records for the chosen domain (e.g., `ticketbox.vn`) are verified in the provider dashboard.

## 7. Remaining Technical Debt
- **Phase C Database Migration**: `User.email` is currently nullable in the JPA entity to support Phase A & B migrations. Once data is verified clean, `@Column(nullable = false)` should be strictly enforced.
- **Orphaned CSV Import**: Discovered in a previous audit, the `CsvImportConsumer` is bypassed. The `GuestController` currently parses CSVs synchronously on the web thread instead of asynchronously.
- **Missing Scheduled Jobs**: The 24-hour Event Reminder workflow lacks a Spring `@Scheduled` producer.

## 8. Recommendations
The system is highly stable following the major architectural overhauls. 
- **Next Priority**: Address the `CsvImportConsumer` architectural mismatch to move guest list uploads into the RabbitMQ async pool. Then, implement the 24-Hour Event Reminder `@Scheduled` job.
