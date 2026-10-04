package com.orders.service;

import java.util.List;
import java.util.UUID;

import com.orders.common.events.PaymentCompletedEvent;
import com.orders.common.events.PaymentFailedEvent;
import com.orders.domain.dto.OrderCreateRequest;
import com.orders.domain.dto.OrderResponse;
import com.orders.domain.dto.OrderUpdateRequest;
import com.orders.domain.model.OrderStatus;

public interface OrderService {

    OrderResponse create(OrderCreateRequest request);

    OrderResponse getById(UUID id);

    OrderResponse getByOrderNumber(String orderNumber);

    List<OrderResponse> getByCustomerId(UUID customerId);

    List<OrderResponse> getByStatus(OrderStatus status);

    List<OrderResponse> getAll();

    OrderResponse update(UUID id, OrderUpdateRequest request);

    void delete(UUID id);

    void processPaymentCompleted(PaymentCompletedEvent event);

    void processPaymentFailed(PaymentFailedEvent event);
}
