package com.tickets.api;

import com.tickets.domain.TicketPriority;
import com.tickets.domain.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        String title,
        String description,
        TicketPriority priority,
        String assignee,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments) {
}
