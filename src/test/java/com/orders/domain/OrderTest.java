package com.orders.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.orders.domain.model.Order;
import com.orders.domain.model.OrderStatus;
import com.orders.domain.model.InventoryStatus;
import com.orders.domain.model.PaymentStatus;
import com.orders.exception.InvalidOrderStateException;

public class OrderTest {

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-000001")
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("150.00"))
                .currency("QAR")
                .status(OrderStatus.CREATED)
                .inventoryStatus(InventoryStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should confirm order when inventory and payment succeed")
    void shouldConfirmOrderWhenInventoryAndPaymentSucceed() {
        order.markInventoryReserved();

        order.markPaymentCompleted();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Should confirm order when payment succeeds before inventory")
    void shouldConfirmWhenPaymentCompletesBeforeInventoryReservation() {
        order.markPaymentCompleted();

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        order.markInventoryReserved();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Should not confirm order when inventory is reserved but payment is pending")
    void shouldNotConfirmWhenPaymentIsPending() {
        order.markInventoryReserved();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());
    }

    @Test
    @DisplayName("Should cancel order when payment fails")
    void shouldCancelOrderWhenPaymentFails() {
        order.markInventoryReserved();

        order.markPaymentFailed("Payment declined");

        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Payment declined", order.getPaymentFailureReason());
    }

    @Test
    @DisplayName("Should cancel order when inventory reservation fails")
    void shouldCancelOrderWhenInventoryReservationFails() {
        order.markInventoryReservationFailed("Insufficient stock");

        assertEquals(InventoryStatus.FAILED, order.getInventoryStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Insufficient stock", order.getInventoryFailureReason());
    }

    @Test
    @DisplayName("Should use a default reason when inventory failure reason is blank")
    void shouldUseDefaultInventoryFailureReason() {
        order.markInventoryReservationFailed(" ");

        assertEquals(
                "Inventory reservation failed",
                order.getInventoryFailureReason());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    @DisplayName("Should treat duplicate inventory-reserved events as idempotent")
    void shouldIgnoreDuplicateInventoryReservedTransition() {
        order.markInventoryReserved();

        assertDoesNotThrow(order::markInventoryReserved);

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
    }

    @Test
    @DisplayName("Should treat duplicate payment-completed events as idempotent")
    void shouldIgnoreDuplicatePaymentCompletedTransition() {
        order.markPaymentCompleted();

        assertDoesNotThrow(order::markPaymentCompleted);

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
    }

    @Test
    @DisplayName("Should reject inventory failure after inventory was reserved")
    void shouldRejectInventoryFailureAfterReservation() {
        order.markInventoryReserved();

        assertThrows(
                InvalidOrderStateException.class,
                () -> order.markInventoryReservationFailed("Insufficient stock"));
    }

    @Test
    @DisplayName("Should reject payment failure after payment completed")
    void shouldRejectPaymentFailureAfterPaymentCompleted() {
        order.markPaymentCompleted();

        assertThrows(
                InvalidOrderStateException.class,
                () -> order.markPaymentFailed("Payment declined"));
    }

    @Test
    @DisplayName("Should reject inventory changes after order is cancelled")
    void shouldRejectInventoryChangesAfterCancellation() {
        order.markInventoryReservationFailed("Insufficient stock");

        assertThrows(
                InvalidOrderStateException.class,
                order::markInventoryReserved);
    }
}
