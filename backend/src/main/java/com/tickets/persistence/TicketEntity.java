package com.tickets.persistence;

import com.tickets.domain.TicketPriority;
import com.tickets.domain.TicketStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Column(length = 100)
    private String assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentEntity> comments = new ArrayList<>();

    protected TicketEntity() {
    }

    public TicketEntity(
            String title,
            String description,
            TicketPriority priority,
            String assignee,
            TicketStatus status,
            Instant createdAt,
            Instant updatedAt) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.assignee = assignee;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<CommentEntity> getComments() {
        return Collections.unmodifiableList(comments);
    }

    public void transitionTo(TicketStatus requestedStatus, Instant changedAt) {
        status = requestedStatus;
        updatedAt = changedAt;
    }

    public boolean updateDetails(
            String requestedTitle,
            String requestedDescription,
            TicketPriority requestedPriority,
            boolean assigneePresent,
            String requestedAssignee,
            Instant changedAt) {
        boolean changed = false;
        if (requestedTitle != null && !requestedTitle.equals(title)) {
            title = requestedTitle;
            changed = true;
        }
        if (requestedDescription != null && !requestedDescription.equals(description)) {
            description = requestedDescription;
            changed = true;
        }
        if (requestedPriority != null && requestedPriority != priority) {
            priority = requestedPriority;
            changed = true;
        }
        if (assigneePresent && !Objects.equals(assignee, requestedAssignee)) {
            assignee = requestedAssignee;
            changed = true;
        }
        if (changed) {
            updatedAt = changedAt;
        }
        return changed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TicketEntity ticket) || id == null) {
            return false;
        }
        return id.equals(ticket.id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
