package com.orders.messaging.publisher;

import com.orders.domain.model.Order;

public interface EventPublisher {

    void publishOrderCreated(Order order);
}
