package com.tickets.persistence;

import com.tickets.domain.TicketStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    @Query("""
            SELECT ticket
            FROM TicketEntity ticket
            WHERE (
                :keyword IS NULL
                OR LOWER(ticket.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
                OR LOWER(ticket.description) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\'
            )
            AND (:status IS NULL OR ticket.status = :status)
            """)
    Page<TicketEntity> search(
            @Param("keyword") String keyword,
            @Param("status") TicketStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ticket FROM TicketEntity ticket WHERE ticket.id = :ticketId")
    Optional<TicketEntity> findByIdForUpdate(@Param("ticketId") UUID ticketId);
}
