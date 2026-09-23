# Feature Specification: Support Ticket Core

**Feature Branch**: `001-support-ticket-core`

**Created**: 2026-09-23

**Status**: Draft

**Input**: User description: "Build a Support Ticket Management System with create, list, view, update, comments, search, status filter, durable persistence, backend validation, meaningful UI errors, and a strictly enforced ticket status state machine."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a support ticket (Priority: P1)

A support operator records a new issue by providing a title, description, priority, and optional assignee. The ticket is stored immediately and starts in Open so work can begin later.

**Why this priority**: Without create-and-persist, there is no product.

**Independent Test**: Create a ticket with valid fields, restart the application, and confirm the ticket is still listed with the same details and Open status.

**Acceptance Scenarios**:

1. **Given** the operator is signed in, **When** they submit a title, description, priority, and optional assignee, **Then** a ticket is created in Open status and they can see it in the list and detail view.
2. **Given** the operator omits assignee, **When** they submit otherwise valid details, **Then** the ticket is created unassigned.
3. **Given** the operator submits a blank title or blank description, **When** they attempt to create, **Then** creation is refused and they see a clear message for each invalid field.
4. **Given** a ticket was created, **When** the application is restarted, **Then** that ticket is still available with the same field values and status.

---

### User Story 2 - Browse and open tickets (Priority: P1)

An operator scans existing tickets and opens one to read its full details.

**Why this priority**: Operators cannot work a queue they cannot see.

**Independent Test**: Seed or create at least two tickets, confirm they appear in the list, and open one to see title, description, priority, assignee, status, and timestamps.

**Acceptance Scenarios**:

1. **Given** multiple tickets exist, **When** the operator opens the ticket list, **Then** they see each ticket’s title, priority, assignee (or unassigned), and status.
2. **Given** a ticket exists, **When** the operator opens it, **Then** they see title, description, priority, assignee, status, and when it was created and last changed.
3. **Given** more tickets exist than one screen should show, **When** the operator browses the list, **Then** they can move through pages of results rather than loading an unbounded dump.
4. **Given** the operator requests a ticket that does not exist, **When** they try to view it, **Then** they see a clear not-found message.

---

### User Story 3 - Enforce ticket status workflow (Priority: P1)

An operator moves a ticket through the allowed workflow only. Illegal moves are blocked even if the operator bypasses the user interface.

**Why this priority**: Status is operational policy. A leaked illegal transition (especially reopening Closed, Resolved, or Cancelled) corrupts reporting and work.

**Independent Test**: Drive each allowed transition on a real ticket; attempt each forbidden example (including Closed→Open, Resolved→Open, Cancelled→Open) without using the UI; confirm rejection with a clear reason and unchanged status.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the operator moves it to In Progress, **Then** the status becomes In Progress.
2. **Given** a ticket is In Progress, **When** the operator moves it to Resolved, **Then** the status becomes Resolved.
3. **Given** a ticket is Resolved, **When** the operator moves it to Closed, **Then** the status becomes Closed.
4. **Given** a ticket is Open or In Progress, **When** the operator cancels it, **Then** the status becomes Cancelled.
5. **Given** a ticket is Closed, Resolved, or Cancelled, **When** anyone requests Open (or any other disallowed next status), **Then** the change is refused, the stored status does not change, and the operator sees a message that names the current status and that the requested move is not allowed.
6. **Given** a ticket is already in a given status, **When** the operator requests that same status again, **Then** the request succeeds and the ticket remains in that status.

Allowed transitions (all others MUST be refused):

- Open → In Progress
- In Progress → Resolved
- Resolved → Closed
- Open → Cancelled
- In Progress → Cancelled

---

### User Story 4 - Update ticket details (Priority: P2)

An operator corrects title, description, priority, and assignee without using that update to sneak in a status change.

**Why this priority**: Details change as facts emerge; this is daily work after create/list/status exist.

**Independent Test**: Update each editable field on an existing ticket and confirm persistence after restart; attempt a status change through the details-update action and confirm it is ignored or refused.

**Acceptance Scenarios**:

1. **Given** an existing ticket, **When** the operator changes title, description, priority, and/or assignee, **Then** the stored ticket reflects only those field changes and keeps its current status.
2. **Given** invalid title, description, or priority, **When** the operator saves, **Then** the update is refused and they see field-level messages; stored values are unchanged.
3. **Given** a details update, **When** the application restarts, **Then** the latest accepted field values are still present.

---

### User Story 5 - Comment on a ticket (Priority: P2)

An operator adds a time-stamped comment on a ticket so the conversation stays with the record.

**Why this priority**: Collaboration happens after the ticket exists; the queue still works without comments.

**Independent Test**: Add two comments to one ticket, reopen the ticket, and confirm both appear in chronological order after restart.

**Acceptance Scenarios**:

1. **Given** an existing ticket, **When** the operator submits a non-empty comment, **Then** the comment appears on that ticket with the time it was added.
2. **Given** a blank comment, **When** the operator submits, **Then** it is refused with a clear message and no comment is stored.
3. **Given** comments exist, **When** the operator views the ticket, **Then** comments appear oldest-first.
4. **Given** a ticket is Cancelled or Closed, **When** the operator adds a comment, **Then** the comment is still accepted (comments do not change status).

---

### User Story 6 - Search and filter the queue (Priority: P3)

An operator finds tickets by words in title or description and/or by status.

**Why this priority**: Valuable once a queue exists; not required to record the first ticket.

**Independent Test**: Create tickets with distinct titles, descriptions, and statuses; search by a unique word; filter by one status; combine both; confirm unmatched tickets are hidden.

**Acceptance Scenarios**:

1. **Given** tickets with different titles and descriptions, **When** the operator searches a keyword, **Then** only tickets whose title or description contains that keyword (case-insensitive) are listed.
2. **Given** tickets in mixed statuses, **When** the operator filters by one status, **Then** only tickets in that status are listed.
3. **Given** a keyword and a status filter together, **When** the operator applies both, **Then** results match both.
4. **Given** no tickets match, **When** the operator searches or filters, **Then** they see an empty list and a clear “no matching tickets” message—not a generic failure.

---

### User Story 7 - Understand failures in the interface (Priority: P2)

When the server refuses an action (validation, illegal status move, missing ticket), the interface shows the same meaning the server decided—not a silent failure or a generic “something went wrong” when a specific reason exists.

**Why this priority**: Operators must trust the queue; hidden backend rules would be unusable.

**Independent Test**: Trigger validation failure, illegal status change, and missing ticket from the UI (and, for status, also without the UI) and confirm the messages are specific and readable.

**Acceptance Scenarios**:

1. **Given** invalid create or update input, **When** the operator submits, **Then** they see which fields failed and why.
2. **Given** an illegal status change, **When** the operator (or any client) requests it, **Then** they see that the transition is not allowed; the ticket status on screen matches stored status after refresh.
3. **Given** the server is reachable but the ticket is gone, **When** the operator opens it, **Then** they see a not-found explanation rather than a blank page.

---

### Edge Cases

- Creating or updating with only whitespace in title, description, or comment MUST be treated as blank and refused.
- Unknown priority values MUST be refused.
- Unknown or missing status values on a transition request MUST be refused.
- Search with blank keyword MUST behave as “no keyword” (list/filter only), not as an error.
- Filter with no status selected MUST show tickets of all statuses (subject to search).
- Viewing, updating, commenting, or transitioning a non-existent ticket MUST return a not-found outcome.
- Concurrent edits: last accepted write wins for fields; status transitions MUST still be validated against the stored status at write time (stale illegal moves MUST be refused).
- Very long title or description beyond stated limits MUST be refused with a length message.
- Assignee may be cleared (set to unassigned) on update.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Signed-in operators MUST be able to create a ticket with title, description, priority, and optional assignee.
- **FR-002**: New tickets MUST be stored with status Open.
- **FR-003**: Ticket data and comments MUST persist across application restarts in a durable store.
- **FR-004**: Operators MUST be able to list tickets with title, priority, assignee, and status, using pagination.
- **FR-005**: Operators MUST be able to view one ticket’s title, description, priority, assignee, status, created time, last-changed time, and comments.
- **FR-006**: Operators MUST be able to update title, description, priority, and assignee on an existing ticket without changing status through that action.
- **FR-007**: Operators MUST be able to add a non-empty comment to an existing ticket; comments MUST record created time and remain attached to that ticket.
- **FR-008**: Operators MUST be able to search tickets by a case-insensitive keyword matched against title and description.
- **FR-009**: Operators MUST be able to filter the list by exactly one status, including in combination with a keyword search.
- **FR-010**: The server MUST accept only these status transitions: Open→In Progress, In Progress→Resolved, Resolved→Closed, Open→Cancelled, In Progress→Cancelled. Requesting the ticket’s current status MUST succeed with no status change.
- **FR-011**: The server MUST reject every other status transition, including Closed→Open, Resolved→Open, and Cancelled→Open, without changing stored status.
- **FR-012**: Status rules MUST be enforced on the server. Client-only checks are not sufficient; a request that bypasses the interface MUST still be refused when illegal.
- **FR-013**: The server MUST validate create, update, comment, and transition payloads (required fields, blank/whitespace, allowed priority values, allowed status values, length limits). Invalid data MUST be refused with field- or rule-specific reasons.
- **FR-014**: The interface MUST present those server reasons in readable language (field errors, illegal transition, not found). It MUST NOT report success when the server refused the action.
- **FR-015**: Unauthenticated callers MUST NOT create, list, view, update, comment on, search, filter, or transition tickets.
- **FR-016**: For this feature, every signed-in operator MAY perform all ticket operations in FR-001 through FR-011 (no separate requester vs agent permission split).
- **FR-017**: Priority MUST be one of Low, Medium, High.
- **FR-018**: Title MUST be 1–200 characters after trim; description MUST be 1–10,000 characters after trim; comment body MUST be 1–4,000 characters after trim; assignee, when present, MUST be 1–100 characters after trim.

### Key Entities

- **Ticket**: A support work item with title, description, priority (Low / Medium / High), optional assignee, status (Open, In Progress, Resolved, Closed, Cancelled), created time, and last-changed time.
- **Comment**: A note belonging to exactly one ticket, with body and created time.
- **Operator**: A signed-in person who may use all ticket operations in this feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A signed-in operator can create a valid ticket and see it in the list and detail view in under 2 minutes.
- **SC-002**: After application restart, 100% of previously accepted tickets and comments remain visible with the same field values and statuses.
- **SC-003**: 100% of forbidden status transitions attempted against the server are refused, and 100% of allowed transitions (plus same-status requests) succeed, measured in a defined transition test set that includes Closed→Open, Resolved→Open, and Cancelled→Open.
- **SC-004**: When create/update/comment validation fails, operators see a specific reason for each failed field in the interface on the first refused submit, without a page that claims the ticket was saved.
- **SC-005**: Operators can locate a known ticket among at least 50 mixed records using keyword search and/or status filter in under 30 seconds.
- **SC-006**: 100% of unauthenticated attempts to read or change tickets are refused.

## Assumptions

- Delivery is a browser UI plus a server-side application using the stack already chosen for this product (Java 21, Spring Boot 3.x, PostgreSQL or H2, HTTP JSON API, React). That stack belongs in planning; success is judged by the outcomes above.
- Authentication exists as a simple signed-in operator session (or equivalent). There is no public anonymous portal, email intake, or role matrix in this feature.
- Assignee is a free-text name or identifier, not a pick-list of registered users.
- Status change is a dedicated action, not a field on the details-update action.
- No attachments, tags, due dates, SLA clocks, email notifications, assignment inbox, or ticket types in this feature.
- “Last accepted write wins” is sufficient for concurrent field edits; no merge UI.
- H2 is acceptable for local development if it still survives restart for that environment; production-like use expects PostgreSQL. Durability is required in whichever store is active.
- Constitution auditability for this feature is satisfied by stored tickets, comments, and created/last-changed times—not a separate immutable audit log.
