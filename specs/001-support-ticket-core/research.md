# Research: Support Ticket Core

## Auth mechanism (session vs JWT)

- **Decision**: JWT Bearer tokens issued by `POST /api/auth/login`. Ticket APIs require `Authorization: Bearer <token>`. All authenticated operators share the same permissions. No roles, refresh-token rotation, or SSO in this feature.
- **Rationale**: The spec and planning input allow session or JWT. Bearer JWT is straightforward to test from MockMvc and from a React client without CSRF machinery. Equal-permission operators map to Spring Security `authenticated()` on ticket routes.
- **Alternatives considered**: Servlet session cookies (better XSS posture with HttpOnly, but CSRF and same-site proxy complexity). OAuth2/OIDC (out of scope). Anonymous access (forbidden by FR-015 and constitution).

## Password and signing-key storage

- **Decision**: Operator username, password hash, and JWT signing key come from environment / Spring profile properties. Production values MUST NOT be committed. Test profile uses a dedicated non-production signing key and a well-known test operator created only in tests.
- **Rationale**: Constitution forbids hardcoded credentials and secrets in source.
- **Alternatives considered**: In-repo `application.yml` passwords (rejected). External IdP (rejected as extra scope).

## Persistence profiles

- **Decision**: PostgreSQL for `prod`. File-based H2 for `local` so data survives process restart. In-memory H2 for automated tests unless a test explicitly needs restart durability (then file-based temp directory). Schema via Flyway; same migration scripts for H2 and PostgreSQL (no vendor-specific SQL in v1).
- **Rationale**: Matches FR-003 and the stated stack. File H2 satisfies “local survives restart”; in-memory H2 keeps tests fast.
- **Alternatives considered**: PostgreSQL-only (slower local/CI). JPA `ddl-auto=update` without Flyway (schema drift). In-memory H2 for local (fails restart durability).

## Status machine placement

- **Decision**: A dedicated domain component (`TicketStatusMachine`) consulted only from the service layer. Controllers MUST NOT apply transitions. PATCH on ticket details MUST NOT accept `status`. Transitions go to `POST /api/tickets/{id}/status`.
- **Rationale**: FR-006, FR-010–FR-012. A single function is easier to unit-test than annotations scattered on the entity.
- **Alternatives considered**: JPA entity setters that allow any enum (unsafe). UI-only disabled buttons (fails FR-012). Workflow engines (YAGNI).

## API error shape

- **Decision**: RFC 7807 Problem Details (`application/problem+json`) with `status`, `title`, `detail`, and optional `errors` map for field violations. Illegal transitions: HTTP 422, `detail` includes current status and requested status. Validation: HTTP 400. Unauthenticated: HTTP 401. Missing ticket: HTTP 404.
- **Rationale**: FR-013/FR-014 need field-level and transition-specific messages the React UI can render as-is.
- **Alternatives considered**: Ad-hoc `{ "message": "error" }` (weak for fields). HTTP 409 for transitions (also viable; 422 chosen to mean “syntactically OK, semantically illegal”).

## List search and pagination

- **Decision**: Spring Data `Page` with `page` (0-based), `size` (default 20, max 100), ordered by `createdAt` descending. Keyword `q` is case-insensitive substring on title and description (trimmed; blank `q` means no keyword). Optional `status` filter. Combine with AND.
- **Rationale**: Constitution pagination; FR-008/FR-009; SC-005.
- **Alternatives considered**: Full-text search engines (YAGNI). Offset-only without page metadata (poor UX).

## React client

- **Decision**: Vite + React (JavaScript or TypeScript; TypeScript preferred). React Router: login, ticket list, ticket detail. API module maps Problem Details into field errors and page-level banners. Dev server proxies `/api` to the backend origin. Token kept in memory plus `sessionStorage` so refresh keeps the session for the tab; not `localStorage`.
- **Rationale**: Planning input requires React. Proxy avoids CORS in local dev. Memory+sessionStorage is a documented XSS tradeoff vs HttpOnly cookies; acceptable for this equal-operator tool, not a public portal.
- **Alternatives considered**: Next.js (extra framework). Cookie session (see auth decision).

## Layering and DTOs

- **Decision**: `controller` → `service` → `repository`. JSON request/response types are DTOs. JPA entities never leave the service/repository boundary.
- **Rationale**: Explicit planning constraint; reduces accidental persistence leaks and lazy-load surprises.
- **Alternatives considered**: Returning entities from controllers (rejected). Hexagonal ports (heavier than needed).

## Testing strategy

- **Decision**: JUnit 5 + Spring Boot Test. Unit tests for `TicketStatusMachine` (all allowed and a representative forbidden set including Closed→Open, Resolved→Open, Cancelled→Open). `@WebMvcTest`/`MockMvc` contract tests against OpenAPI status codes and problem+json. `@SpringBootTest` integration tests for persistence, authz (401 without token), and lifecycle. Frontend: React Testing Library for validation and illegal-transition message rendering.
- **Rationale**: Constitution III and IV; SC-003/SC-006.
- **Alternatives considered**: UI-only E2E as the sole gate (too late to catch machine bugs).
