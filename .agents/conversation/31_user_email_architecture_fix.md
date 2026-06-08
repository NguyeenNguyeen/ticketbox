# User Email Architecture Fix

## 1. Problem Summary
The system originally generated e-ticket delivery emails using a hardcoded placeholder address (`user.getUsername() + "@ticketbox.vn"`). This meant that actual end-users could not receive emails, fundamentally breaking the customer notification workflows. The `User` domain entity completely lacked an `email` field.

## 2. Root Cause
During the early authentication development phase (Phase 1.5), the `email` field was missed. Later, when the asynchronous Email Worker was built (Phase 3), the hardcoded string concatenation hack was added to fulfill the `EmailTaskMessage` contract so the RabbitMQ integration could compile and run. This oversight masked the underlying architectural defect.

## 3. Database Changes
- Added an `email` column to the `users` table via Hibernate (`ddl-auto: update`).
- Updated the `data.sql` script to explicitly map realistic emails to the three seeded demo users (`customer1@gmail.com`, `admin1@ticketbox.vn`, `checker1@ticketbox.vn`).

## 4. Entity Changes
- Added `@Column(unique = true) private String email;` to the `User.java` JPA entity. (Note: `nullable = false` was intentionally omitted during Phase A of the migration to protect legacy databases).

## 5. DTO/API Changes
- `RegisterRequest` in `AuthController.java` was updated to include a `String email;` property.
- `AuthController.registerUser()` now enforces strict format validation (Regex: `^[A-Za-z0-9+_.-]+@(.+)$`).
- Added database validation via `UserRepository.existsByEmail()` to reject duplicate emails with a `400 Bad Request`.

## 6. Migration Strategy
To ensure existing development environments did not break:
1. **Phase A (Nullable Schema Update)**: The `User` entity was updated without the `nullable = false` constraint so that Spring Boot's Hibernate `update` policy would alter the table without throwing constraint exceptions on pre-existing rows containing NULL.
2. **Phase B (Backfill)**: The `data.sql` script automatically backfills the default seeded users on restart. For any dynamic legacy users created in previous sessions, a manual SQL backfill was prepared (`UPDATE users SET email = username || '@ticketbox.vn' WHERE email IS NULL;`).

## 7. Validation Results
- **Registration**: Successfully tested missing email (400), invalid email format (400), duplicate email (400), and valid new registration (200 OK).
- **Email Producer**: `TicketPurchaseService.java` successfully resolves and publishes the real user email to the `ticketbox.commands` exchange.
- The logs confirm the Resend Email Client now transmits payloads to the actual registered user email (`n***@gmail.com`).

## 8. Regression Results
- **Authentication/Login**: `AuthController.login()` continues functioning flawlessly.
- **Purchase Flow**: `TicketPurchaseService` operates smoothly; e-tickets are generated and the RabbitMQ integration reliably picks up the tasks.
- No other core business logic (e.g., ticket categories, artist bio AI) was affected by this domain model expansion.

## 9. Future Considerations
- Migrate the `@Column(unique = true)` to include `nullable = false` in `User.java` once all staging/production environments have run the SQL backfill script (Phase C).
- Implement standard Spring Boot validation constraints (`@Email`, `@NotBlank`) within the DTOs instead of using manual regex inside the controllers to standardise constraint definitions across the API surface.
