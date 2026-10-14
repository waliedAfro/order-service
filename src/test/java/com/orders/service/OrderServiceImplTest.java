package com.orders.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.orders.common.events.PaymentCompletedEvent;
import com.orders.common.events.PaymentFailedEvent;
import com.orders.domain.dto.OrderCreateRequest;
import com.orders.domain.dto.OrderItemRequest;
import com.orders.domain.model.InventoryStatus;
import com.orders.domain.model.Order;
import com.orders.domain.model.OrderStatus;
import com.orders.domain.model.PaymentStatus;
import com.orders.exception.OrderNotFoundException;
import com.orders.idempotency.ProcessedEvent;
import com.orders.idempotency.ProcessedEventRepository;
import com.orders.messaging.publisher.EventPublisher;
import com.orders.repository.OrderRepository;
import com.orders.service.OrderServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private UUID customerId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .orderNumber("ORD-TEST-001")
                .customerId(customerId)
                .totalAmount(new BigDecimal("150.00"))
                .currency("QAR")
                .status(OrderStatus.CREATED)
                .inventoryStatus(InventoryStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should create order, calculate total, save and publish event")
    void shouldCreateOrderSuccessfully() {
        OrderItemRequest item = new OrderItemRequest(
                UUID.randomUUID(),
                "Laptop",
                2,
                new BigDecimal("75.00"));

        OrderCreateRequest request = new OrderCreateRequest(
                customerId,
                new BigDecimal("150.00"),
                "qar",
                List.of(item));

        when(orderRepository.existsByOrderNumber(anyString()))
                .thenReturn(false);

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.create(request);

        assertNotNull(response);
        assertEquals(customerId, response.customerId());
        assertEquals(new BigDecimal("150.00"), response.totalAmount());
        assertEquals("QAR", response.currency());
        assertEquals(OrderStatus.CREATED, response.status());
        assertEquals(1, response.items().size());

        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(any(Order.class));
    }

    @Test
    @DisplayName("Should retrieve an existing order by ID")
    void shouldGetOrderById() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        var response = orderService.getById(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        assertEquals("ORD-TEST-001", response.orderNumber());

        verify(orderRepository).findById(orderId);
    }

    @Test
    @DisplayName("Should throw when order does not exist")
    void shouldThrowWhenOrderNotFound() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getById(orderId));
    }

    @Test
    @DisplayName("Should mark inventory as reserved")
    void shouldMarkInventoryReserved() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        orderService.markInventoryReserved(orderId);

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Should cancel order when inventory reservation fails")
    void shouldMarkInventoryReservationFailed() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        orderService.markInventoryReservationFailed(
                orderId, "Insufficient stock");

        assertEquals(InventoryStatus.FAILED, order.getInventoryStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Insufficient stock", order.getInventoryFailureReason());

        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("Should process a payment completed event")
    void shouldProcessPaymentCompletedEvent() {

        order.setStatus(OrderStatus.CREATED);

        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String transactionId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now();

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, paymentId, orderId, new BigDecimal("150"), transactionId, occurredAt);

        when(processedEventRepository
                .existsByEventIdAndConsumerName(any(), anyString()))
                .thenReturn(false);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processPaymentCompleted(event);

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should ignore a duplicate payment completed event")
    void shouldIgnoreDuplicatePaymentCompletedEvent() {

        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String transactionId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now();

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, paymentId, orderId, new BigDecimal("150"), transactionId, occurredAt);

        when(processedEventRepository
                .existsByEventIdAndConsumerName(any(), anyString()))
                .thenReturn(true);

        orderService.processPaymentCompleted(event);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any(Order.class));
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should process a payment failed event")
    void shouldProcessPaymentFailedEvent() {
        order.setStatus(OrderStatus.PAYMENT_PENDING);

        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        PaymentFailedEvent event = new PaymentFailedEvent(
                eventId,
                paymentId,
                orderId,
                new BigDecimal("150"),
                "Payment declined",
                occurredAt);

        when(processedEventRepository
                .existsByEventIdAndConsumerName(any(), anyString()))
                .thenReturn(false);

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processPaymentFailed(event);

        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Payment declined", order.getPaymentFailureReason());

        verify(orderRepository).save(order);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("Should ignore a duplicate payment failed event")
    void shouldIgnoreDuplicatePaymentFailedEvent() {

        Instant occurredAt = Instant.now();
        UUID orderId = UUID.randomUUID();

        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID(),
                orderId,
                UUID.randomUUID(),
                new BigDecimal("150"),
                "Payment declined",
                occurredAt);

        when(processedEventRepository
                .existsByEventIdAndConsumerName(any(), anyString()))
                .thenReturn(true);

        orderService.processPaymentFailed(event);

        verify(orderRepository, never()).findById(any());
        verify(orderRepository, never()).save(any(Order.class));
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
