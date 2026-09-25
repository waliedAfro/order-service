package com.orders.common.exception;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderNumber) {
        super("Order number already exists: " + orderNumber);
    }
}
