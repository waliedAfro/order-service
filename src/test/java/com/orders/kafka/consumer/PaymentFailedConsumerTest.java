package com.orders.kafka.consumer;

import static org.mockito.Mockito.*;

import com.orders.common.events.PaymentFailedEvent;
import com.orders.service.OrderService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PaymentFailedConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentFailedConsumer consumer;

    @Test
    void consume_shouldDelegatePaymentFailedEvent() throws Exception {
        PaymentFailedEvent event = mock(PaymentFailedEvent.class);

        consumer.consume(event);

        verify(orderService).processPaymentFailed(event);
        verifyNoMoreInteractions(orderService);
    }
}
