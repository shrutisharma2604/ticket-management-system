# Generate Tests

Generate tests for the specified class, service, or feature, following this project's Test-First principle.

Requirements:
- Unit tests for service-layer logic, especially ticket state-machine transition validation (both valid and invalid transitions, including all explicitly forbidden ones: CLOSED→OPEN, RESOLVED→OPEN, CANCELLED→OPEN)
- Integration tests for REST endpoints covering success and validation-failure cases
- Tests must assert on both success paths and rejection/error paths
- Do not write tests that only assert the code does what the code currently does (avoid tautological tests) — assert against the actual spec requirement in `spec/state-machine.md` or the relevant spec file
- Use JUnit 5 and Spring Boot Test conventions
