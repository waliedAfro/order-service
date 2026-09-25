package com.orders.common.events;

import com.orders.order.model.Order;

public interface EventPublisher {

    void publishOrderCreated(Order order);
}
