package com.orders.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.orders.exception.InvalidOrderStateException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_number", columnNames = { "order_number" })
})
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "payment_failure_reason")
    private String paymentFailureReason;

    @Default
    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false)
    private InventoryStatus inventoryStatus = InventoryStatus.PENDING;

    @Default
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "inventory_failure_reason")
    private String inventoryFailureReason;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = OrderStatus.CREATED;
        }

    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Version
    @Column(nullable = false)
    private Long version;

    // ============================================================
    // ORDER ITEMS
    // ============================================================

    public void addItem(OrderItem item) {

        items.add(item);

        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {

        items.remove(item);
        item.setOrder(null);
    }

    // ============================================================
    // PAYMENT STATE TRANSITIONS
    // ============================================================

    public void markPaymentPending() {

        if (status == OrderStatus.PAYMENT_PENDING) {
            return;
        }

        ensureOrderIsNotFinal();

        if (inventoryStatus != InventoryStatus.RESERVED) {
            throw new InvalidOrderStateException(
                    "Cannot move order " + orderNumber
                            + " to PAYMENT_PENDING before inventory is reserved");
        }

        if (paymentStatus != PaymentStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot start payment for order " + orderNumber
                            + " from payment status " + paymentStatus);
        }

        status = OrderStatus.PAYMENT_PENDING;
    }

    /**
     * Records a successful payment.
     *
     * PENDING -> COMPLETED
     *
     * The centralized state machine decides whether the order
     * can be confirmed.
     */
    public void markPaymentCompleted() {

        // Idempotent duplicate event.
        if (paymentStatus == PaymentStatus.COMPLETED) {
            return;
        }

        ensureOrderIsNotFinal();

        if (paymentStatus != PaymentStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot complete payment for order " + orderNumber
                            + " from payment status " + paymentStatus);
        }

        paymentStatus = PaymentStatus.COMPLETED;
        paymentFailureReason = null;

        evaluateFinalStatus();
    }

    /**
     * Records a failed payment.
     *
     * PENDING -> FAILED
     *
     * A definitive payment failure cancels the order.
     */
    public void markPaymentFailed(String reason) {

        // Idempotent duplicate event.
        if (paymentStatus == PaymentStatus.FAILED) {
            return;
        }

        ensureOrderIsNotFinal();

        if (paymentStatus != PaymentStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot fail payment for order " + orderNumber
                            + " from payment status " + paymentStatus);
        }

        paymentStatus = PaymentStatus.FAILED;

        paymentFailureReason = reason == null || reason.isBlank()
                ? "Payment failed"
                : reason;

        evaluateFinalStatus();
    }

    // Optional convenience overload if your consumer needs it.
    public void markPaymentFailed() {
        markPaymentFailed("Payment failed");
    }

    /**
     * Backward-compatible method for existing callers.
     * Prefer markPaymentCompleted() in new code.
     */
    @Deprecated
    public void markPaid() {
        markPaymentCompleted();
    }

    // ============================================================
    // INVENTORY STATE TRANSITIONS
    // ============================================================

    public void markInventoryReserved() {

        // Duplicate event.
        if (inventoryStatus == InventoryStatus.RESERVED) {
            return;
        }

        ensureOrderIsNotFinal();

        if (inventoryStatus != InventoryStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot reserve inventory for order " + orderNumber
                            + " from inventory status " + inventoryStatus);
        }

        inventoryStatus = InventoryStatus.RESERVED;
        inventoryFailureReason = null;

        evaluateFinalStatus();
    }

    public void markInventoryReservationFailed(String reason) {

        // Duplicate event.
        if (inventoryStatus == InventoryStatus.FAILED) {
            return;
        }

        ensureOrderIsNotFinal();

        if (inventoryStatus != InventoryStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Cannot fail inventory reservation for order " + orderNumber
                            + " from inventory status " + inventoryStatus);
        }

        inventoryStatus = InventoryStatus.FAILED;

        inventoryFailureReason = reason == null || reason.isBlank()
                ? "Inventory reservation failed"
                : reason;

        evaluateFinalStatus();
    }

    // ============================================================
// CENTRALIZED ORDER STATE MACHINE
// ============================================================

private void evaluateFinalStatus() {

    // A final order must never be reopened.
    if (isFinal()) {
        return;
    }

    // Any definitive prerequisite failure cancels the order.
    if (paymentStatus == PaymentStatus.FAILED
            || inventoryStatus == InventoryStatus.FAILED) {

        status = OrderStatus.CANCELLED;
        return;
    }

    // Confirm only when both prerequisites have succeeded.
    if (paymentStatus == PaymentStatus.COMPLETED
            && inventoryStatus == InventoryStatus.RESERVED) {

        status = OrderStatus.CONFIRMED;
        return;
    }

    // Maintain the inventory milestone while waiting for payment.
    if (inventoryStatus == InventoryStatus.RESERVED
            && paymentStatus == PaymentStatus.PENDING
            && status == OrderStatus.CREATED) {

        status = OrderStatus.INVENTORY_RESERVED;
    }
}

private boolean isFinal() {

    return status == OrderStatus.CONFIRMED
            || status == OrderStatus.CANCELLED
            || status == OrderStatus.COMPLETED;
}

private void ensureOrderIsNotFinal() {

    if (isFinal()) {
        throw new InvalidOrderStateException(
                "Cannot transition final order " + orderNumber
                        + " from status " + status);
    }
}

}
