package com.orders.inventory;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.orders.common.events.OrderCreatedEvent;
import com.orders.consumer.IdempotencyService;
import com.orders.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryConsumer {

    private static final String CONSUMER_NAME = "INVENTORY";
    private final IdempotencyService idempotencyService;
    private final InventoryService inventoryService;

    @KafkaListener(topics = "${kafka.topics.order-created}", groupId = "inventory-service")
    @Transactional
    public void consume(OrderCreatedEvent event) {

        log.info("Received OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());

        // -------------------------------------------------
        // Idempotency check
        // -------------------------------------------------
        if (idempotencyService.alreadyProcessed(event.eventId(), CONSUMER_NAME)) {

            log.info("Duplicate inventory event ignored: eventId={}", event.eventId());

            return;
        }

        // -------------------------------------------------
        // Reserve inventory
        // -------------------------------------------------
        reserveInventory(event);
        // -------------------------------------------------
        // Mark event as processed
        // -------------------------------------------------
        idempotencyService.markProcessed(event.eventId(), CONSUMER_NAME);
        log.info("Inventory processed successfully: orderId={}", event.orderId());

    }

    private void reserveInventory(OrderCreatedEvent event) {
        log.info("Reserving inventory for orderId={}, items={}",
                event.orderId(), event.items().size());
        event.items().forEach(item -> {
            log.info("Reserving inventory: " + "orderId={}, productId={}, productName={}, quantity={}",
                    event.orderId(), item.productId(), item.productName(), item.quantity());
            inventoryService.reserveStock(item.productId(), item.quantity());
        });
    }
}
