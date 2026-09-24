---
name: documentation
description: >-
  Generate and update project documentation for the support ticket system.
  Use when writing README, Javadoc, REST endpoint docs, OpenAPI notes, or
  when the user asks to document APIs, services, or how to run the app.
---

# Documentation

When generating or updating documentation for this project:

- Every REST endpoint must be documented with: method, path, request body shape, response shape, and possible error responses.
- Every service method with non-trivial logic (especially state-machine transition validation) must have a Javadoc comment explaining the rule, not just what the code does.
- README.md must always include: how to run the backend, how to run the frontend, how to run tests, and the tech stack used.
- Do not document implementation details that duplicate what the code already makes obvious (e.g. do not write "this getter returns the id").

## Additional rules

- Treat `specs/001-support-ticket-core/contracts/openapi.yaml` as the HTTP source of truth. Do not invent fields or status codes.
- NEVER put secrets, passwords, JWT values, or real operator credentials in docs. Point at environment variable names only.
- Do not log or paste ticket bodies in examples; use short placeholders.
- Prefer linking to spec/plan (`specs/001-support-ticket-core/spec.md`, `quickstart.md`) over copying large sections.

## README skeleton

```markdown
# ticket-management-system

## Tech stack
Java 21, Spring Boot 3.x, PostgreSQL/H2, REST, React (Vite).

## Run backend
## Run frontend
## Run tests
```

## Endpoint doc template

```text
METHOD /path
Request: { ... }
Response: { ... }
Errors: 400 (fields), 401, 404, 422 (illegal status) as applicable
```

## Javadoc for transitions

Document allowed pairs and that all other transitions are rejected, including `CLOSED→OPEN`, `RESOLVED→OPEN`, `CANCELLED→OPEN`. Point to `data-model.md`, not to restating getters.
