package com.orders.order.dto;

import java.math.BigDecimal; 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.orders.order.model.OrderStatus;

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
