# Current Project State

## Current Project Phase
Async Infrastructure and Worker Implementation Phase (Completed)

## Existing Architecture Status
- RabbitMQ infrastructure is fully functional with dead-letter queue (DLQ) routing and exponential backoff retry mechanisms.
- Database models (`TicketCategory`, `Concert`, `Ticket`, `Order`, `User`) are properly mapping relationships, although care must be taken in test environments regarding cascading deletes and cleanup order.
- Three major async workflows have been completed: CSV Guest List Import, AI Artist Biography Generation, and Email E-Ticket Delivery.

## Existing Implementations
- `CsvWorker`: Parses CSVs and bulk-adds users to concert guest lists.
- `AiWorker`: Integrates with an external LLM API to generate localized artist biographies.
- `EmailWorker`: Leverages Apache PDFBox to generate in-memory e-tickets (with ZXing QR codes) and delivers them using a hot-swappable external email provider abstraction (Resend/Brevo).

## Missing Implementations
- Synchronous API layer for dispatching these jobs from client requests.
- Notification strategy (Polling vs WebSocket) to inform the frontend when a dispatched job resolves.
- The Mobile application and UI components related to these features.

## Recommended Next Step
- Transition to implementing the backend API controllers that dispatch jobs to RabbitMQ and expose job tracking status from Redis to the frontend UI.
