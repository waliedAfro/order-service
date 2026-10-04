package com.orders.domain.dto;

import java.math.BigDecimal; 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.orders.domain.model.OrderStatus;

public record OrderResponse(
    UUID id, 
    String orderNumber, 
    UUID customerId, 
    BigDecimal totalAmount, 
    String currency, 
    OrderStatus status, 
    LocalDateTime createdAt, 
    LocalDateTime updatedAt ,
    List<OrderItemResponse> items
) {}
