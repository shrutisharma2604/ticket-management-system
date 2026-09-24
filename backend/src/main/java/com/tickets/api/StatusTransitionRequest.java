package com.tickets.api;

import com.tickets.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusTransitionRequest(
        @NotNull(message = "Status is required")
        TicketStatus status) {
}
