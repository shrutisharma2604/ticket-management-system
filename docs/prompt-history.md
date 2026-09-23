# Prompt History

This file logs key prompts given to AI during development, along with any mistakes caught and corrections made.

---

## Entry 1 — Constitution generation
Command: /speckit-constitution
Prompt given: Project details covering Java 21, Spring Boot 3.x, PostgreSQL/H2, REST API, React frontend, coding structure, state rules, validation, error handling, and testing requirements.

AI output: Created .specify/memory/constitution.md with 5 main principles:

Spec-First Delivery
Secure by Default
Test-First
Contract and Integration Integrity
Simplicity and Minimal Change

It also included Engineering Constraints, Quality Gates and Review, and Governance sections.

Issue found: The file had a temporary "Sync Impact Report" comment at the top. The Governance section says this comment should be removed before committing the file.

Fix applied: Removed the Sync Impact Report comment manually before finalizing the file.

---
## Entry 2 — Specification generation
Command: /speckit-specify [feature description]
AI output: specs/001-support-ticket-core/spec.md

Decision made: We kept the authentication requirement — only signed-in users can access the system, all users have the same permissions, and there is no role-based access or anonymous access.
