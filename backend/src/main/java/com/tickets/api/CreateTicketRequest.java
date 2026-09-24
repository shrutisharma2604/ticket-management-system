package com.tickets.api;

import com.tickets.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must not exceed 200 characters")
        String title,
        @NotBlank(message = "Description is required")
        @Size(max = 10000, message = "Description must not exceed 10000 characters")
        String description,
        @NotNull(message = "Priority is required")
        TicketPriority priority,
        @Size(max = 100, message = "Assignee must not exceed 100 characters")
        String assignee) {

    public CreateTicketRequest {
        title = trim(title);
        description = trim(description);
        assignee = normalizeAssignee(assignee);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeAssignee(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
