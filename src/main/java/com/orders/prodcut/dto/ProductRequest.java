package com.orders.prodcut.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    @Size(max = 100, message = "SKU must not exceed 100 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00",inclusive = true,message = "Price must be greater than or equal to zero")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    @Size(min = 3,max = 3,message = "Currency must be a 3-letter ISO currency code")
    private String currency;

    @NotNull(message = "Stock quantity is required")
    @DecimalMin(value = "0",inclusive = true,message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    private Boolean active = true;
}
