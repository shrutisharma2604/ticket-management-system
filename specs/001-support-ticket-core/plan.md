# Implementation Plan: Support Ticket Core

**Branch**: `001-support-ticket-core` | **Date**: 2026-09-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-support-ticket-core/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

Operators sign in, then create, list, view, update, comment on, search, and filter support tickets. Status follows a strict server-side machine (Open → In Progress → Resolved → Closed, plus cancel from Open or In Progress). Data survives restart. The UI shows server validation and illegal-transition errors.

Approach: Java 21 / Spring Boot 3.x layered REST API (controller → service → repository), DTO responses only, Flyway schema, PostgreSQL in production and H2 locally/tests, JWT Bearer auth with equal-permission operators, React (Vite) client. Status transitions live in a dedicated domain component invoked only by the service. See [research.md](./research.md).

## Technical Context

**Language/Version**: Java 21; React on Node 20+ (TypeScript preferred)

**Primary Dependencies**: Spring Boot 3.x (Web, Data JPA, Validation, Security), Flyway, JWT (Nimbus / Spring OAuth2 JOSE), Bean Validation; Vite, React, React Router

**Storage**: PostgreSQL (`prod`); file H2 (`local`); in-memory H2 (`test` except explicit durability tests)

**Testing**: JUnit 5, Spring Boot Test, MockMvc contract tests, React Testing Library

**Target Platform**: Linux server (API) + modern browser (SPA)

**Project Type**: Web application (REST backend + React frontend)

**Performance Goals**: Operator list/search among ≥50 tickets in under 30 seconds (SC-005); create+view under 2 minutes (SC-001)

**Constraints**: No entity leakage in JSON; no hardcoded secrets; CORS not permissive; pagination max page size 100; status machine only on the server; logs must not dump ticket bodies, passwords, or tokens

**Scale/Scope**: Single-product MVP: login, ticket CRUD-ish fields, comments, search/filter, status machine; one operator permission set; no attachments, SLA, email, or role matrix

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Pre-research | Post-design |
|------|----------------|-------------|
| I. Spec-first | Plan traces FR-001–FR-018 and SC-001–SC-006 | OpenAPI and data-model only encode spec behavior; no extra ticket types |
| II. Secure by default | Auth required; validation on server; secrets via env | JWT on ticket routes; 401 without token; Problem Details without leaking secrets; CORS limited to frontend origin |
| III. Test-first | Status, authz, and persistence tests required before behavior lands | Unit machine + contract + integration + UI error tests named in research/quickstart |
| IV. Contracts | Public HTTP contract required | [contracts/openapi.yaml](./contracts/openapi.yaml); integration covers lifecycle and 401 |
| V. Simplicity | No workflow engine; no role split | Layered Spring as requested; no extra services |
| Java / persistence | Constructor injection, Optional, no `@Data` on entities, paginated lists | Entities and page API documented in data-model |
| Git branch | Implement on `cursor/001-support-ticket-core` (constitution), not `main` | Unchanged |

**Gate result**: PASS. No unjustified violations. Repository layer is the requested Spring pattern, not a constitution exception.

## Project Structure

### Documentation (this feature)

```text
specs/001-support-ticket-core/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md             # Phase 2 output (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── src/main/java/com/tickets/
│   ├── TicketApplication.java
│   ├── api/                 # controllers, DTO request/response, exception handlers
│   ├── security/            # JWT filter, login, SecurityFilterChain
│   ├── domain/              # TicketStatus, TicketPriority, TicketStatusMachine
│   ├── service/
│   └── persistence/         # JPA entities, repositories
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-prod.yml
│   └── db/migration/        # Flyway
└── src/test/java/com/tickets/
    ├── domain/              # status machine unit tests
    ├── api/                 # contract / MockMvc
    └── integration/         # persistence, authz, lifecycle

frontend/
├── package.json
├── src/
│   ├── api/                 # fetch helpers, Problem Details mapping
│   ├── auth/
│   ├── pages/               # Login, TicketList, TicketDetail
│   └── components/
└── src/__tests__/           # RTL: field errors, illegal transition
```

**Structure Decision**: Split `backend/` and `frontend/` at repository root. Matches a Spring API plus Vite SPA. Local Vite proxy `/api` to the backend; production serves API and static UI on configured origins with explicit CORS.

## Complexity Tracking

No constitution violations requiring justification.

## Phase 0 / Phase 1 outputs

- [research.md](./research.md) — JWT, H2/Postgres profiles, machine placement, Problem Details
- [data-model.md](./data-model.md) — Ticket, Comment, transitions
- [contracts/openapi.yaml](./contracts/openapi.yaml) — login, tickets, status, comments
- [quickstart.md](./quickstart.md) — run and verify against spec
