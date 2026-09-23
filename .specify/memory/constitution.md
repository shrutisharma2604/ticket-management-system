<!--
Sync Impact Report (temporary; remove before commit)
- Version change: (unratified placeholders) → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Spec-First Delivery
  - [PRINCIPLE_2_NAME] → II. Secure by Default
  - [PRINCIPLE_3_NAME] → III. Test-First (NON-NEGOTIABLE)
  - [PRINCIPLE_4_NAME] → IV. Contract and Integration Integrity
  - [PRINCIPLE_5_NAME] → V. Simplicity and Minimal Change
- Added sections:
  - Engineering Constraints
  - Quality Gates and Review
  - Governance (filled)
- Removed sections: none (template scaffold retained)
- Follow-up TODOs: none
-->

# Ticket Management System Constitution

## Core Principles

### I. Spec-First Delivery
Every user-visible capability MUST begin as a Spec Kit feature specification
before implementation. Plans and tasks MUST trace to that spec. Code MUST NOT
introduce behavior that the spec does not authorize. When implementation
diverges from the spec, the spec MUST be amended first. Runtime guidance lives
in `.specify/memory/constitution.md` and the active feature artifacts under
`.specify/` and `specs/`.

Rationale: A ticket system encodes workflow, authorization, and audit rules.
Unspecified behavior becomes silent policy.

### II. Secure by Default
Authentication, authorization, input validation, and auditability are
non-optional for ticket create, read, update, assign, comment, and status
transitions. Features MUST NOT use `eval`, unsafe deserialization, hardcoded
credentials, disabled TLS, permissive CORS, or auth/validation bypasses.
Secrets (API keys, tokens, passwords, certificates, private keys) MUST NEVER
appear in source, tests, logs, or documentation. Tickets MAY contain personal
or operationally sensitive data; logs MUST NOT dump ticket bodies, credentials,
or tokens.

Rationale: Ticket records are operational records. A leak or privilege bypass
is a product failure, not a later hardening task.

### III. Test-First (NON-NEGOTIABLE)
Behavior and logic changes MUST have tests written that fail for the intended
behavior before production code is added or altered. Tests MUST cover
authorization boundaries, invalid transitions, and persistence of ticket
state. Code MUST be able to pass lint, type checks, and the project test
suite. If tests cannot be run, the change MUST state why and the follow-up
required. Skipping tests because "it is a small UI tweak" is not allowed when
state, routing, or rendered ticket data changes.

Rationale: Ticket status and assignment bugs compound into lost work and
incorrect SLA reporting.

### IV. Contract and Integration Integrity
Public APIs, persistence schemas, and status-machine transitions MUST have
contract or integration coverage when they are introduced or changed.
Integration tests MUST cover: ticket lifecycle across layers, authorization
on protected operations, shared schemas, and inter-service calls if added.
Breaking API or schema changes MUST bump a documented version and include a
migration path. Callers MUST NOT rely on undocumented fields.

Rationale: Tickets are shared across agents, requesters, and reporting.
Silent contract drift breaks those consumers.

### V. Simplicity and Minimal Change
Prefer the smallest change that satisfies the spec. Do not add dependencies
unless necessary; when added, justify, prefer maintained libraries, pin
versions, and update lockfiles. Do not introduce new frameworks or patterns
unless the spec or this constitution requires them. Deep inheritance, speculative
abstractions, and drive-by refactors are forbidden. YAGNI applies: unused
ticket types, queues, or integrations MUST NOT ship "for later."

Rationale: Complexity in workflow engines is where incorrect transitions hide.

## Engineering Constraints

Java code MUST follow Oracle Java Code Conventions: PascalCase classes,
camelCase methods, UPPER_SNAKE_CASE constants, 4-space indent, 120-character
line length.

Object design MUST:
- Encapsulate with private fields; prefer constructor injection and `final`
  collaborators; limit setters when immutability is intended.
- Prefer composition over deep inheritance; program to interfaces.
- Return `Optional<T>` instead of null; NEVER call `Optional.get()` without
  `isPresent()` or `orElse` / `orElseThrow`.
- Catch specific exceptions; NEVER swallow exceptions; NEVER use exceptions
  for control flow; use try-with-resources.
- Log with parameterized messages via SLF4J (or project-equivalent); NEVER
  log secrets or full ticket payloads.

Entities MUST NOT use Lombok `@Data` (recursive equals/hashCode/toString
risk). DTOs MAY use `@Data`; immutable values MAY use `@Value`; builders MAY
use `@Builder`.

Data access MUST use pagination for large ticket lists, prefer joins over
N+1 queries, and use batch operations where the workload is bulk.

Cloud and git policy: work MUST occur on a branch named
`cursor/<ticket>-<summary>`. Direct pushes to `main` or release branches
are forbidden.

## Quality Gates and Review

A change is not review-ready unless:
- Spec, plan, and tasks (when the change is a Spec Kit feature) remain consistent.
- Tests exist for new or changed behavior and would be expected to pass.
- Security constraints in Principle II are satisfied.
- Complexity added is justified in the PR or commit description.
- UI or rendered-data changes are verified end-to-end in the browser when
  browser tools are available; otherwise via tests or HTTP checks, with gaps
  stated.

Reviews MUST reject: auth bypass, secrets in tree, untested status-machine
changes, and scope expansion beyond the spec.

## Governance

This constitution supersedes informal practice, README snippets, and chat
conventions wherever they conflict. Implementers and reviewers MUST verify
compliance on every pull request.

Amendments:
1. Propose the change in `.specify/memory/constitution.md` with a Sync Impact
   Report HTML comment describing version bump, modified principles, and
   added or removed sections.
2. Version using semantic versioning: MAJOR for removed or incompatible
   principle redefinitions; MINOR for new or materially expanded principles
   or sections; PATCH for clarification and non-semantic wording.
3. Set `Last Amended` to the amendment date; keep `Ratified` as the original
   adoption date.
4. Remove the Sync Impact Report before merging the amended constitution.
5. If an amendment requires migration (renamed gates, new required tests),
   the PR MUST include that migration plan.

Compliance review: feature specs and plans MUST NOT contradict this file.
Where a spec needs an exception, the exception MUST be documented in the spec
and approved as a constitution amendment if it is standing policy.

**Version**: 1.0.0 | **Ratified**: 2026-09-23 | **Last Amended**: 2026-09-23
