package com.orders.kafka.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.orders.common.events.PaymentFailedEvent;
import com.orders.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFailedConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "${spring.kafka.topics.payment-failed}",groupId = "order-service",
            containerFactory ="paymentFailedKafkaListenerContainerFactory")
    public void consume(PaymentFailedEvent event) throws Exception {

        //PaymentFailedEvent event = objectMapper.readValue(payload,PaymentFailedEvent.class);

        log.info("Received PaymentFailedEvent. " +
                "eventId={}, orderId={}, paymentId={}",event.eventId(),event.orderId(),event.paymentId());

        orderService.processPaymentFailed(event);
    }
}
