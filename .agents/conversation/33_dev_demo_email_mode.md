# Development/Demo Email Mode

## 1. Problem Summary
The Resend API imposes strict sandbox limitations, requiring that emails only be sent from `onboarding@resend.dev` to the registered account email unless a custom sender domain is verified via DNS. Because the backend source code hardcoded the sender address as `tickets@ticketbox.com`, the RabbitMQ worker consistently failed to deliver test emails during local development, blocking end-to-end demo validation.

## 2. Sender Configuration Changes
The hardcoded sender addresses were removed from the provider clients and externalized to the Spring configuration properties.
- **New Property**: `ticketbox.email.from`
- **Default Value**: `onboarding@resend.dev`
- **Environment Variable**: `EMAIL_FROM`

This allows the application to cleanly default to the Resend sandbox sender out-of-the-box, ensuring immediate compatibility with local development.

## 3. Files Modified
- **`application.yml`**: Added `from: ${EMAIL_FROM:onboarding@resend.dev}` under the `ticketbox.email` block.
- **`ResendEmailClient.java`**: Replaced `private static final String SENDER_EMAIL` with `@Value("${ticketbox.email.from}") private String senderEmail;`.
- **`BrevoEmailClient.java`**: Replaced `private static final String SENDER_EMAIL` with `@Value("${ticketbox.email.from}") private String senderEmail;`.

## 4. Validation Results
1. **Application Start**: Started successfully without bean injection errors.
2. **Email Workflow**: `EmailTaskMessage` processing and PDF generation remained unaffected.
3. **Resend Invocation**: By creating a test user with the sandbox-authorized email and executing a ticket purchase, the worker successfully passed `onboarding@resend.dev` as the sender. Resend returned a `200 OK` status, and the order transitioned to `emailStatus: SENT`.

## 5. Development-Mode Usage
To use the application locally or for academic demos:
1. Start the application normally without changing any configuration. It defaults to `onboarding@resend.dev`.
2. Ensure you register users with the exact email address authorized in your Resend account dashboard.
3. Purchase tickets and observe the emails arriving in your inbox.

## 6. Production Migration Instructions
When migrating the application to a staging or production environment with a verified domain (e.g., `ticketbox.vn`), **no source code changes are required**.

Simply inject the desired sender address via environment variables:
```bash
export EMAIL_FROM="tickets@ticketbox.vn"
```
Or define it in an external `.env` file loaded by the runtime environment. The Spring Boot application will automatically prioritize `EMAIL_FROM` over the `onboarding@resend.dev` fallback.
