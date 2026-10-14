package com.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import  com.orders.service.OrderService;
import  com.orders.repository.OrderRepository;
import  com.orders.outbox.OutboxEventRepository;
import  com.orders.outbox.OutboxEvent ;
import  com.orders.domain.dto.OrderItemRequest; 
import  com.orders.domain.dto.OrderCreateRequest ;
import  com.orders.domain.dto.OrderResponse; 
import  com.orders.domain.model.Order;
import  com.orders.domain.model.OrderStatus ;


public class OrderServiceIntegrationTest extends AbstractOrderIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

     @BeforeEach
    void cleanDatabase() {
        // Delete dependent records before their parent orders.
        jdbcTemplate.update("DELETE FROM order_items");
        outboxEventRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
    }

    @Test
    void shouldPersistOrderItemsAndOutboxEvent() {

        // Arrange
        UUID customerId = UUID.randomUUID();

        OrderItemRequest item1 = new OrderItemRequest(
                UUID.randomUUID(),
                "Mechanical Keyboard",
                2,
                new BigDecimal("19.99"));

        OrderItemRequest item2 = new OrderItemRequest(
                UUID.randomUUID(),
                "USB Cable",
                1,
                new BigDecimal("5.00"));

        OrderCreateRequest request = new OrderCreateRequest(
                customerId,
                new BigDecimal("999.99"),
                "qar",
                List.of(item1, item2));

        // Act: invoke the real Spring service and database repositories.
        OrderResponse response = orderService.create(request);

        // Assert 1: service response
        assertThat(response.id()).isNotNull();
        assertThat(response.orderNumber()).startsWith("ORD-");
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.currency()).isEqualTo("QAR");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);

        // Total is calculated from the items, not request.totalAmount.
        assertThat(response.totalAmount())
                .isEqualByComparingTo("44.98");

        assertThat(response.items()).hasSize(2);

        // Assert 2: persisted order
        Order savedOrder = orderRepository.findById(response.id())
                .orElseThrow(() ->
                        new AssertionError("Persisted order was not found"));

        assertThat(savedOrder.getOrderNumber())
                .isEqualTo(response.orderNumber());

        assertThat(savedOrder.getCustomerId()).isEqualTo(customerId);
        assertThat(savedOrder.getTotalAmount())
                .isEqualByComparingTo("44.98");

        // Assert 3: persisted order items
        Integer itemCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id = ?",
                Integer.class,
                response.id());

        assertThat(itemCount).isEqualTo(2);

        BigDecimal persistedSubtotal = jdbcTemplate.queryForObject(
                """
                SELECT SUM(subtotal)
                FROM order_items
                WHERE order_id = ?
                """,
                BigDecimal.class,
                response.id());

        assertThat(persistedSubtotal)
                .isEqualByComparingTo("44.98");

        // Assert 4: outbox event persisted for this order
        List<OutboxEvent> orderEvents = outboxEventRepository.findAll()
                .stream()
                .filter(event ->
                        response.id().equals(event.getAggregateId()))
                .toList();

        assertThat(orderEvents)
                .as("An outbox event should be persisted for the order")
                .hasSize(1);

        OutboxEvent outboxEvent = orderEvents.get(0);

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(response.id());

        assertThat(outboxEvent.getEventType()).isNotBlank();
        assertThat(outboxEvent.getTopic()).isNotBlank();
        assertThat(outboxEvent.getEventKey()).isNotBlank();
        assertThat(outboxEvent.getPayload())
                .contains(response.id().toString())
                .contains(response.orderNumber());
    }

}
