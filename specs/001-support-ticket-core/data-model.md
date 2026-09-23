# Data Model: Support Ticket Core

## Ticket

Represents a support work item.

| Field | Type | Rules |
|-------|------|--------|
| id | UUID | Generated on create; immutable |
| title | string | Required; trim; length 1–200 |
| description | string | Required; trim; length 1–10_000 |
| priority | enum | `LOW`, `MEDIUM`, `HIGH` |
| assignee | string, optional | Null = unassigned; if present, trim, length 1–100; blank/whitespace treated as unassigned |
| status | enum | See state machine; create always `OPEN` |
| createdAt | instant | Set on insert; immutable |
| updatedAt | instant | Set on insert and on every successful field or status change |

Relationships: one ticket has many comments (cascade persist; delete ticket deletes comments).

### Status enum

`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`

Stored as strings. API JSON uses these exact names.

### Status state machine

Initial: `OPEN`.

Allowed:

| From | To |
|------|-----|
| OPEN | IN_PROGRESS |
| IN_PROGRESS | RESOLVED |
| RESOLVED | CLOSED |
| OPEN | CANCELLED |
| IN_PROGRESS | CANCELLED |

Idempotent: `from == to` is allowed; no `updatedAt` change required for status-only no-op (implementation MAY skip write).

Forbidden: every other pair, including `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`, `OPEN→RESOLVED`, `OPEN→CLOSED`, `IN_PROGRESS→OPEN`, `IN_PROGRESS→CLOSED`, `RESOLVED→IN_PROGRESS`, `RESOLVED→CANCELLED`, `CLOSED→*`, `CANCELLED→*` (except same-status no-op).

Enforcement: service layer only, against the persisted status at write time. PATCH details MUST NOT change status even if a client sends `status`.

## Comment

A note on exactly one ticket.

| Field | Type | Rules |
|-------|------|--------|
| id | UUID | Generated on create |
| ticketId | UUID | Required FK to ticket |
| body | string | Required; trim; length 1–4_000 |
| createdAt | instant | Set on insert; immutable |

Comments are allowed in any ticket status, including `CLOSED` and `CANCELLED`. Adding a comment MUST NOT change ticket status. Ticket `updatedAt` MAY be left unchanged by comments (comments have their own `createdAt`).

Ordering: oldest-first by `createdAt`, then `id`.

## Operator

Not a ticket-table entity. A signed-in principal with username. All operators have identical permissions. Seeded via configuration for v1 (no self-registration, no operator CRUD API).

## Validation summary (server)

- Whitespace-only title, description, comment body → invalid (same as blank).
- Unknown priority or status token → invalid.
- Missing ticket id on view/update/comment/transition → not found.
- Blank search `q` → no keyword predicate.
- Absent status filter → all statuses.

## Persistence notes

- Indexes: `ticket(status)`, `ticket(created_at DESC)`, `comment(ticket_id, created_at)`.
- Pagination on list queries (LIMIT/OFFSET via Spring Data pages).
- Entities MUST NOT use Lombok `@Data`. Equals/hashCode on `id` only after persist.
