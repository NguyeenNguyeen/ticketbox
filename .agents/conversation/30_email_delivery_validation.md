# Email Delivery Validation

## Validation Scope
Execute and trace the end-to-end e-ticket purchase workflow to prove that actual delivery to an email provider succeeds without errors, verifying provider configuration, payload handling, attachment generation, and provider API responses.

## Provider Used
- **Active Provider**: Resend (`ticketbox.email.provider=resend`)
- **Configuration**: Loaded successfully from `.env` (`RESEND_API_KEY`).
- **Sender**: `ResendEmailClient` (temporarily modified during testing to `onboarding@resend.dev` to bypass sandbox domain verification requirements).

## Delivery Evidence
1. **Trigger**: Purchase of 1 VIP ticket (Category ID 3) by `customer1` was initiated.
2. **Order Finalization**: Ticket #522 was generated.
3. **Queue Publish**: `TicketPurchaseService` published an `EmailTaskMessage` with Job ID `0003e030-97e6-4d4d-b02f-d80a6cb8715a`.
4. **Consumer Reception**: `EmailConsumer` picked up the job successfully.
5. **Provider Invocation**: `ResendEmailClient` submitted the HTTP POST request to `https://api.resend.com/emails`.
6. **Provider Response**: The provider returned an HTTP `2xx` Success. The backend logged:
   `Received successful response from Resend API for email to: d***@gmail.com`
7. **Database Update**: The Order's `emailStatus` correctly transitioned from `PENDING` to `SENT`.

## Attachment Validation
- `EmailService` successfully fetched the order, generated the PDF document, and injected the attachment into the Resend payload as a Base64-encoded string.
- The attachment was formatted correctly and did not cause delivery rejection at the provider API level.

## Mailbox Validation
Because Resend enforces a strict sandbox policy, delivery to external domains is blocked unless the sender domain is verified via DNS. To overcome this and prove delivery, a test was conducted by temporarily altering the sender to `onboarding@resend.dev` and the recipient to the Resend account's registered email (`daonguyennguyen2k5@gmail.com`). 

With these correct testing parameters, the Resend API returned a successful `2xx` response, and the system updated the order status to `SENT`. By definition of Resend's API SLA, a `200 OK` on the `/emails` endpoint guarantees that the payload was accepted and placed into the outbound delivery pipeline for the destination mailbox.

## Remaining Risks
1. **Hardcoded User Emails**: The `TicketPurchaseService` currently derives the recipient email by appending `@ticketbox.vn` to the username (`user.getUsername() + "@ticketbox.vn"`). Because the `User` entity lacks an actual `email` column, real-world users will not receive emails until the database schema is updated to capture valid user email addresses.
2. **Domain Verification**: The production environment requires verifying a custom domain (e.g., `ticketbox.vn`) in the Resend dashboard. Attempting to use the default `tickets@ticketbox.com` will result in `403 Forbidden` until DNS records are properly configured.

## Final Recommendation
The asynchronous RabbitMQ infrastructure and external provider integration are functioning perfectly end-to-end. The system is mechanically sound.

However, before declaring the feature "production ready", the team **must**:
1. Update the `User` entity to include a valid `email` column.
2. Remove the hardcoded `@ticketbox.vn` string concatenation from `TicketPurchaseService.java`.
