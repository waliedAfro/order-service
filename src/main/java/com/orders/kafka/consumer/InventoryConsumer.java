package com.orders.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import  com.orders.service.OrderService;
import com.orders.idempotency.IdempotencyService;
import com.orders.common.events.InventoryReservedEvent;
import com.orders.common.events.InventoryReservationFailedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryConsumer {

    private static final String CONSUMER_NAME = "ORDER_INVENTORY";
    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "${spring.kafka.topics.inventory-reserved}", groupId = "${spring.kafka.consumer.group-id}", 
    containerFactory = "kafkaInventoryReservedListenerContainerFactory")
    public void consumeInventoryReserved(InventoryReservedEvent event) {
        
        log.info("Received InventoryReservedEvent: " + "eventId={}, orderId={}, orderNumber={}", 
        event.getEventId(),
        event.getOrderId(), 
        event.getOrderNumber());
        
        if (idempotencyService.alreadyProcessed(event.getEventId(), CONSUMER_NAME)) {

            log.info("InventoryReservedEvent already processed: eventId={}", 
            event.getEventId());
            return;
        }

        orderService.markInventoryReserved(event.getOrderId());

        idempotencyService.markProcessed(event.getEventId(), CONSUMER_NAME);

        log.info("InventoryReservedEvent processed successfully: " 
        + "eventId={}, orderId={}", 
            event.getEventId(),
                event.getOrderId());
    }

    @KafkaListener(topics = "${spring.kafka.topics.inventory-reservation-failed}", groupId = "${spring.kafka.consumer.group-id}", 
    containerFactory = "kafkaInventoryReservationFailedListenerContainerFactory")
    public void consumeInventoryReservationFailed(InventoryReservationFailedEvent event) {

        log.warn("Received InventoryReservationFailedEvent: " 
        + "eventId={}, orderId={}, orderNumber={}, reason={}",
                event.getEventId(), 
                event.getOrderId(), 
                event.getOrderNumber(),
                event.getReason());

        if (idempotencyService.alreadyProcessed(event.getEventId(), CONSUMER_NAME)) {

            log.info("InventoryReservationFailedEvent already processed: " 
            + "eventId={}", event.getEventId());

            return;
        }

        orderService.markInventoryReservationFailed(event.getOrderId(), event.getReason());

        idempotencyService.markProcessed(event.getEventId(), CONSUMER_NAME);

        log.info("InventoryReservationFailedEvent processed successfully: " + "eventId={}, orderId={}", event.getEventId(),
                event.getOrderId());
    }
}
