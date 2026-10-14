package com.orders.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.orders.domain.model.Order;
import com.orders.exception.OrderNotFoundException;
import com.orders.messaging.publisher.EventPublisher;
import com.orders.repository.OrderRepository;
import com.orders.idempotency.ProcessedEventRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplEventTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID orderId;
    private Order order;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        order = mock(Order.class);
    }

    @Test
    void markInventoryReserved_shouldUpdateAndSaveOrder() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        orderService.markInventoryReserved(orderId);

        verify(orderRepository).findById(orderId);
        verify(order).markInventoryReserved();
        verify(orderRepository).save(order);
    }

    @Test
    void markInventoryReservationFailed_shouldUpdateAndSaveOrder() {
        String reason = "Insufficient stock";

        when(orderRepository.findById(orderId))
                .thenReturn(Optional.of(order));

        orderService.markInventoryReservationFailed(orderId, reason);

        verify(orderRepository).findById(orderId);
        verify(order).markInventoryReservationFailed(reason);
        verify(orderRepository).save(order);
    }

    @Test
    void markInventoryReserved_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.markInventoryReserved(orderId));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void markInventoryReservationFailed_shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findById(orderId))
                .thenReturn(Optional.empty());

        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.markInventoryReservationFailed(
                        orderId, "Insufficient stock"));

        verify(orderRepository, never()).save(any());
    }
}
