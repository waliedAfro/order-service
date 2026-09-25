package com.orders.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orders.common.events.EventPublisher;
import com.orders.common.exception.OrderNotFoundException;
import com.orders.order.dto.OrderCreateRequest;
import com.orders.order.dto.OrderItemRequest;
import com.orders.order.dto.OrderItemResponse;
import com.orders.order.dto.OrderResponse;
import com.orders.order.dto.OrderUpdateRequest;
import com.orders.order.model.Order;
import com.orders.order.model.OrderItem;
import com.orders.order.model.OrderStatus;
import com.orders.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CREATED_EVENT = "order.created";

    private static final String ORDER_CREATED_TOPIC = "order.created";

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    @Override
    public OrderResponse create(OrderCreateRequest request) {

        // -----------------------------------------
        // 1. Create Order
        // -----------------------------------------

        String orderNumber = generateOrderNumber();

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            orderNumber = generateOrderNumber();
        }

        Order order = Order.builder()
                .customerId(request.customerId())
                .customerId(request.customerId())
                .status(OrderStatus.CREATED)
                .totalAmount(BigDecimal.ZERO)
                .currency(request.currency().toUpperCase())
                .build();

        // -----------------------------------------
        // 2. Create Order Items
        // -----------------------------------------
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {

            BigDecimal subtotal = itemRequest.unitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.productId())
                    .productName(itemRequest.productName())
                    .quantity(itemRequest.quantity())
                    .unitPrice(itemRequest.unitPrice())
                    .subtotal(subtotal)
                    .build();

            order.addItem(item);

            totalAmount = totalAmount.add(subtotal);
        }

        // -----------------------------------------
        // 3. Set Order Total
        // -----------------------------------------

        order.setTotalAmount(totalAmount);

        // -----------------------------------------
        // 4. Save Order
        // -----------------------------------------

        Order savedOrder = orderRepository.save(order);

        // Stored in the outbox within the same transaction.
        eventPublisher.publishOrderCreated(savedOrder);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getById(UUID id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(this::toResponse).orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByCustomerId(UUID customerId) {
        return orderRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public OrderResponse update(UUID id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setCustomerId(request.customerId());
        order.setTotalAmount(request.totalAmount());
        order.setCurrency(request.currency().toUpperCase());
        return toResponse(orderRepository.save(order));
    }

    @Override
    public void delete(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        orderRepository.delete(order);
    }

    private OrderResponse toResponse(Order order) {

        List<OrderItemResponse> items = order.getItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items);
    }

    private String generateOrderNumber() {
        String date = LocalDate.now().toString().replace("-", "");
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "ORD-" + date + "-" + suffix;
    }

}
