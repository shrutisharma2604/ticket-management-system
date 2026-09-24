---
description: REST contract, Problem Details, auth, and DTO rules for the ticket API
globs: backend/**/*.java,**/openapi.yaml,frontend/src/api/**
alwaysApply: false
---

# API standards

Canonical contract: `specs/001-support-ticket-core/contracts/openapi.yaml`. Callers MUST NOT rely on undocumented fields. Breaking changes require a version bump.

## Auth

- `POST /api/auth/login` issues a JWT. Ticket routes require `Authorization: Bearer`.
- Equal-permission operators only. No role split. Unauthenticated ticket access MUST be 401.

## Resources

| Method | Path | Notes |
|--------|------|--------|
| POST | `/api/tickets` | Create; status always `OPEN` |
| GET | `/api/tickets` | Page + optional `q`, `status` |
| GET | `/api/tickets/{ticketId}` | Detail + comments oldest-first |
| PATCH | `/api/tickets/{ticketId}` | Title, description, priority, assignee only — never status |
| POST | `/api/tickets/{ticketId}/status` | State machine; same-status no-op 200 |
| POST | `/api/tickets/{ticketId}/comments` | Does not change status |

## Errors (`application/problem+json`)

- 400 validation: `errors` map of field → message (trim/length/enum).
- 401 missing or invalid token.
- 404 unknown ticket.
- 422 illegal transition: `detail` names current and requested status; stored status MUST NOT change.

## Validation (server)

- Title 1–200 after trim; description 1–10_000; comment 1–4_000; assignee 1–100 or null (blank → unassigned).
- Priority `LOW|MEDIUM|HIGH`. Status enum exact names. Whitespace-only = blank.
- Blank `q` = no keyword. Missing `status` filter = all statuses.

## CORS

- Allow the configured frontend origin only. NEVER `*`.
