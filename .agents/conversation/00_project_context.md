# 00 — Project Context

---

## Executive Summary

TicketBox is a high-concurrency online ticketing platform for massive music concerts in Vietnam. The system must handle extreme traffic (80,000 CCU in 5 minutes), prevent ticket overselling, survive payment gateway failures, support offline check-in, and integrate AI and CSV processing asynchronously. The project is built by a team of 4 engineers, each owning a specific domain.

---

## Project Overview

### Problem Statement

Current concert ticketing in Vietnam suffers from:

* Websites crashing within minutes of ticket sales opening
* Users charged without receiving tickets (double-charging)
* Scalper bots hoarding all tickets in seconds
* Manual processes via Zalo OA, Google Form, bank transfers — unfair and fraud-prone

### Solution

TicketBox digitizes the entire ticketing lifecycle — from ticket sales to event gate check-in — with resilience, fairness, and anti-fraud mechanisms built in.

---

## Business Domains

| Domain | Description |
|--------|-------------|
| Concert Management | Create/manage concerts, ticket categories, pricing, schedules |
| Ticket Sales | Real-time SVG seat maps, ticket selection, purchase flow |
| Payment Processing | VNPAY/MoMo integration with fault tolerance |
| E-Ticket & QR | Generate QR-based e-tickets after successful payment |
| Notifications | Email, push notifications, extensible to Zalo OA/SMS |
| Check-in | Offline-capable mobile QR scanning at event gates |
| AI Artist Bio | PDF upload → AI-generated artist summaries |
| CSV VIP Import | Nightly CSV import of sponsor guest lists |
| Admin Dashboard | Revenue tracking, concert management, analytics |

---

## User Roles

| Role | Vietnamese | Capabilities |
|------|-----------|-------------|
| CUSTOMER (Khán giả) | Khán giả | Browse concerts, SVG seat maps, buy tickets, receive QR e-tickets, get notifications |
| ORGANIZER (Ban tổ chức) | Ban tổ chức | Create/manage concerts, configure ticket categories, upload PDF/CSV, view revenue |
| CHECKER (Nhân sự soát vé) | Nhân sự soát vé | Mobile app QR scanning at gates, offline check-in |

External actor: **Sponsor (Nhãn hàng tài trợ)** — provides CSV guest lists

---

## Functional Requirements

### FR-01: Concert Browsing & Ticket Purchase

* View upcoming concerts with artist info, venue, interactive SVG seat map
* Real-time seat availability (GA, SVIP, VIP, CAT1, CAT2)
* Select ticket type and quantity → pay via VNPAY/MoMo → receive QR e-ticket

### FR-02: Per-User Ticket Limits

* Organizer configures max tickets per account per category (e.g., SVIP max 2/user)
* Limit enforced globally across all successful orders
* Users cannot bypass via multiple concurrent small orders

### FR-03: Notifications

* Post-purchase: confirmation via app + email with e-ticket
* Pre-event: automatic reminder 24 hours before concert
* Extensible to new channels (Zalo OA, SMS) via Strategy/Observer patterns

### FR-04: Admin Management

* Create, edit, cancel concerts
* Configure ticket categories (name, price, quantity, sale start time)
* View revenue and sales statistics

### FR-05: Event Check-in

* Mobile app QR scanning
* Must work offline (stadiums with weak/no connectivity)
* Prevent double-entry (same ticket scanned twice)
* Auto-sync scan logs when connectivity restored

### FR-06: AI Artist Bio

* Upload PDF press kit → extract text → clean → send to AI (Gemini/OpenAI)
* Generate concise artist bio for concert detail page
* Must NOT block main thread

### FR-07: CSV VIP Guest List

* Nightly CSV from sponsors (no API available)
* Fault-tolerant import: handle format errors, duplicates
* Must not disrupt running system

### FR-08: RBAC Access Control

* CUSTOMER: view and buy only
* ORGANIZER: full concert management + revenue
* CHECKER: QR scan access only
* Strict enforcement at API endpoint, admin page, and mobile app levels

---

## Non-Functional Requirements

### NFR-01: Extreme Concurrency

* 80,000 CCU in first 5 minutes, 70% in minute 1
* Zero ticket overselling under contention
* Pessimistic Locking + Redis buffering

### NFR-02: Burst Traffic Protection

* Rate Limiting via Token Bucket (Bucket4j + Redis)
* Anti-bot mechanisms
* Fair load distribution among real users

### NFR-03: Payment Fault Tolerance

* Circuit Breaker + Bulkhead (Resilience4j) for payment gateways
* Graceful degradation: non-payment features unaffected during gateway failures
* Idempotency Key to prevent double-charging

### NFR-04: High-Volume Read Optimization

* Cache-aside with Redis
* Static data: long TTL (30 min)
* Dynamic data (ticket availability): short TTL or active invalidation

### NFR-05: Offline-First Check-in

* SQLite local storage on mobile
* RSA asymmetric cryptography for offline QR verification
* Background sync when connectivity restored

### NFR-06: Asynchronous Processing

* Email, CSV, AI tasks offloaded to RabbitMQ
* Dead Letter Queue for failed messages
* Retry mechanism (max 3 retries)
* Line-by-line CSV streaming with per-line error handling

---

## Technology Stack

### Backend Core (Member 1)

* **Language/Framework:** Java, Spring Boot 3
* **Database:** PostgreSQL 15
* **Security:** Spring Security, JWT (Stateless), RBAC
* **Locking:** Pessimistic Locking (SELECT FOR UPDATE) + Redis distributed lock
* **Idempotency:** Redis SETNX for Idempotency-Key
* **Patterns:** State Pattern (Order lifecycle), Strategy Pattern (pricing), Abstract Factory (ticket types)
* **Caching:** Cache-aside with Redis

### Web Frontend (Member 2)

* **Framework:** Next.js 14 (App Router)
* **Styling:** Tailwind CSS
* **State:** Zustand
* **Data Fetching:** TanStack Query
* **Seat Map:** Inline SVG with React.memo + Zustand selectors
* **Real-time:** SSE (Server-Sent Events)
* **Anti-spam:** Disable button on click + UUID Idempotency Key

### Mobile Check-in (Member 3)

* **Framework:** React Native or Flutter (not yet finalized)
* **Offline Storage:** SQLite
* **Cryptography:** RSA public key for offline QR verification
* **Sync:** Background worker for scan log sync

### Infrastructure & Async (Member 4 — My Scope)

* **Docker:** docker-compose with alpine images (PostgreSQL, Redis, RabbitMQ)
* **Rate Limiting:** Bucket4j + Redis (Token Bucket algorithm)
* **Circuit Breaker:** Resilience4j (count-based sliding window)
* **Bulkhead:** Thread limit for payment gateway calls
* **Message Queue:** RabbitMQ with DLQ and retry (max 3)
* **AI:** Gemini API (cloud)
* **Email:** Resend API (cloud)

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Architecture Style | Monolithic (Spring Boot) with Docker containers | Simplicity for academic project, single team |
| Database | PostgreSQL | ACID compliance, complex transactions, data integrity |
| Cache | Redis (Cache-aside) | High-speed reads, distributed locking, rate limiting |
| Message Broker | RabbitMQ | Reliable async processing, DLQ support |
| Locking Strategy | Pessimistic (SELECT FOR UPDATE) | Zero overselling guarantee under extreme contention |
| Auth | JWT Stateless | Scalable, no server-side session |
| Rate Limiting | Token Bucket (Bucket4j + Redis) | Distributed, fair throttling |
| Payment Protection | Resilience4j Circuit Breaker + Bulkhead | Failure isolation, graceful degradation |
| Idempotency | Redis SETNX | Prevent double-charging |
| Real-time Updates | SSE | Lighter than WebSocket, server push |
| AI Integration | Gemini (Cloud API) | No self-hosting required |
| Email Service | Resend (Cloud API) | Quick setup, no infrastructure needed |

---

## Security Requirements

* JWT stateless authentication
* RBAC with 3 roles (CUSTOMER, ORGANIZER, CHECKER)
* Spring Security filter-based API protection
* Rate limiting per IP (public) and per User ID (private)
* Idempotency Key for payment endpoints
* RSA asymmetric cryptography for e-ticket QR verification
* SOC-mindset logging for all critical actions (auth failures, payment timeouts, rate limit hits)

---

## Reliability Requirements

* System must NOT crash if third-party services fail
* Graceful degradation: non-payment features remain available during payment gateway outages
* All RabbitMQ queues must have DLQ
* CSV import must not crash on malformed lines
* Mobile check-in must work offline
* Health checks on all Docker services
* Explicit depends_on with healthcheck in docker-compose

---

## AI Features

* PDF text extraction
* Text cleaning and preprocessing
* AI model call (Gemini/OpenAI) for artist bio generation
* Async processing via RabbitMQ worker
* Request throttling to avoid API rate limits

---

## Async Processing Features

* **CSV Import Worker:** Streaming line-by-line, fault-tolerant, idempotent, duplicate detection
* **AI Worker:** PDF → text → AI model → store result
* **Email Worker:** Order confirmation, e-ticket delivery, event reminders
* All workers consume from RabbitMQ queues
* Retry mechanism (max 3 retries) → DLQ on failure

---

## External Integrations

| Service | Type | Purpose |
|---------|------|---------|
| VNPAY | Payment Gateway | Ticket payment processing |
| MoMo | Payment Gateway | Alternative payment processing |
| Gemini | AI API (Cloud) | Artist bio generation from PDF |
| OpenAI | AI API (Cloud) | Fallback AI provider |
| Resend | Email API (Cloud) | Transactional emails |
| Brevo | Email API (Cloud) | Alternative email provider |

---

## Constraints

* **Infrastructure:** Local development on lightweight Linux Mint XFCE — minimal Docker images required (alpine)
* **Academic Project:** Full implementation required, not just stubs or mocks
* **Seed Data:** Must include sample concerts (Anh Trai Say Hi, Anh Trai Vượt Ngàn Chông Gai, Em Xinh Say Hi, Chị Đẹp Đạp Gió Rẽ Sóng) with ticket types, prices, seat maps
* **Documentation:** Blueprint (design docs) + full working implementation required
* **Team:** 4 members with strict domain ownership

---

## Risks

| Risk | Severity | Mitigation |
|------|----------|------------|
| Member 3 (Mobile) has not finalized technology | Medium | May delay check-in integration |
| Payment gateway integration may be simulated | Low | Focus on protection mechanisms (CB/Bulkhead) |
| Tight coupling between backend business logic and infrastructure | Medium | Clear interfaces and contracts between Member 1 and Member 4 |
| Docker compose healthcheck for PostgreSQL has placeholder values | High | Fix healthcheck to use actual env variables |
| No Spring Boot project initialized yet | High | Blocks all Java-based implementation |

---

## Open Questions

1. Has Member 1 initialized the Spring Boot project structure?
2. Has Member 3 decided between React Native and Flutter?
3. Are VNPAY/MoMo integrations real or simulated for this academic project?
4. What is the exact schema design for orders, tickets, and concerts?
5. Is the init.sql file meant to be the final schema or just a test placeholder?
