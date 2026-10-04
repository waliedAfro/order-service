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
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "orders", 
indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "status")
} ,
uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_order_number",
            columnNames = {"order_number"}
        )
    }
)
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

    /**
     * Move the order into PAYMENT_PENDING.
     *
     * Valid transitions:
     *
     * CREATED -> PAYMENT_PENDING
     *
     * This should normally happen after the OrderCreated
     * event has been accepted for payment processing.
     */
    public void markPaymentPending() {

        if (status != OrderStatus.CREATED) {

            throw new InvalidOrderStateException(
                    "Cannot move order " + id +
                            " to PAYMENT_PENDING from status " +
                            status);
        }

        status = OrderStatus.PAYMENT_PENDING;
    }

    /**
     * Mark the order as PAID.
     *
     * Valid transition:
     *
     * PAYMENT_PENDING -> PAID
     */
    public void markPaid() {

        if (status != OrderStatus.PAYMENT_PENDING) {

            throw new InvalidOrderStateException(
                    "Cannot mark order " + id +
                            " as PAID from status " +
                            status);
        }

        status = OrderStatus.PAID;

        paymentFailureReason = null;
    }

    /**
     * Mark the order payment as failed.
     *
     * Valid transition:
     *
     * PAYMENT_PENDING -> PAYMENT_FAILED
     */
    public void markPaymentFailed(String reason) {

        if (status != OrderStatus.PAYMENT_PENDING) {

            throw new InvalidOrderStateException(
                    "Cannot mark payment as FAILED for order "
                            + id
                            + " from status "
                            + status);
        }

        status = OrderStatus.PAYMENT_FAILED;

        paymentFailureReason = reason;
    }

}
