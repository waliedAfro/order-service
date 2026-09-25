package com.orders.audit;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.orders.common.events.OrderCreatedEvent;
import com.orders.consumer.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component 
@RequiredArgsConstructor 
@Slf4j 
public class AuditConsumer {

    private static final String CONSUMER_NAME = "AUDIT";

    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "${kafka.topics.order-created}",groupId = "audit-service")
    @Transactional
    public void consume(OrderCreatedEvent event) {

        log.info("Received OrderCreatedEvent: eventId={}, orderId={}",event.eventId(),event.orderId());

        if (idempotencyService.alreadyProcessed(event.eventId(),CONSUMER_NAME)) {

            log.info("Duplicate audit event ignored: eventId={}",event.eventId());

            return;
        }

        // -----------------------------------------
        // Audit business logic
        // -----------------------------------------

        createAuditRecord(event);

        // -----------------------------------------
        // Mark event as processed
        // -----------------------------------------

        idempotencyService.markProcessed(event.eventId(),CONSUMER_NAME);

        log.info("Audit record created: orderId={}", event.orderId());
    }

    private void createAuditRecord(OrderCreatedEvent event) {

        log.info("Creating audit record for orderId={}", event.orderId());

        // TODO:
        // auditRepository.save(...)
    }
}
