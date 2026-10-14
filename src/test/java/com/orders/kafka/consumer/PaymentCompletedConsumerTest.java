package com.orders.kafka.consumer;

import static org.mockito.Mockito.*;

import com.orders.common.events.PaymentCompletedEvent;
import com.orders.service.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentCompletedConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentCompletedConsumer consumer;

    @Test
    void consume_shouldDelegatePaymentCompletedEvent() throws Exception {
        PaymentCompletedEvent event = mock(PaymentCompletedEvent.class);

        consumer.consume(event);

        verify(orderService).processPaymentCompleted(event);
        verifyNoMoreInteractions(orderService);
    }
}
