# Quickstart: Support Ticket Core

Validation guide for implementers. Commands assume repository root. Do not treat this file as the implementation.

## Prerequisites

- JDK 21
- Node.js 20+ (for the React app)
- Docker optional (PostgreSQL for `prod`-like runs)
- Environment: `JWT_SECRET` (256-bit), `OPERATOR_USERNAME`, `OPERATOR_PASSWORD` — never commit values

## Layout

- Backend: `backend/` (Spring Boot 3.x, Flyway, profiles `local` | `test` | `prod`)
- Frontend: `frontend/` (Vite + React)
- Contract: [contracts/openapi.yaml](./contracts/openapi.yaml)
- Model: [data-model.md](./data-model.md)

## Local run

1. Start backend with `local` profile (file H2). Confirm Flyway applied.
2. Start frontend; `/api` proxied to backend.
3. Sign in as the configured operator.
4. Create a ticket; open it; restart backend; confirm the ticket remains.
5. Walk Open → In Progress → Resolved → Closed on one ticket.
6. On a Closed ticket, request Open via HTTP (not only UI); expect 422 and unchanged status.
7. Submit blank title; expect 400 with field errors shown in the UI.
8. Call list/create without `Authorization`; expect 401.

## Automated checks (minimum)

Run from `backend/`: unit tests for the status machine (allowed set + Closed/Resolved/Cancelled → Open); contract tests for OpenAPI statuses and `application/problem+json`; integration tests for persistence and 401. From `frontend/`: tests that render field errors and illegal-transition `detail`.

## Expected outcomes mapped to spec

| Check | Spec |
|-------|------|
| Create + list + detail after restart | FR-001–FR-005, SC-001, SC-002 |
| PATCH does not change status | FR-006 |
| POST status allowed/forbidden | FR-010–FR-012, SC-003 |
| Comments on closed ticket | FR-007 |
| `q` and `status` query params | FR-008, FR-009, SC-005 |
| Problem Details in UI | FR-013, FR-014, SC-004 |
| No token → 401 | FR-015, SC-006 |
