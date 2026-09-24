# ticket-management-system

Support ticket management for equal-permission operators: create, list, search, update, comment, and strictly enforced status transitions.

Feature spec and contracts: [specs/001-support-ticket-core/spec.md](specs/001-support-ticket-core/spec.md), [quickstart.md](specs/001-support-ticket-core/quickstart.md), [contracts/openapi.yaml](specs/001-support-ticket-core/contracts/openapi.yaml).

## Tech stack

- Java 21, Spring Boot 3.x, Flyway
- PostgreSQL (`prod` profile) and H2 (`local` file, `test` in-memory)
- REST API with JWT Bearer auth and RFC 7807 Problem Details
- React (Vite + TypeScript)

## Authentication

There is **no user registration**. A single operator is configured from the environment. All authenticated operators have **equal permissions** (no roles). Ticket APIs require `Authorization: Bearer <token>` from `POST /api/auth/login`.

### Environment variables

Set these in the process that starts the backend. Do not commit real values. Copy [backend/.env.example](backend/.env.example) as a reminder of the names only; Spring Boot does not load `.env` automatically, so export the variables (or inject them from your shell/CI).

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | HMAC signing key for JWTs. Must be at least 32 UTF-8 bytes. Generate your own local value; **never put the secret in git or this README**. |
| `OPERATOR_USERNAME` | Login username for the seeded operator. |
| `OPERATOR_PASSWORD` | **BCrypt hash** of the operator password (not plaintext). |
| `FRONTEND_ORIGIN` | Optional. Single allowed CORS origin (not `*`). Default: `http://localhost:5173`. |

Local example (replace the JWT secret and password hash with values you generate):

```bash
export JWT_SECRET='<generate-a-random-string-of-at-least-32-characters>'
export OPERATOR_USERNAME=admin
export OPERATOR_PASSWORD='<bcrypt-hash-of-the-demo-password>'
```

Generate a BCrypt hash of the password you will type at login (for the demo, that password is `admin123`). Keep the hash only in your local environment.

### Demo login (local UI)

Username: `admin`  
Password: `admin123`

These are demo credentials for local use when `OPERATOR_USERNAME` is `admin` and `OPERATOR_PASSWORD` is a BCrypt hash of `admin123`. Do not use them in production.

## Run backend

From `backend/`, with the environment variables above set:

```bash
mvn spring-boot:run
```

The default Spring profile is `local` (file H2 under `backend/data/`, Flyway migrations, data survives restart). API listens on port 8080.

Production-like PostgreSQL: `prod` profile plus `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.

## Run frontend

From `frontend/` (Node.js 20+):

```bash
npm install
npm run dev
```

Vite serves the UI (typically `http://localhost:5173`) and proxies `/api` to `http://localhost:8080`. Sign in with the demo operator, then use the ticket dashboard.

## Run tests

Backend (from `backend/`):

```bash
mvn test
```

Frontend (from `frontend/`):

```bash
npm test
```

Manual validation steps: [specs/001-support-ticket-core/quickstart.md](specs/001-support-ticket-core/quickstart.md).
