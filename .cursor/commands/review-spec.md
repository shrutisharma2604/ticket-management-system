# Review Spec

Review the specification file(s) under `spec/` for completeness and internal consistency.

Check specifically for:
- All 10 functional requirements from the assignment are represented (create, list, view, update, comment, search, filter, persistence, backend validation, meaningful UI errors)
- The state machine section exactly matches: OPEN → IN_PROGRESS → RESOLVED → CLOSED, OPEN → CANCELLED, IN_PROGRESS → CANCELLED, with all other transitions explicitly forbidden
- No invented requirements not present in the original assignment
- Acceptance criteria are concrete and testable, not vague
- data-model.md, api-contract.md, and state-machine.md are consistent with each other (same field names, same status values)

Report any gaps, contradictions, or vague/untestable criteria found.
