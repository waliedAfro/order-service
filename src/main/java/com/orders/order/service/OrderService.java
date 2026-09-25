package com.orders.order.service;

import java.util.List;
import java.util.UUID;

import com.orders.order.dto.OrderCreateRequest;
import com.orders.order.dto.OrderResponse;
import com.orders.order.dto.OrderUpdateRequest;
import com.orders.order.model.OrderStatus;

public interface OrderService {

    OrderResponse create(OrderCreateRequest request);

    OrderResponse getById(UUID id);

    OrderResponse getByOrderNumber(String orderNumber);

    List<OrderResponse> getByCustomerId(UUID customerId);

    List<OrderResponse> getByStatus(OrderStatus status);

    List<OrderResponse> getAll();

    OrderResponse update(UUID id, OrderUpdateRequest request);

    void delete(UUID id);
}
