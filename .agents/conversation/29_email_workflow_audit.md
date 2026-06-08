# Email Workflow Coverage Audit

## Objective
Identify all email-related business workflows in the TicketBox project, determine their implementation status across the asynchronous RabbitMQ infrastructure, and highlight architectural inconsistencies.

## Discovered Workflows

Based on `project_requirements.md` and codebase analysis, the following email workflows were identified:

1. **Ticket Purchase Confirmation / E-Ticket Delivery**
   - **Trigger**: Successful payment and order finalization.
   - **Requirement**: Explicitly required by "Khán giả nhận thông báo xác nhận qua app và email kèm e-ticket".
2. **24-Hour Event Reminder**
   - **Trigger**: Scheduled check for concerts occurring in the next 24 hours.
   - **Requirement**: Explicitly required by "Khi concert sắp diễn ra (trước 24 giờ), hệ thống gửi nhắc nhở tự động".
3. **Event Cancellation Notification**
   - **Trigger**: Admin cancels an event.
   - **Requirement**: Implicitly expected but not explicitly stated.
4. **Event Update Notification**
   - **Trigger**: Admin updates an event.
   - **Requirement**: Implicitly expected but not explicitly stated.
5. **Welcome / Registration Email**
   - **Trigger**: User account registration.
   - **Requirement**: Implicitly expected but not explicitly stated.
6. **Password Reset / Verification**
   - **Trigger**: User requests password reset.
   - **Requirement**: Implicitly expected but not explicitly stated.

## Workflow Matrix

| Workflow | Expected | Implemented | Producer Exists | Consumer Exists | End-to-End Functional | Notes |
| -------- | -------- | ----------- | --------------- | --------------- | --------------------- | ----- |
| **Purchase Confirmation / E-Ticket** | Yes (Reqs) | Yes | Yes | Yes | Yes | Restored in previous fix. Uses `EmailTaskMessage` and `RabbitTemplate.convertAndSend()`. |
| **24-Hour Event Reminder** | Yes (Reqs) | No | No | No | No | Completely missing. No `@Scheduled` cron job, no producer, no consumer, no template. |
| **Event Cancellation** | No (Implicit) | No | No | No | No | `ConcertService.cancelConcert` exists but triggers no email. |
| **Event Update** | No (Implicit) | No | No | No | No | `ConcertService.updateConcert` exists but triggers no email. |
| **Welcome / Registration** | No (Implicit) | No | No | No | No | `AuthController.registerUser` exists but triggers no email. |
| **Password Reset** | No (Implicit) | No | No | No | No | API endpoint does not exist. |

## Architectural Concerns

1. **Missing Asynchronous Producers (Non-Email)**:
   - While investigating the message broker topology, a massive architectural inconsistency was discovered outside the email domain. The **CSV Import Worker** (`csv_queue`) has a fully implemented `CsvImportConsumer` and `CsvImportService` that accepts `CsvImportMessage`. However, the `GuestController.importGuests` endpoint bypasses RabbitMQ entirely and synchronously delegates to `GuestService.importGuestsFromCsv`.
   - **Result**: `CsvImportConsumer` is an **orphan consumer**, and the `csv_queue` is an **unused queue**. The CSV parsing blocks the web thread, violating Phase 3's asynchronous streaming design.

2. **Missing Scheduled Jobs**:
   - The required 24-hour reminder email has no cron trigger (`@Scheduled`). The only scheduled job in the entire codebase is `StaleOrderCleanupJob`.

3. **Single-Purpose DTOs & Templates**:
   - `EmailTaskMessage` and `EmailTemplateBuilder` are hardcoded exclusively for the `Order` confirmation and e-ticket PDF generation. They cannot be reused for Event Reminders, Cancellations, or Welcome emails without significant refactoring to support polymorphic payloads or dynamic template resolution.

## Final Report Summary

1. **Total email workflows discovered**: 6 (2 Explicitly Required, 4 Implicitly Expected).
2. **Fully functional workflows**: 1 (Purchase Confirmation / E-Ticket).
3. **Partially implemented workflows**: 0.
4. **Broken workflows**: 0 (Nothing is broken, just missing).
5. **Missing producers**: 1 explicit requirement (24-Hour Event Reminder), 4 implicit. Also, 1 non-email missing producer (`CsvImportMessage` for Guest CSVs).
6. **Missing consumers**: 1 explicit requirement (24-Hour Event Reminder Consumer/Template).
7. **Architectural concerns**: The CSV Import architecture is bypassed by a synchronous HTTP controller, leaving the CSV worker as an orphan consumer. The email architecture is not generic and cannot easily scale to other notification types without refactoring `EmailTaskMessage` into a generic notification envelope.
8. **Recommended next priorities**:
   - **Priority 1**: Refactor `GuestController` to produce `CsvImportMessage` instead of parsing CSVs synchronously, restoring the Phase 3 architecture and utilizing the orphaned `CsvImportConsumer`.
   - **Priority 2**: Implement the `EventReminderJob` (@Scheduled), create a `ReminderEmailMessage` DTO, update `EmailTemplateBuilder`, and add logic to `EmailConsumer` to fulfill the 24-hour reminder requirement.
