package com.orders.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.orders.common.events.PaymentCompletedEvent;
import com.orders.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCompletedConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${kafka.topics.payment-completed}",groupId = "order-service",
            containerFactory ="paymentCompletedKafkaListenerContainerFactory")
    public void consume(PaymentCompletedEvent event)throws Exception {

       // PaymentCompletedEvent event = objectMapper.readValue(payload, PaymentCompletedEvent.class);

        log.info("Received PaymentCompletedEvent. " +
                "eventId={}, orderId={}, paymentId={}",
                event.eventId(),
                event.orderId(),
                event.paymentId());

        orderService.processPaymentCompleted(event);
    }
}
