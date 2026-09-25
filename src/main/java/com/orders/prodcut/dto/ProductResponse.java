package com.orders.prodcut.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class ProductResponse {

    private UUID id;

    private String sku;

    private String name;

    private String description;

    private BigDecimal price;

    private String currency;

    private Integer stockQuantity;

    private Boolean active;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
