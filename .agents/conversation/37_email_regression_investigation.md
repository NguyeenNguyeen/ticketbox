# Email Delivery Regression Investigation

## 1. Investigation Scope
This document details the investigation into why the email delivery workflow started failing (emails not received) after the application's configuration loading strategy was migrated from `spring-dotenv` to standard OS environment variables. The investigation focused on tracing the exact point of failure, inspecting the current configuration injection behavior, and ruling out infrastructure issues.

## 2. Workflow Trace
1. **Purchase Request**: `TicketController` → `TicketPurchaseService` (Success)
2. **Order Finalization**: `finalizeOrderSuccess()` saves the order (Success)
3. **Message Production**: `EmailTaskMessage` is created and published to RabbitMQ (Success)
4. **Message Consumption**: `EmailConsumer` picks up the message (Success)
5. **Job Processing**: `EmailService` fetches the order, generates the PDF, and builds the HTML (Success)
6. **Provider Invocation**: `ResendEmailClient` issues a POST request to the Resend API with the headers and payload (Success)
7. **Provider Response**: Resend API evaluates the payload. **(FAILURE POINT)**

## 3. Findings
During the investigation, the active `.env` file was inspected, and the variables loaded into the Spring Boot process via the new `set -a && source .env` method were verified using a custom Java validation script (`EnvChecker.java`).

The findings are as follows:
- `RESEND_API_KEY` is successfully injected.
- `EMAIL_FROM` is successfully injected as `tickets@ticketbox.vn`.
- Spring Boot successfully resolves `${EMAIL_FROM:onboarding@resend.dev}` to `tickets@ticketbox.vn`.

## 4. Root Cause
The root cause is a **Resend Sandbox Policy Violation caused by successful configuration overriding**.

In the Resend Sandbox mode, emails can **only** be sent from `onboarding@resend.dev`. Any attempt to send emails from an unverified domain (like `tickets@ticketbox.vn`) results in an **HTTP 403 Forbidden** response.

Because the `.env` file currently contains `EMAIL_FROM=tickets@ticketbox.vn`, the new OS environment variable loading mechanism successfully injects this production value into the Spring Boot application, overriding the safe default (`onboarding@resend.dev`). 

When `ResendEmailClient` sends the payload with `"from": "tickets@ticketbox.vn"`, the Resend API rejects the request. `EmailService` correctly catches the 4xx `HttpClientErrorException`, marks the order's `emailStatus` as `FAILED`, and throws an `AmqpRejectAndDontRequeueException`, immediately sending the message to the Dead Letter Queue (DLQ). Thus, the user never receives the email.

## 5. Alternative Hypotheses Ruled Out
- **RabbitMQ or Consumer Failure**: Ruled out. The workflow executes all the way to the external API call.
- **Environment Variable Parsing Failure**: Ruled out. `EnvChecker.java` confirmed that `RESEND_API_KEY` and `EMAIL_FROM` are correctly passed to the JVM.
- **API Key Missing**: Ruled out. The API key is valid; the rejection is specifically due to the sender domain (`tickets@ticketbox.vn`).

## 6. Confidence Level
**100%**. A direct `curl` command using the exact `RESEND_API_KEY` and `from: tickets@ticketbox.vn` payload reproduced the exact `403 Forbidden` error with the message: `"The ticketbox.vn domain is not verified"`.
