# Current Project State

## Current project phase
Maintenance & Bug Fixing (AI Press Kit Language Fix Completed)

## Existing architecture status
- Frontend: Next.js (React), Zustand for state management
- Backend: Spring Boot, PostgreSQL, Redis, RabbitMQ
- Integrations: Sandbox Payment Gateway, Resend (for emails), Gemini/OpenAI (for AI Press Kit)

## Existing implementations
- Authentication and User Management
- Concerts and Ticket Categories viewing
- Shopping Cart and Checkout Flow
- Order creation, payment processing (Sandbox) and Ticket generation
- Emailing E-tickets via RabbitMQ and Resend
- Guest Import with strict CSV validation
- AI Press Kit generation (enforced Vietnamese output)

## Missing implementations
- Production deployment configuration
- Real Payment Gateway (VNPAY/MoMo) integrations (currently using sandbox)

## Recommended next step
- Conduct End-to-End testing for the multi-item checkout flow to ensure emails are correctly sent with multiple ticket PDFs.
- Begin work on integrating a real Payment Gateway.
