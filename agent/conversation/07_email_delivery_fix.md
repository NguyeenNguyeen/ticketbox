# Email Delivery Fix

## Objective
The goal was to implement the missing producer logic for the Email Delivery architecture, connecting the `TicketPurchaseService` order completion to the `EmailConsumer` RabbitMQ queue without breaking any existing synchronous behavior or idempotency handling.

## Decisions Made
- **Non-blocking Event Publication**: RabbitMQ `convertAndSend` is used after database transactions complete to ensure the purchase is persisted first.
- **Data Encapsulation**: The user's mock email address was constructed via `username + "@ticketbox.vn"` matching the existing `TicketController` schema.
- **Job Identifiers**: Added a newly generated `UUID` for the `EmailTaskMessage` job tracker.
- **RabbitTemplate Injection**: Used standard Spring `@Autowired` injection for `RabbitTemplate` inside `TicketPurchaseService`.

## Files Created
None

## Files Modified
- `apps/backend/src/main/java/com/ticketbox/backend/service/TicketPurchaseService.java`

## Workflow Added
1. Upon `finalizeOrderSuccess`, iterate over created tickets and store their IDs into an `ArrayList`.
2. Construct `EmailTaskMessage` passing `jobId`, `orderId`, `userId`, `recipientEmail`, `ticketIds`, and the `idempotencyKey`.
3. Call `rabbitTemplate.convertAndSend` to the `ticketbox.commands` exchange with routing key `email.send`.

## Dependencies
- RabbitMQ infrastructure.

## Remaining Work
- The demo environment currently uses placeholder API Keys for Resend/Brevo resulting in a 403 Forbidden rejection during the provider request phase. These API Keys must be updated in `.env` to actually dispatch real emails.

## Open Questions
- None. The RabbitMQ and worker integration functions perfectly, preserving state, idempotency, and non-blocking asynchronous email delegation.
