package com.tickets.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByTicketIdOrderByCreatedAtAscIdAsc(UUID ticketId);
}
