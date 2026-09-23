# Review Code

Review the code changes in the current diff or the file(s) specified against this project's constitution (`.specify/memory/constitution.md`).

Check specifically for:
- Layering violations (business logic or state-machine logic in controllers instead of services)
- JPA entities exposed directly in REST responses instead of DTOs
- Missing server-side validation
- Swallowed or overly broad exception handling
- Use of `Optional.get()` without a presence check
- Secrets, credentials, or full ticket payloads in logs
- Missing tests for new or changed behavior
- Any invalid ticket status transition not rejected by the service layer

Report findings as a list: file, line/area, issue, and suggested fix. Do not silently rewrite code — flag issues and propose the fix for review.
