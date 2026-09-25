package com.orders.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.orders.order.model.Order;
import com.orders.outbox.OutboxEvent;
import com.orders.outbox.OutboxEventRepository;
import com.orders.outbox.OutboxStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component 
@RequiredArgsConstructor 
public class EventPublisherImpl implements  EventPublisher{


    @Value("${ORDER_CREATED_TOPIC}")
    private  final String ORDER_CREATED_TOPIC;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publishOrderCreated(Order order) {

         UUID eventId = UUID.randomUUID();

        List<OrderCreatedItem> eventItems =
                order.getItems()
                        .stream()
                        .map(item ->
                                new OrderCreatedItem(
                                        item.getProductId(),
                                        item.getProductName(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getSubtotal())).toList();

        OrderCreatedEvent event =
                new OrderCreatedEvent(
                        eventId,
                        "ORDER_CREATED",
                        Instant.now() ,
                        order.getId(),
                        order.getOrderNumber() ,
                        order.getCustomerId(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        eventItems
                );

        // -----------------------------------------
        //   Serialize Event
        // -----------------------------------------
        try {

            String payload = objectMapper.writeValueAsString(event);
        
        // -----------------------------------------
        // Create Outbox Event
        // -----------------------------------------

            OutboxEvent outboxEvent = new OutboxEvent();

            outboxEvent.setAggregateId(order.getId());
            outboxEvent.setAggregateType("ORDER");
            outboxEvent.setEventType("ORDER_CREATED");
            outboxEvent.setTopic(ORDER_CREATED_TOPIC);
            outboxEvent.setEventKey(order.getId().toString());
            outboxEvent.setPayload(payload);
            outboxEvent.setStatus(OutboxStatus.PENDING);
            outboxEvent.setRetryCount(0);

        // -----------------------------------------
        // Save Outbox Event
        // -----------------------------------------

        outboxEventRepository.save(outboxEvent);

        } catch (JsonProcessingException ex) {

            throw new IllegalStateException(
                    "Failed to serialize OrderCreatedEvent",
                    ex
            );
        }
    }

}
