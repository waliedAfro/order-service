package com.orders.exception;

public class InvalidOrderStateException  extends RuntimeException {

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
