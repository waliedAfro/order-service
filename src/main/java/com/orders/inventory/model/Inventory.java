package com.orders.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "inventory",
        indexes = {
                @Index(name = "idx_inventory_product_id", columnList = "product_id"),
                @Index(name = "idx_inventory_created_at", columnList = "created_at")
        }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the Product Service.
     *
     * We intentionally do NOT use:
     *
     * @ManyToOne
     * private Product product;
     *
     * because Product belongs to the Product module/service.
     */
    @Column(name = "product_id", nullable = false, unique = true)
    @NotNull
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    @NotNull
    @Min(0)
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @NotNull
    @Min(0)
    private Integer reservedQuantity = 0;

    @Column(name = "available_quantity", nullable = false)
    @NotNull
    @Min(0)
    private Integer availableQuantity = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (quantity == null) {
            quantity = 0;
        }

        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }

        calculateAvailableQuantity();
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = OffsetDateTime.now();

        calculateAvailableQuantity();
    }

    private void calculateAvailableQuantity() {

        if (quantity == null) {
            quantity = 0;
        }

        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }

        availableQuantity = quantity - reservedQuantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
        calculateAvailableQuantity();
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
        calculateAvailableQuantity();
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

