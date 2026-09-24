# Quickstart: Support Ticket Core

Validation guide for implementers. Commands assume repository root. Do not treat this file as the implementation.

## Prerequisites

- JDK 21
- Node.js 20+ (for the React app)
- Docker optional (PostgreSQL for `prod`-like runs)
- Environment variables for the backend (never commit values; Spring Boot does not load `.env` automatically — export them in the shell)

### Authentication and local credentials

User registration is out of scope. Configure one operator via environment variables. All authenticated operators have equal permissions (no roles). Ticket APIs require a JWT from `POST /api/auth/login`.

| Variable | How to set |
| --- | --- |
| `JWT_SECRET` | Random string of at least 32 UTF-8 bytes. Keep it only as an environment variable; do not write the actual secret into this file or git. |
| `OPERATOR_USERNAME` | Plaintext username. For the local demo, use `admin`. |
| `OPERATOR_PASSWORD` | BCrypt hash of the operator password, not plaintext. For the local demo, hash `admin123`. |
| `FRONTEND_ORIGIN` | Optional single CORS origin (not `*`). Default `http://localhost:5173`. |

Placeholder names: [backend/.env.example](../../backend/.env.example). Example exports (substitute your own secret and hash):

```bash
export JWT_SECRET='<generate-a-random-string-of-at-least-32-characters>'
export OPERATOR_USERNAME=admin
export OPERATOR_PASSWORD='<bcrypt-hash-of-admin123>'
```

**Demo UI login:** username `admin`, password `admin123` (local/demo only; requires the matching username and BCrypt hash in the environment).

## Layout

- Backend: `backend/` (Spring Boot 3.x, Flyway, profiles `local` | `test` | `prod`)
- Frontend: `frontend/` (Vite + React)
- Contract: [contracts/openapi.yaml](./contracts/openapi.yaml)
- Model: [data-model.md](./data-model.md)

## Local run

1. Start backend with `local` profile (file H2). Confirm Flyway applied.
2. Start frontend; `/api` proxied to backend.
3. Sign in as the configured operator (local demo: `admin` / `admin123`).
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
