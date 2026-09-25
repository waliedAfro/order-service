package com.orders.common.exception;

public class DuplicateProductException  extends RuntimeException {

    public DuplicateProductException(String sku) {
        super("Product already exists with SKU: " + sku);
    }
}
