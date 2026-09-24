---
description: Test-first rules for ticket behavior, auth, persistence, and UI errors
globs: "**/{*Test.java,*IT.java,*.test.tsx,*.test.ts}"
alwaysApply: false
---

# Testing

Constitution Principle III: tests that fail for the intended behavior MUST exist before production code.

## Stack

- Backend: JUnit 5, Spring Boot Test, MockMvc for HTTP contracts.
- Frontend: React Testing Library for field errors, 422 transition messages, and 404 copy.

## What MUST be covered

- `TicketStatusMachine`: every allowed transition; same-status no-op; forbidden set including `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`.
- REST: success and validation-failure for create, update, comment, list/search, status.
- Unauthenticated `/api/tickets/**` → 401 (SC-006).
- Persistence of tickets/comments across restart (`local` file H2 or dedicated IT).
- Assert against `specs/001-support-ticket-core/spec.md` and `data-model.md`, not against “whatever the code currently does.”

## TDD loop

1. Write the test from the spec.
2. Run it; confirm it fails for the right reason.
3. Implement the minimum code.
4. Re-run. Do not skip tests because the change looks small if state, routing, or ticket data changed.

```java
// ❌ BAD — tautology
assertEquals(service.transition(t, OPEN).getStatus(), service.transition(t, OPEN).getStatus());

// ✅ GOOD — spec rule
assertThrows(IllegalTicketTransitionException.class,
        () -> machine.validate(TicketStatus.CLOSED, TicketStatus.OPEN));
```
