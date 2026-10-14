package com.orders.domain.model;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.orders.exception.InvalidOrderStateException;



public class OrderLifecycleTest {

     private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-LIFECYCLE-001")
                .customerId(UUID.randomUUID())
                .totalAmount(new BigDecimal("250.00"))
                .currency("QAR")
                .status(OrderStatus.CREATED)
                .inventoryStatus(InventoryStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Inventory first: order is confirmed after payment completes")
    void inventoryFirstThenPaymentShouldConfirmOrder() {
        order.markInventoryReserved();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(PaymentStatus.PENDING, order.getPaymentStatus());
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());

        order.markPaymentCompleted();

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Payment first: order is confirmed after inventory is reserved")
    void paymentFirstThenInventoryShouldConfirmOrder() {
        order.markPaymentCompleted();

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(InventoryStatus.PENDING, order.getInventoryStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());

        order.markInventoryReserved();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    @DisplayName("Payment completion alone must not confirm order")
    void paymentCompletionAloneShouldNotConfirmOrder() {
        order.markPaymentCompleted();

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    @DisplayName("Inventory reservation alone must not confirm order")
    void inventoryReservationAloneShouldNotConfirmOrder() {
        order.markInventoryReserved();

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());
    }

    @Test
    @DisplayName("Payment failure cancels order")
    void paymentFailureShouldCancelOrder() {
        order.markInventoryReserved();

        order.markPaymentFailed("Payment declined");

        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Payment declined", order.getPaymentFailureReason());
    }

    @Test
    @DisplayName("Inventory failure cancels order")
    void inventoryFailureShouldCancelOrder() {
        order.markInventoryReservationFailed("Insufficient stock");

        assertEquals(InventoryStatus.FAILED, order.getInventoryStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Insufficient stock", order.getInventoryFailureReason());
    }

    @Test
    @DisplayName("Duplicate inventory reservation is idempotent")
    void duplicateInventoryReservationShouldNotChangeOrder() {
        order.markInventoryReserved();

        assertDoesNotThrow(order::markInventoryReserved);

        assertEquals(InventoryStatus.RESERVED, order.getInventoryStatus());
        assertEquals(OrderStatus.INVENTORY_RESERVED, order.getStatus());
    }

    @Test
    @DisplayName("Duplicate payment completion is idempotent")
    void duplicatePaymentCompletionShouldNotChangeOrder() {
        order.markPaymentCompleted();

        assertDoesNotThrow(order::markPaymentCompleted);

        assertEquals(PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    @DisplayName("Duplicate inventory failure is idempotent")
    void duplicateInventoryFailureShouldNotChangeOrder() {

        order.markInventoryReservationFailed("Insufficient stock");

        assertDoesNotThrow(
                () -> order.markInventoryReservationFailed("Duplicate failure"));

        assertEquals(InventoryStatus.FAILED, order.getInventoryStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Insufficient stock", order.getInventoryFailureReason());
    }

    @Test
    @DisplayName("Duplicate payment failure is idempotent")
    void duplicatePaymentFailureShouldNotChangeOrder() {

        order.markPaymentFailed("Payment declined");

        assertDoesNotThrow(
                () -> order.markPaymentFailed("Duplicate failure"));

        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Payment declined", order.getPaymentFailureReason());
    }

    @Test
    @DisplayName("Inventory failure after reservation is rejected")
    void inventoryFailureAfterReservationShouldBeRejected() {

        order.markInventoryReserved();

        assertThrows(
                InvalidOrderStateException.class,
                () -> order.markInventoryReservationFailed("Insufficient stock"));
    }

    @Test
    @DisplayName("Payment failure after successful payment is rejected")
    void paymentFailureAfterCompletionShouldBeRejected() {
        
        order.markPaymentCompleted();

        assertThrows(
                InvalidOrderStateException.class,
                () -> order.markPaymentFailed("Payment declined"));
    }

    @Test
    @DisplayName("Inventory reservation after cancellation is rejected")
    void inventoryReservationAfterCancellationShouldBeRejected() {
        order.markInventoryReservationFailed("Insufficient stock");

        assertThrows(
                InvalidOrderStateException.class,
                order::markInventoryReserved);
    }
}
