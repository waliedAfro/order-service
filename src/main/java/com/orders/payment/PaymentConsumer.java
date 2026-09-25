package com.orders.payment;

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
public class PaymentConsumer {

    private static final String CONSUMER_NAME = "PAYMENT";

    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "${kafka.topics.order-created}",groupId = "payment-service")
    @Transactional
    public void consume(OrderCreatedEvent event) {

        log.info("Received OrderCreatedEvent: eventId={}, orderId={}",
            event.eventId(),
            event.orderId()
        );

        if (idempotencyService.alreadyProcessed(event.eventId(),CONSUMER_NAME)) {

            log.info("Duplicate payment event ignored: eventId={}", event.eventId());

            return;
        }

        // -----------------------------------------
        // Payment business logic
        // -----------------------------------------

        processPayment(event);

        // -----------------------------------------
        // Mark event as processed
        // -----------------------------------------

        idempotencyService.markProcessed(event.eventId(),CONSUMER_NAME);

        log.info("Payment processed successfully: orderId={}",event.orderId());
    }

    private void processPayment(OrderCreatedEvent event) {

        log.info("Creating payment for orderId={}, amount={}",
            event.orderId(),event.totalAmount());

        // TODO:
        // paymentRepository.save(...)
    }

}
