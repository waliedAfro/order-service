package com.orders.order.dto;

import jakarta.validation.constraints.DecimalMin; 
import jakarta.validation.constraints.NotNull; 
import jakarta.validation.constraints.Size; 
import java.math.BigDecimal; 
import java.util.UUID;

public record OrderUpdateRequest(

    @NotNull(message = "Customer ID is required") 
    UUID customerId, 


    @NotNull(message = "Total amount is required")
    @DecimalMin( 
        value = "0.01",
        message = "Total amount must be greater than zero" ) 
    BigDecimal totalAmount,
    
    @NotNull(message = "Currency is required") 
    @Size( 
        min = 3,
         max = 3, message = "Currency must contain exactly 3 characters" ) 
    String currency
) {

}
