package com.orders.exception;

public class DuplicateOrderException extends RuntimeException {

    public DuplicateOrderException(String orderNumber) {
        super("Order number already exists: " + orderNumber);
    }
}
