package com.orders.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            OutboxStatus status,
            Instant now);

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE e.status = :status
              AND (
                  e.nextRetryAt IS NULL
                  OR e.nextRetryAt <= :now
              )
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findEventsForProcessing(@Param("status") OutboxStatus status,
            @Param("now") Instant now, Pageable pageable);

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE
                e.status = com.orders.outbox.OutboxStatus.PENDING
                OR (
                    e.status = com.orders.outbox.OutboxStatus.FAILED
                    AND e.nextRetryAt <= :now
                )
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findEventsForProcessing(
            @Param("now") Instant now,
            Pageable pageable);
}
