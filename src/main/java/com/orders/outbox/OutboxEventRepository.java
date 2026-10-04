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

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE
                e.status = com.orders.outbox.OutboxStatus.PENDING
                OR (
                    e.status = com.orders.outbox.OutboxStatus.FAILED
                    AND e.nextRetryAt IS NOT NULL
                    AND e.nextRetryAt <= :now
                )
            ORDER BY e.createdAt ASC
            """)
    List<OutboxEvent> findEventsForProcessing(
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
            SELECT e
            FROM OutboxEvent e
            WHERE
                e.status = com.orders.outbox.OutboxStatus.PROCESSING

                AND e.processingAt IS NOT NULL

                AND e.processingAt <= :staleBefore

            ORDER BY e.processingAt ASC
            """)
    List<OutboxEvent> findStaleProcessingEvents(
            @Param("staleBefore") Instant staleBefore,
            Pageable pageable);
}
