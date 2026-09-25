package com.orders.prodcut.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@Entity
@Table(
    name = "products",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_products_sku",
            columnNames = "sku"
        )
    },
    indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_active", columnList = "active"),
        @Index(name = "idx_products_created_at", columnList = "created_at"),
        @Index(
            name = "idx_products_active_name",
            columnList = "active, name"
        )
    }
)
@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "sku",nullable = false,unique = true,length = 100)
    private String sku;

    @NotBlank
    @Size(max = 255)
    @Column(name = "name",nullable = false,length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "price",nullable = false,precision = 19,scale = 4)
    private BigDecimal price;

    @Default 
    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency",nullable = false,length = 3)
    private String currency = "QAR";

    @Default
    @NotNull
    @Column(name = "stock_quantity",nullable = false)
    private Integer stockQuantity = 0;

    @Default 
    @NotNull
    @Column(name = "active",nullable = false)
    private Boolean active = true;

    @Column(name = "created_at",nullable = false,updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private OffsetDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (currency == null) {
            currency = "QAR";
        }

        if (stockQuantity == null) {
            stockQuantity = 0;
        }

        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }


}
