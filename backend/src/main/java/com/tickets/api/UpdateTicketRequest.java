package com.tickets.api;

import com.tickets.domain.TicketPriority;
import jakarta.validation.constraints.Size;

public final class UpdateTicketRequest {

    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @Size(min = 1, max = 10000, message = "Description must be between 1 and 10000 characters")
    private String description;

    private TicketPriority priority;

    @Size(max = 100, message = "Assignee must not exceed 100 characters")
    private String assignee;

    private boolean assigneePresent;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : trim(title);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : trim(description);
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        assigneePresent = true;
        String trimmed = trim(assignee);
        this.assignee = trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    public boolean isAssigneePresent() {
        return assigneePresent;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
