---
description: "Task list for Support Ticket Core implementation"
---

# Tasks: Support Ticket Core

**Input**: Design documents from `/specs/001-support-ticket-core/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Included. Constitution Principle III and plan.md require TDD: write tests, confirm they fail, then implement.

**Organization**: Phases follow spec user stories (US1–US7). Implement on branch `cursor/001-support-ticket-core`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete work)
- **[Story]**: User story label (US1–US7) on story-phase tasks only
- Exact file paths in every task

## Path Conventions

- Backend: `backend/src/main/java/com/tickets/`, tests under `backend/src/test/java/com/tickets/`
- Frontend: `frontend/src/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Empty Spring + React workspaces with no secrets in git

- [ ] T001 Create directory layout from plan.md: `backend/src/main/java/com/tickets/{api,security,domain,service,persistence}/`, `backend/src/main/resources/db/migration/`, `backend/src/test/java/com/tickets/{domain,api,integration}/`, `frontend/src/{api,auth,pages,components,__tests__}/`
- [ ] T002 Initialize Java 21 Spring Boot 3.x Maven app in `backend/pom.xml` with Web, Data JPA, Validation, Security, Flyway, H2, PostgreSQL driver, OAuth2 JOSE/JWT; main class `backend/src/main/java/com/tickets/TicketApplication.java`
- [ ] T003 [P] Initialize Vite + React (TypeScript) in `frontend/package.json` and `frontend/vite.config.ts`
- [ ] T004 [P] Ignore secrets and build output in `.gitignore` (`backend/.env`, `frontend/.env`, `backend/data/`, `**/target/`, `frontend/node_modules/`); add `backend/.env.example` with placeholder keys only (`JWT_SECRET`, `OPERATOR_USERNAME`, `OPERATOR_PASSWORD`) and no real values
- [ ] T005 [P] Configure 4-space indent and 120-character line length in `backend/pom.xml` (Spotless or Checkstyle)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Profiles, schema, JWT, Problem Details, entities — required before any story

**⚠️ CRITICAL**: No user story work until this phase is complete

- [ ] T006 Configure `local` (file H2, restart-durable), `test` (in-memory H2), and `prod` (PostgreSQL) in `backend/src/main/resources/application.yml`, `application-local.yml`, `application-prod.yml`, `application-test.yml`; bind `JWT_SECRET`, `OPERATOR_USERNAME`, `OPERATOR_PASSWORD` from environment only
- [ ] T007 Add Flyway `backend/src/main/resources/db/migration/V1__tickets.sql` with ticket columns: `id` UUID PK, `title` VARCHAR(200) NOT NULL, `description` VARCHAR(10000) or TEXT NOT NULL, `priority` VARCHAR NOT NULL, `assignee` VARCHAR(100) NULL, `status` VARCHAR NOT NULL, `created_at` timestamptz NOT NULL, `updated_at` timestamptz NOT NULL; indexes on `status` and `created_at DESC`
- [ ] T008 Add Flyway `backend/src/main/resources/db/migration/V2__comments.sql` with `id` UUID PK, `ticket_id` UUID NOT NULL FK to tickets, `body` VARCHAR(4000) NOT NULL, `created_at` timestamptz NOT NULL; index `(ticket_id, created_at)`
- [ ] T009 [P] Add enums `OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED` and `LOW, MEDIUM, HIGH` in `backend/src/main/java/com/tickets/domain/TicketStatus.java` and `TicketPriority.java`
- [ ] T010 Map JPA `TicketEntity` in `backend/src/main/java/com/tickets/persistence/TicketEntity.java` (no Lombok `@Data`; equals/hashCode on `id` only after persist; fields match V1)
- [ ] T011 [P] Map JPA `CommentEntity` in `backend/src/main/java/com/tickets/persistence/CommentEntity.java` (no `@Data`; FK to ticket; body + createdAt)
- [ ] T012 Add `backend/src/main/java/com/tickets/persistence/TicketRepository.java` and `CommentRepository.java` (Spring Data JPA)
- [ ] T013 Implement RFC 7807 `application/problem+json` in `backend/src/main/java/com/tickets/api/ProblemExceptionHandler.java` for HTTP 400 (`errors` field map), 401, 404, 422 (detail names current and requested status); parameterized SLF4J only — never log passwords, tokens, or full ticket bodies
- [ ] T014 Implement JWT login `POST /api/auth/login` and Bearer filter so `/api/tickets/**` requires authentication in `backend/src/main/java/com/tickets/security/JwtService.java`, `AuthController.java`, `JwtAuthenticationFilter.java`, `SecurityConfig.java`; unauthenticated ticket calls return 401; all operators share the same permissions (no roles)
- [ ] T015 Restrict CORS to the configured frontend origin (not `*`) in `backend/src/main/java/com/tickets/security/SecurityConfig.java`
- [ ] T016 Proxy `/api` to the backend in `frontend/vite.config.ts`
- [ ] T017 Implement login client and in-memory plus `sessionStorage` token (not `localStorage`) in `frontend/src/auth/AuthContext.tsx` and `frontend/src/api/client.ts`
- [ ] T018 Add login UI in `frontend/src/pages/LoginPage.tsx` and route gate in `frontend/src/App.tsx`
- [ ] T019 Write failing contract tests for login 200 and ticket routes 401 without token in `backend/src/test/java/com/tickets/api/AuthContractTest.java`

**Checkpoint**: Foundation ready — user stories may start

---

## Phase 3: User Story 1 - Create a support ticket (Priority: P1) 🎯 MVP

**Goal**: Signed-in operator creates a ticket with title, description, priority, optional assignee; stored as `OPEN`; survives restart

**Independent Test**: Create a valid ticket, restart backend (`local` file H2), confirm it remains with same fields and Open status; blank title/description refused with field errors

### Tests for User Story 1

> Write these FIRST and ensure they FAIL before implementation

- [ ] T020 [P] [US1] Write failing contract tests for `POST /api/tickets` in `backend/src/test/java/com/tickets/api/CreateTicketContractTest.java`: 201 with status `OPEN`; 400 when title or description is blank/whitespace-only; 400 unknown priority; 401 without token
- [ ] T021 [P] [US1] Write failing integration test that creates a ticket then reloads from a file-based store in `backend/src/test/java/com/tickets/integration/TicketPersistenceIT.java`

### Implementation for User Story 1

- [ ] T022 [US1] Add `CreateTicketRequest` in `backend/src/main/java/com/tickets/api/CreateTicketRequest.java`: title required trim length 1–200; description required trim length 1–10,000; priority required `LOW|MEDIUM|HIGH`; assignee optional, if present trim length 1–100, blank/whitespace treated as unassigned
- [ ] T023 [US1] Add `TicketResponse` DTO (never return `TicketEntity`) in `backend/src/main/java/com/tickets/api/TicketResponse.java`
- [ ] T024 [US1] Implement `create` in `backend/src/main/java/com/tickets/service/TicketService.java`: new tickets MUST have status `OPEN`; set `createdAt`/`updatedAt`; constructor injection, `final` collaborators
- [ ] T025 [US1] Implement `POST /api/tickets` in `backend/src/main/java/com/tickets/api/TicketController.java` returning 201 and DTO
- [ ] T026 [US1] Add create-ticket form in `frontend/src/pages/TicketCreatePage.tsx` posting via `frontend/src/api/tickets.ts`
- [ ] T027 [US1] Confirm T020–T021 pass; trim whitespace-only fields as blank per spec edge cases

**Checkpoint**: US1 independently testable

---

## Phase 4: User Story 2 - Browse and open tickets (Priority: P1)

**Goal**: Paginated list (title, priority, assignee, status) and detail (plus timestamps); 404 for missing id

**Independent Test**: Two tickets appear in the list; opening one shows title, description, priority, assignee, status, created and last-changed times; unknown id shows not-found

### Tests for User Story 2

- [ ] T028 [P] [US2] Write failing contract tests in `backend/src/test/java/com/tickets/api/GetTicketsContractTest.java` for `GET /api/tickets` (page metadata: `content`, `page`, `size`, `totalElements`, `totalPages`; default size 20 max 100) and `GET /api/tickets/{ticketId}` 200 vs 404
- [ ] T029 [P] [US2] Write failing UI test for not-found copy in `frontend/src/__tests__/TicketDetailNotFound.test.tsx`

### Implementation for User Story 2

- [ ] T030 [US2] Add `TicketSummary` and `TicketPage` DTOs in `backend/src/main/java/com/tickets/api/TicketPageResponse.java`; list ordered by `createdAt` descending
- [ ] T031 [US2] Implement paginated `list` and `getById` in `backend/src/main/java/com/tickets/service/TicketService.java` using Spring Data pages (`page` 0-based, `size` default 20 maximum 100)
- [ ] T032 [US2] Implement `GET /api/tickets` and `GET /api/tickets/{ticketId}` in `backend/src/main/java/com/tickets/api/TicketController.java`; missing ticket → 404 Problem Detail
- [ ] T033 [US2] Add list and detail pages in `frontend/src/pages/TicketListPage.tsx` and `frontend/src/pages/TicketDetailPage.tsx` with pagination controls
- [ ] T034 [US2] Load comments as empty array on detail until US5 (`TicketDetailResponse` in `backend/src/main/java/com/tickets/api/TicketDetailResponse.java`)

**Checkpoint**: US1 and US2 work independently

---

## Phase 5: User Story 3 - Enforce ticket status workflow (Priority: P1)

**Goal**: Server-only machine: Open→In Progress→Resolved→Closed; Open|In Progress→Cancelled; same-status no-op succeeds; all other moves 422 without changing stored status

**Independent Test**: Drive each allowed transition; `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN` via HTTP (not UI) return 422 and unchanged status

### Tests for User Story 3

- [ ] T035 [P] [US3] Write failing unit tests in `backend/src/test/java/com/tickets/domain/TicketStatusMachineTest.java` covering allowed pairs, same-status no-op, and forbidden set including `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`, `OPEN→RESOLVED`, `OPEN→CLOSED`, `IN_PROGRESS→OPEN`, `IN_PROGRESS→CLOSED`, `RESOLVED→IN_PROGRESS`, `RESOLVED→CANCELLED`
- [ ] T036 [P] [US3] Write failing contract tests for `POST /api/tickets/{ticketId}/status` in `backend/src/test/java/com/tickets/api/StatusTransitionContractTest.java` (200 allowed, 422 illegal with detail naming current and requested status, 400 unknown status token, 404 missing ticket)

### Implementation for User Story 3

- [ ] T037 [US3] Implement `TicketStatusMachine` in `backend/src/main/java/com/tickets/domain/TicketStatusMachine.java` consulted only from the service; allowed table per data-model.md; `from == to` allowed
- [ ] T038 [US3] Add `StatusTransitionRequest` in `backend/src/main/java/com/tickets/api/StatusTransitionRequest.java` (required `status` enum)
- [ ] T039 [US3] Implement `transition` in `backend/src/main/java/com/tickets/service/TicketService.java` against **persisted** status at write time; illegal → 422 no update; success updates `updatedAt` except same-status no-op MAY skip write
- [ ] T040 [US3] Implement `POST /api/tickets/{ticketId}/status` in `backend/src/main/java/com/tickets/api/TicketController.java`
- [ ] T041 [US3] Add status actions on `frontend/src/pages/TicketDetailPage.tsx` (UI may hide illegal targets; server remains source of truth)

**Checkpoint**: Status policy holds even if the UI is bypassed

---

## Phase 6: User Story 4 - Update ticket details (Priority: P2)

**Goal**: PATCH title, description, priority, assignee; must not change status; assignee may be cleared

**Independent Test**: Change each field, restart, values persist; sending status on PATCH ignored or 400; invalid fields 400 and stored values unchanged

### Tests for User Story 4

- [ ] T042 [P] [US4] Write failing contract tests in `backend/src/test/java/com/tickets/api/UpdateTicketContractTest.java`: PATCH updates fields and keeps status; `status` in body does not change status; 400 invalid title/description/priority; 404 missing id; null assignee clears assignee

### Implementation for User Story 4

- [ ] T043 [US4] Add `UpdateTicketRequest` in `backend/src/main/java/com/tickets/api/UpdateTicketRequest.java` with optional title (1–200 after trim), description (1–10,000), priority enum, assignee (1–100 or null to unassign); **must not include status**
- [ ] T044 [US4] Implement `updateDetails` in `backend/src/main/java/com/tickets/service/TicketService.java` (last accepted write wins; never apply status from this method)
- [ ] T045 [US4] Implement `PATCH /api/tickets/{ticketId}` in `backend/src/main/java/com/tickets/api/TicketController.java`
- [ ] T046 [US4] Add edit form on `frontend/src/pages/TicketDetailPage.tsx` or `frontend/src/pages/TicketEditPage.tsx`

**Checkpoint**: Field edits cannot smuggle status changes

---

## Phase 7: User Story 5 - Comment on a ticket (Priority: P2)

**Goal**: Non-empty comments with created time, oldest-first; allowed on Closed/Cancelled; comments do not change status

**Independent Test**: Two comments on one ticket appear oldest-first after restart; blank comment refused; comment on Closed does not change status

### Tests for User Story 5

- [ ] T047 [P] [US5] Write failing contract tests in `backend/src/test/java/com/tickets/api/CommentContractTest.java`: `POST /api/tickets/{ticketId}/comments` 201; 400 blank/whitespace body; 404 missing ticket; comment on `CLOSED`/`CANCELLED` succeeds and ticket status unchanged; detail lists comments oldest-first then `id`

### Implementation for User Story 5

- [ ] T048 [US5] Add `AddCommentRequest` and `CommentResponse` in `backend/src/main/java/com/tickets/api/AddCommentRequest.java` and `CommentResponse.java`; body required trim length 1–4,000
- [ ] T049 [US5] Implement `addComment` and include comments on get-by-id in `backend/src/main/java/com/tickets/service/TicketService.java` (ticket `updatedAt` MAY stay unchanged)
- [ ] T050 [US5] Implement `POST /api/tickets/{ticketId}/comments` in `backend/src/main/java/com/tickets/api/TicketController.java`
- [ ] T051 [US5] Render and submit comments on `frontend/src/pages/TicketDetailPage.tsx` and `frontend/src/components/CommentList.tsx`

**Checkpoint**: Conversation lives on the ticket record

---

## Phase 8: User Story 6 - Search and filter the queue (Priority: P3)

**Goal**: Case-insensitive keyword on title/description; optional single status filter; AND together; empty match is empty list not an error

**Independent Test**: Distinct tickets; unique word search; one status filter; both combined; no matches show “no matching tickets”

### Tests for User Story 6

- [ ] T052 [P] [US6] Write failing contract tests in `backend/src/test/java/com/tickets/api/SearchFilterContractTest.java`: `q` matches title or description case-insensitively; blank `q` means no keyword predicate; `status` filters exactly one status; `q` AND `status`; zero matches return 200 empty `content`

### Implementation for User Story 6

- [ ] T053 [US6] Add list query methods in `backend/src/main/java/com/tickets/persistence/TicketRepository.java` (no N+1; paginated)
- [ ] T054 [US6] Pass `q` and `status` from `GET /api/tickets` in `backend/src/main/java/com/tickets/api/TicketController.java` and `TicketService.java`
- [ ] T055 [US6] Add search and status filter plus empty-state copy on `frontend/src/pages/TicketListPage.tsx`

**Checkpoint**: Operators can find a known ticket among mixed records

---

## Phase 9: User Story 7 - Understand failures in the interface (Priority: P2)

**Goal**: UI shows the same meaning the server decided (field errors, illegal transition, not found); never success on refused actions

**Independent Test**: Invalid create/update, illegal status, missing ticket — specific readable messages; after illegal transition, refresh matches stored status

### Tests for User Story 7

- [ ] T056 [P] [US7] Write failing RTL tests in `frontend/src/__tests__/ProblemDetailsDisplay.test.tsx` for field-level 400 `errors`, 422 `detail` on illegal transition, and 404 not-found (no false success toast)

### Implementation for User Story 7

- [ ] T057 [US7] Map `application/problem+json` to field errors and banners in `frontend/src/api/problem.ts` and `frontend/src/components/ErrorBanner.tsx`
- [ ] T058 [US7] Wire mapping into create, edit, status, and comment forms so refused submits do not claim save succeeded
- [ ] T059 [US7] Show not-found explanation on `frontend/src/pages/TicketDetailPage.tsx` when GET returns 404 (not a blank page)

**Checkpoint**: Operators see server reasons, including when the UI is bypassed for status

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Quickstart, docs, security review

- [ ] T060 [P] Document local run, env vars, and profiles in `README.md` using `specs/001-support-ticket-core/quickstart.md` (no secrets)
- [ ] T061 Execute quickstart.md validation (create, restart, allowed/forbidden status, 401, field errors in UI)
- [ ] T062 Confirm logs never contain JWT, passwords, or full ticket payloads (spot-check `TicketService.java` and `AuthController.java`)
- [ ] T063 [P] Add frontend lint/test scripts in `frontend/package.json` and ensure `backend` tests cover SC-003 transition set and SC-006 unauthenticated refusal

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1–US7 (Phases 3–9)**: Depend on Foundational; sequential by priority is safest because later stories assume tickets exist
- **Polish (Phase 10)**: After stories you intend to ship

### User Story Dependencies

- **US1 (P1)**: After Phase 2 — MVP
- **US2 (P1)**: After Phase 2; practically needs US1 (or seed data) for a non-empty list
- **US3 (P1)**: After tickets exist (US1); machine is independent domain code (T035/T037 can start once T009 exists)
- **US4 (P2)**: After US1 (and typically US2 for UI)
- **US5 (P2)**: After US2 detail payload
- **US6 (P3)**: After US2 list endpoint
- **US7 (P2)**: After at least US1 errors exist; full value after US3 422 and US2 404

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- DTOs/entities before services before controllers before UI

### Parallel Opportunities

- T003, T004, T005 after T001
- T009 and T011 after T007/T008 schema
- T020 and T021; T028 and T029; T035 and T036; T047; T052; T056
- After Phase 2, T035 `TicketStatusMachineTest` can proceed in parallel with US1 if T009 is done
- Frontend US7 mapping (T057) can start once Problem Details shape is stable (T013)

---

## Parallel Example: User Story 1

```bash
# After Phase 2, launch US1 tests together:
Task: "CreateTicketContractTest.java"
Task: "TicketPersistenceIT.java"

# After tests fail, implement request DTO then service then controller then UI (sequential on overlapping files)
```

## Parallel Example: User Story 3

```bash
Task: "TicketStatusMachineTest.java"
Task: "StatusTransitionContractTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 Setup
2. Phase 2 Foundational
3. Phase 3 US1
4. **STOP and VALIDATE**: create, validation errors, restart durability, 401
5. Demo create-ticket only

### Incremental Delivery

1. Setup + Foundational
2. US1 create → demo
3. US2 list/detail → usable queue
4. US3 status machine → policy complete
5. US4 updates → US5 comments → US7 error UX → US6 search/filter
6. Polish + quickstart.md

### Parallel Team Strategy

1. Together: Phases 1–2
2. Then: Developer A US1+US2; Developer B T035/T037 machine then US3; Developer C frontend shell (login/list) against mocked API until contracts exist
3. Integrate on `cursor/001-support-ticket-core`

---

## Notes

- Quote constraints in this file are binding: title 1–200, description 1–10,000, comment 1–4,000, assignee 1–100 or null, page size max 100
- Do not return JPA entities from controllers
- Do not implement attachments, SLA, email, or role split
- Verify tests fail before implementing each story
