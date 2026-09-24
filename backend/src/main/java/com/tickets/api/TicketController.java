package com.tickets.api;

import com.tickets.domain.TicketStatus;
import com.tickets.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public TicketPageResponse list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ticketService.list(q, status, page, size);
    }

    @GetMapping("/{ticketId}")
    public TicketDetailResponse getById(@PathVariable UUID ticketId) {
        return ticketService.getById(ticketId);
    }

    @PostMapping("/{ticketId}/status")
    public TicketResponse transition(
            @PathVariable UUID ticketId,
            @Valid @RequestBody StatusTransitionRequest request) {
        return ticketService.transition(ticketId, request.status());
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse update(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request) {
        return ticketService.updateDetails(ticketId, request);
    }

    @PostMapping("/{ticketId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request) {
        return ticketService.addComment(ticketId, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.create(request);
    }
}
