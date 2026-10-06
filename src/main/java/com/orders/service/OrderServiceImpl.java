package com.orders.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orders.common.events.PaymentCompletedEvent;
import com.orders.common.events.PaymentFailedEvent;

import com.orders.domain.dto.OrderCreateRequest;
import com.orders.domain.dto.OrderItemRequest;
import com.orders.domain.dto.OrderItemResponse;
import com.orders.domain.dto.OrderResponse;
import com.orders.domain.dto.OrderUpdateRequest;
import com.orders.domain.model.Order;
import com.orders.domain.model.OrderItem;
import com.orders.domain.model.OrderStatus;
import com.orders.exception.OrderNotFoundException;
import com.orders.idempotency.ProcessedEvent;
import com.orders.idempotency.ProcessedEventRepository;
import com.orders.messaging.publisher.EventPublisher;
import com.orders.repository.OrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

        private static final String PAYMENT_COMPLETED_CONSUMER = "PAYMENT_COMPLETED_CONSUMER";
        private static final String PAYMENT_FAILED_CONSUMER = "PAYMENT_FAILED_CONSUMER";

        private final OrderRepository orderRepository;
        private final EventPublisher eventPublisher;
        private final ProcessedEventRepository processedEventRepository;

        // ============================================================
        // CREATE ORDER
        // ============================================================

        @Override
        public OrderResponse create(OrderCreateRequest request) {

                // -----------------------------------------
                // 1.Generate Order Number
                // -----------------------------------------

                String orderNumber = generateOrderNumber();

                while (orderRepository.existsByOrderNumber(orderNumber)) {
                        orderNumber = generateOrderNumber();
                }

                // -----------------------------------------
                // 2. Create Order
                // -----------------------------------------

                Order order = Order.builder()
                                .customerId(request.customerId())
                                .status(OrderStatus.PAYMENT_PENDING)
                                .orderNumber(orderNumber)
                                .totalAmount(BigDecimal.ZERO)
                                .currency(request.currency().toUpperCase())
                                .build();

                // -----------------------------------------
                // 3. Create Order Items
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
                // 4. Set Order Total
                // -----------------------------------------

                order.setTotalAmount(totalAmount);

                // -----------------------------------------
                // 5. Save Order
                // -----------------------------------------

                Order savedOrder = orderRepository.save(order);

                // -----------------------------------------
                // 6. Create OrderCreatedEvent
                // Order + Outbox Event are committed
                // in the same transaction.
                // -----------------------------------------

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

        // ============================================================
        // PAYMENT COMPLETED
        // ============================================================

        @Override
        @Transactional
        public void processPaymentCompleted(PaymentCompletedEvent event) {

                log.info("Processing PaymentCompletedEvent. " + "eventId={}, orderId={}, paymentId={}",
                                event.eventId(), event.orderId(), event.paymentId());

                // -----------------------------------------
                // 1. Idempotency Check
                // -----------------------------------------
                if (processedEventRepository
                                .existsByEventIdAndConsumerName(event.eventId(), PAYMENT_COMPLETED_CONSUMER)) {
                        log.info("PaymentCompletedEvent already processed. "
                                        + "eventId={}, consumer={}", event.eventId(),
                                        PAYMENT_COMPLETED_CONSUMER);
                        return;
                }

                // -----------------------------------------
                // 2. Find Order
                // -----------------------------------------
                Order order = orderRepository.findById(event.orderId())
                                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

                // -----------------------------------------
                // 3. Update Order
                // -----------------------------------------
                order.markPaid();
                orderRepository.save(order);

                // -----------------------------------------
                // 4. Record Processed Event
                // IMPORTANT:
                // Order update + processed event are
                // committed in ONE transaction.
                // -----------------------------------------
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                                .eventId(event.eventId())
                                .consumerName(PAYMENT_COMPLETED_CONSUMER)
                                .processedAt(Instant.now())
                                .build();
                processedEventRepository.save(processedEvent);

                log.info("Order marked as PAID. " + "orderId={}, eventId={}",
                                order.getId(), event.eventId());

        }

        // ============================================================
        // PAYMENT FAILED
        // ============================================================

        @Override
        @Transactional
        public void processPaymentFailed(PaymentFailedEvent event) {

                log.info("Processing PaymentFailedEvent. "
                                + "eventId={}, orderId={}, paymentId={}",
                                event.eventId(), event.orderId(), event.paymentId());

                // -----------------------------------------
                // 1. Idempotency Check
                // -----------------------------------------

                if (processedEventRepository.existsByEventIdAndConsumerName(event.eventId(),
                                PAYMENT_FAILED_CONSUMER)) {
                        log.info("PaymentFailedEvent already processed. "
                                        + "eventId={}, consumer={}", event.eventId(), PAYMENT_FAILED_CONSUMER);
                        return;
                }

                // -----------------------------------------
                // 2. Find Order
                // -----------------------------------------

                Order order = orderRepository
                                .findById(event.orderId())
                                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));

                // -----------------------------------------
                // 3. Update Order
                // -----------------------------------------

                order.markPaymentFailed(event.reason());
                orderRepository.save(order);

                // -----------------------------------------
                // 4. Record Processed Event
                // -----------------------------------------
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                                .eventId(event.eventId())
                                .consumerName(PAYMENT_FAILED_CONSUMER)
                                .build();
                processedEventRepository.save(processedEvent);

                log.warn("Order payment failed. "
                                + "orderId={}, eventId={}, reason={}",
                                order.getId(), event.eventId(), event.reason());
        }

        // ============================================================
        // MAPPING
        // ============================================================
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

        // ============================================================
        // ORDER NUMBER
        // ============================================================

        private String generateOrderNumber() {
                String date = LocalDate.now().toString().replace("-", "");
                String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                return "ORD-" + date + "-" + suffix;
        }

}
