package com.orders.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin; 
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull; 
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    @NotNull (message = "Customer ID is required")
    UUID customerId,

    @NotNull(message = "Total amount is required")
    @DecimalMin( value = "0.01", message = "Total amount must be greater than zero" ) 
    BigDecimal totalAmount, 

    @NotBlank(message = "Currency is required") 
    @Size( min = 3, max = 3, message = "Currency must contain exactly 3 characters" ) 
    String currency ,

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid OrderItemRequest> items
) {}
