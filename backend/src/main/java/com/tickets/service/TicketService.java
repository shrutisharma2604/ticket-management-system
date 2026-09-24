package com.tickets.service;

import com.tickets.api.AddCommentRequest;
import com.tickets.api.CommentResponse;
import com.tickets.api.CreateTicketRequest;
import com.tickets.api.TicketDetailResponse;
import com.tickets.api.TicketPageResponse;
import com.tickets.api.TicketResponse;
import com.tickets.api.UpdateTicketRequest;
import com.tickets.domain.TicketStatus;
import com.tickets.domain.TicketStatusMachine;
import com.tickets.persistence.CommentEntity;
import com.tickets.persistence.CommentRepository;
import com.tickets.persistence.TicketEntity;
import com.tickets.persistence.TicketRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Sort LIST_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));

    private final TicketRepository ticketRepository;
    private final TicketStatusMachine ticketStatusMachine;
    private final CommentRepository commentRepository;

    public TicketService(
            TicketRepository ticketRepository,
            TicketStatusMachine ticketStatusMachine,
            CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketStatusMachine = ticketStatusMachine;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Instant now = Instant.now();
        TicketEntity ticket = new TicketEntity(
                request.title(),
                request.description(),
                request.priority(),
                request.assignee(),
                TicketStatus.OPEN,
                now,
                now);
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketPageResponse list(String keyword, TicketStatus status, int page, int size) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Page<TicketEntity> tickets = ticketRepository.search(
                normalizedKeyword,
                status,
                PageRequest.of(page, size, LIST_SORT));
        List<TicketPageResponse.TicketSummary> content = tickets.getContent()
                .stream()
                .map(TicketService::toSummary)
                .toList();
        return new TicketPageResponse(
                content,
                tickets.getNumber(),
                tickets.getSize(),
                tickets.getTotalElements(),
                tickets.getTotalPages());
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return escapeLikeLiteral(keyword.trim());
    }

    private static String escapeLikeLiteral(String keyword) {
        return keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(UUID ticketId) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        List<CommentResponse> comments = commentRepository.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId)
                .stream()
                .map(TicketService::toCommentResponse)
                .toList();
        return toDetail(ticket, comments);
    }

    @Transactional
    public TicketResponse transition(UUID ticketId, TicketStatus requestedStatus) {
        TicketEntity ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        TicketStatus currentStatus = ticket.getStatus();
        ticketStatusMachine.validate(currentStatus, requestedStatus);
        if (currentStatus != requestedStatus) {
            ticket.transitionTo(requestedStatus, Instant.now());
        }
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse updateDetails(UUID ticketId, UpdateTicketRequest request) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        ticket.updateDetails(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                request.isAssigneePresent(),
                request.getAssignee(),
                Instant.now());
        return toResponse(ticket);
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        CommentEntity comment = new CommentEntity(ticket, request.body(), Instant.now());
        return toCommentResponse(commentRepository.save(comment));
    }

    private static TicketPageResponse.TicketSummary toSummary(TicketEntity ticket) {
        return new TicketPageResponse.TicketSummary(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus());
    }

    private static TicketDetailResponse toDetail(TicketEntity ticket, List<CommentResponse> comments) {
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                comments);
    }

    private static CommentResponse toCommentResponse(CommentEntity comment) {
        return new CommentResponse(comment.getId(), comment.getBody(), comment.getCreatedAt());
    }

    private static TicketResponse toResponse(TicketEntity ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
