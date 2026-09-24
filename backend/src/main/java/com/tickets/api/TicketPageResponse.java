package com.tickets.api;

import com.tickets.domain.TicketPriority;
import com.tickets.domain.TicketStatus;
import java.util.List;
import java.util.UUID;

public record TicketPageResponse(
        List<TicketSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public record TicketSummary(
            UUID id,
            String title,
            TicketPriority priority,
            String assignee,
            TicketStatus status) {
    }
}
