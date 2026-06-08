# TicketBox - Current Project State

## Current Phase
**Phase 12: Bug Fixes & Completeness Audit** — ✅ COMPLETED

## Project Status
All critical bugs have been identified and fixed. The system is now complete with real API connections throughout, no mock data in production flows, and proper backend enforcement of business rules.

## Architecture Status

### Technology Stack (Implemented & Connected)
- **Framework**: Next.js 16.2.7 (App Router) + TypeScript
- **Styling**: Tailwind CSS v4 with custom design system (light theme)
- **State**: Zustand (authStore, cartStore — connected to real APIs)
- **Data & APIs**: Real REST integration via native `fetch` calling Spring Boot backend
- **QR Code**: qrcode.react (rendering real ticket UUID from backend)
- **Forms**: react-hook-form + zod v4

### Backend Architecture
- **Framework**: Spring Boot (Java 25, PostgreSQL, Redis, RabbitMQ)
- **Security**: JWT + Role-based (CUSTOMER, ORGANIZER, CHECKER) + Rate Limiting
- **Concurrency**: Pessimistic locking + Distributed Redis lock + Idempotency key
- **Per-user limit**: Enforced in `TicketPurchaseService.reserveTickets()` via DB query
- **Circuit Breaker**: Resilience4j on PaymentGatewayService
- **Workers**: AI Bio (Gemini/OpenAI via RabbitMQ), CSV Import, Email (Resend/Brevo)

### Existing Implementations
1. **Design System** — globals.css with HSL variables, animations
2. **Types** — concert.ts, seat.ts, order.ts, user.ts
3. **Foundation** — utils, constants, auth, api, backend integration
4. **Stores** — useAuthStore, useCartStore (full API integration)
5. **UI Components** — Button, Card, Badge, Input, Dialog, Toast, Skeleton
6. **Layout** — Header, Footer, AdminSidebar
7. **Features** — ConcertCard, ConcertGrid, ConcertInfo, TicketSelector (API maxPerUser), CheckoutForm (multi-item), ETicket, StatsCards, RevenueChart, ConcertForm
8. **Pages** — All routes connected to real APIs
9. **Security** — middleware.ts protecting /admin routes via JWT cookies

### API Connections (All Fixed)

| Frontend Call | Backend Endpoint | Status |
|---|---|---|
| `POST /auth/login` | AuthController | ✅ |
| `POST /auth/register` | AuthController | ✅ |
| `GET /concerts` | ConcertController | ✅ Dynamic status |
| `GET /concerts/{id}` | ConcertController | ✅ Real saleStartTime |
| `GET /concerts/{id}/categories` | ConcertController | ✅ Real maxPerUser |
| `POST /tickets/purchase` | TicketController | ✅ idempotencyKey in body, ALL items |
| `GET /tickets/{id}` | TicketController | ✅ |
| `GET /orders/history` | OrderController | ✅ |
| `GET /admin/stats` | AdminStatsController | ✅ |
| `GET /admin/concerts` | → /concerts | ✅ |
| `POST /admin/concerts` | ConcertController | ✅ NEW |
| `PUT /admin/concerts/{id}` | ConcertController | ✅ NEW |
| `DELETE /admin/concerts/{id}` | ConcertController | ✅ NEW |
| `GET /admin/orders` | OrderController | ✅ |
| `GET /admin/users` | UserController | ✅ |
| `GET /admin/guests` | GuestController | ✅ |
| `POST /admin/guests/import` | GuestController | ✅ |
| `POST /admin/concerts/{id}/upload-bio` | AiBioController | ✅ |
| `POST /payments/verify` | PaymentController | ✅ NEW |

### Missing Implementations
- Real SSE server connection (hook is ready, awaits backend event stream deployment)
- Mobile app soát vé (intentionally excluded per user request)
- E2E test suite (Playwright / Cypress)

## Recommended Next Step
1. Start infrastructure: `docker-compose -f infra/docker/docker-compose.yml up -d`
2. Build and start backend via IDE (IntelliJ) or: `java -jar target/ticketbox-backend.jar`
3. Launch Next.js: `npm run dev` inside `apps/frontend/`
4. Login with `customer1/password` → select tickets → checkout → verify all items purchased
5. Login with `admin1/password` → create new concert → verify in concert list
6. Admin → Guests → upload CSV → verify guest list loads
