package com.orders.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.UUID;

import com.orders.common.events.InventoryReservedEvent;
import com.orders.common.events.InventoryReservationFailedEvent;
import com.orders.idempotency.IdempotencyService;
import com.orders.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InventoryConsumerTest {

    private static final String CONSUMER_NAME = "ORDER_INVENTORY";

    @Mock
    private OrderService orderService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private InventoryConsumer inventoryConsumer;

    private UUID eventId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    void consumeInventoryReserved_shouldProcessNewEvent() {

        InventoryReservedEvent event = mock(InventoryReservedEvent.class);

        when(event.getEventId()).thenReturn(eventId);
        when(event.getOrderId()).thenReturn(orderId);
        when(event.getOrderNumber()).thenReturn("ORD-TEST-001");

        when(idempotencyService.alreadyProcessed(
                eventId, CONSUMER_NAME))
                .thenReturn(false);

        inventoryConsumer.consumeInventoryReserved(event);

        verify(idempotencyService)
                .alreadyProcessed(eventId, CONSUMER_NAME);

        // The order ID, not the event ID, identifies the order.
        verify(orderService).markInventoryReserved(orderId);

        verify(idempotencyService)
                .markProcessed(eventId, CONSUMER_NAME);

        verifyNoMoreInteractions(orderService);
    }

    @Test
    void consumeInventoryReserved_shouldIgnoreDuplicateEvent() {

        InventoryReservedEvent event = mock(InventoryReservedEvent.class);

        when(event.getEventId()).thenReturn(eventId);
        when(idempotencyService.alreadyProcessed(eventId, CONSUMER_NAME)).thenReturn(true);

        inventoryConsumer.consumeInventoryReserved(event);

        verify(idempotencyService).alreadyProcessed(eventId, CONSUMER_NAME);

        verifyNoInteractions(orderService);

        verify(idempotencyService, never()).markProcessed(any(), anyString());
    }

    @Test
    void consumeInventoryReserved_shouldNotMarkProcessedWhenServiceFails() {

        InventoryReservedEvent event = mock(InventoryReservedEvent.class);

        when(event.getEventId()).thenReturn(eventId);
        when(event.getOrderId()).thenReturn(orderId);
        when(idempotencyService.alreadyProcessed(eventId, CONSUMER_NAME)).thenReturn(false);

        doThrow(new RuntimeException("Database unavailable"))
                .when(orderService)
                .markInventoryReserved(orderId);

        assertThrows( RuntimeException.class,
                () -> inventoryConsumer.consumeInventoryReserved(event));

        verify(idempotencyService, never()).markProcessed(eventId, CONSUMER_NAME);
    }

    @Test
    void consumeInventoryReservationFailed_shouldProcessNewEvent() {

        InventoryReservationFailedEvent event = mock(InventoryReservationFailedEvent.class);

        String reason = "Insufficient stock";

        when(event.getEventId()).thenReturn(eventId);
        when(event.getOrderId()).thenReturn(orderId);
        when(event.getOrderNumber()).thenReturn("ORD-TEST-001");
        when(event.getReason()).thenReturn(reason);

        when(idempotencyService.alreadyProcessed(eventId, CONSUMER_NAME)).thenReturn(false);
        inventoryConsumer.consumeInventoryReservationFailed(event);
        verify(orderService).markInventoryReservationFailed(orderId, reason);
        verify(idempotencyService).markProcessed(eventId, CONSUMER_NAME);
    }

    @Test
    void consumeInventoryReservationFailed_shouldIgnoreDuplicateEvent() {

        InventoryReservationFailedEvent event = mock(InventoryReservationFailedEvent.class);

        when(event.getEventId()).thenReturn(eventId);

        when(idempotencyService.alreadyProcessed(eventId, CONSUMER_NAME)).thenReturn(true);

        inventoryConsumer.consumeInventoryReservationFailed(event);

        verify(idempotencyService).alreadyProcessed(eventId, CONSUMER_NAME);

        verifyNoInteractions(orderService);

        verify(idempotencyService, never()).markProcessed(any(), anyString());
    }
}
