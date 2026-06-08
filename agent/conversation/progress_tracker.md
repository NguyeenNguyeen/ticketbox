# TicketBox Frontend - Progress Tracker

## Phase 1: Project Setup & Configuration ✅
- [x] Initialize Next.js 14 (App Router) with TypeScript
- [x] Install dependencies (Zustand, React Query, Recharts, etc.)
- [x] Configure Tailwind CSS with custom design system
- [x] Setup SEO metadata & Inter Vietnamese font
- [x] Create root layout with Providers

## Phase 2: Type System & Foundation ✅
- [x] Create type definitions (concert, seat, order, user)
- [x] Create utility functions (formatCurrency, formatDate, cn, etc.)
- [x] Create constants (zone colors, labels, prices)
- [x] Create mock JWT auth utilities
- [x] Create API client wrapper
- [x] Fix TypeScript error in api.ts (empty object generic cast)

## Phase 3: State Management ✅
- [x] Auth store (useAuthStore) - mock login, role-based, localStorage
- [x] Seat store (useSeatStore) - per-seat selectors, toggle selection
- [x] Cart store (useCartStore) - checkout flow, idempotency key
- [x] Custom hooks (useCountdown, useIdempotencyKey, useSSE)

## Phase 4: Mock Data ✅
- [x] Mock concerts (4 concerts with Vietnamese details)
- [x] Mock seats (seeded random, stadium layout)
- [x] Generate concert banner images (4 AI-generated)

## Phase 5: UI Components ✅
- [x] Button (CVA variants, loading state)
- [x] Card (compound component)
- [x] Badge (6 variants)
- [x] Input (forwardRef, error display)
- [x] Dialog (modal with blur overlay)
- [x] Toast (notification system with provider)
- [x] Skeleton (shimmer loading)

## Phase 6: Layout Components ✅
- [x] Header (sticky, auth dropdown, mobile hamburger)
- [x] Footer (company info, links)
- [x] AdminSidebar (collapsible, active highlight)

## Phase 7: Feature Components ✅
- [x] ConcertCard (premium card with hover animation)
- [x] ConcertGrid (search + status filter tabs)
- [x] ConcertInfo (detailed info with ticket table)
- [x] SeatMap (SVG seat map with curved stadium layout)
- [x] Seat (React.memo, per-seat subscription, tooltip)
- [x] ZoneLegend (color indicators + availability)
- [x] OrderSummary (grouped by zone)
- [x] CountdownTimer (color transitions, pulse)
- [x] PaymentMethodSelector (VNPAY + MoMo)
- [x] CheckoutForm (full checkout flow — ALL items sent, idempotency key in body)
- [x] TicketSelector (maxPerUser from API, zone color dot, limit label)
- [x] ETicket (QR code, perforated edge design)
- [x] StatsCards (admin dashboard)
- [x] RevenueChart (Recharts line chart)
- [x] ConcertForm (zod validation, dynamic ticket categories, missing api import fixed)

## Phase 8: Pages ✅
- [x] Homepage (hero + concert grid)
- [x] Concert Detail (/concerts/[id])
- [x] Checkout (/checkout)
- [x] E-ticket (/tickets/[id])
- [x] Login (/auth/login) with Suspense
- [x] Register (/auth/register)
- [x] Admin Dashboard (/admin)
- [x] Admin Concerts CRUD (/admin/concerts)
- [x] Admin Guests (/admin/guests)
- [x] Admin Orders (/admin/orders)
- [x] Admin Users (/admin/users)
- [x] Payment Callback (/payment/callback) — backend verify call, VNPAY error codes

## Phase 9: Security ✅
- [x] Middleware for /admin/* route protection
- [x] JWT token in cookie for middleware checks
- [x] Role-based UI rendering

## Phase 10: Build & Verification ✅
- [x] TypeScript compilation: PASSED (0 errors after fix)
- [x] All routes generated successfully

## Phase 11: Backend & Connection Integration ✅
- [x] Real REST API integration for authentication and registration
- [x] Real REST API connection for concert listing & concert details
- [x] Dynamic seat availability mapping based on database ticket category quantities
- [x] Complete purchase flow integration via `/api/tickets/purchase` (all items, idempotency in body)
- [x] Real QR code ticket lookup via `/api/tickets/{id}`
- [x] Admin: create concert via `POST /api/admin/concerts`
- [x] Admin: update concert via `PUT /api/admin/concerts/{id}`
- [x] Admin: cancel concert via `DELETE /api/admin/concerts/{id}`
- [x] Payment callback verify via `POST /api/payments/verify`

## Phase 12: Bug Fixes & Completeness Audit ✅
- [x] Fix CheckoutForm: only sent items[0] → now sends ALL items sequentially
- [x] Fix idempotency key: was in Header → now in request body (matches backend DTO)
- [x] Fix admin concert edit: mock handleSubmit → real PUT API call
- [x] Fix admin concert new: mock handleSubmit → real POST API call
- [x] Fix admin concerts list: alert() cancel → real DELETE API call
- [x] Fix TicketSelector: hardcoded max=10 → uses maxPerUser from API
- [x] Fix concert status: hardcoded "ON_SALE" → dynamic getEffectiveStatus()
- [x] Fix saleStartTime: hardcoded string → real DB value
- [x] Add per-user limit enforcement in backend TicketPurchaseService
- [x] Add PaymentController with /api/payments/verify endpoint
- [x] Add ConcertController admin CRUD endpoints (POST, PUT, DELETE)
- [x] Add Concert.cancelledStatus and Concert.saleStartTime fields
- [x] Fix TypeScript api.ts empty object generic type error

## Remaining Work
- [ ] Real SSE connection for seat updates (requires active backend event streams)
- [ ] E2E testing with Playwright/Cypress
- [ ] Performance optimization (React Profiler)
- [ ] Accessibility audit
