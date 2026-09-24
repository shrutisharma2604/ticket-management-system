---
description: Java 21 and Spring Boot 3.x layering, persistence, and style for this repo
globs: backend/**/*.java
alwaysApply: false
---

# Java / Spring Boot

Stack: Java 21, Spring Boot 3.x, constructor injection, `final` collaborators.

## Layers

- `api` (controllers, DTOs, exception handlers) → `service` → `persistence` (JPA + repositories).
- Business rules and ticket status transitions belong in `service` / `domain` only. Controllers MUST NOT apply the state machine.
- REST responses and requests MUST be DTOs. NEVER serialize JPA entities.

## Style

- Oracle Java Code Conventions: PascalCase types, camelCase methods, UPPER_SNAKE_CASE constants.
- 4-space indent, max 120 characters.
- Return `Optional<T>` instead of null. NEVER call `Optional.get()` without `isPresent()` or `orElse` / `orElseThrow`.
- Catch specific exceptions. NEVER swallow exceptions. NEVER use exceptions for control flow. Use try-with-resources.
- Entities MUST NOT use Lombok `@Data`. DTOs MAY use `@Data` / `@Value` / `@Builder`.
- Equals/hashCode on entities: `id` only after persist.

## Persistence

- Flyway for schema. Paginate ticket lists (`page` 0-based, default size 20, max 100).
- Prefer joins over N+1. PostgreSQL in `prod`; file H2 `local`; in-memory H2 `test`.

## Security and logging

- Secrets only from environment. NEVER hardcode credentials or JWT keys.
- Parameterized SLF4J. NEVER log passwords, tokens, or full ticket bodies.

```java
// ❌ BAD — entity in controller, machine in controller
return ticketRepository.findById(id).get();

// ✅ GOOD
return ticketService.getById(id).orElseThrow(() -> new TicketNotFoundException(id));
```
