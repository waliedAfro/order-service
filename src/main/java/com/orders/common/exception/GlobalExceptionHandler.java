package com.orders.common.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiError> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateOrderException.class)
    public ResponseEntity<ApiError> handleDuplicateOrder(DuplicateOrderException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_ORDER",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateProductException.class)
    public ResponseEntity<ApiError> handleDuplicateProduct(DuplicateProductException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT",
                ex.getMessage(), request.getRequestURI());

    }
    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<ApiError> handleDuplicateCustomer(DuplicateCustomerException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_CUSTOMER",
                ex.getMessage(), request.getRequestURI());

    }

    @ExceptionHandler(DuplicateInventoryException.class)
    public ResponseEntity<ApiError> handleDuplicateCustomer(DuplicateInventoryException ex,
            HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT_IN_INVENTORY",
                ex.getMessage(), request.getRequestURI());

    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND",
                ex.getMessage(), request.getRequestURI());

    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiError> handleCustomerNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND",
                ex.getMessage(), request.getRequestURI());

    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ApiError> handleInventoryNotFound(
            ProductNotFoundException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "PRODUCT_IN_INVENTORY_NOT_FOUND",
                ex.getMessage(), request.getRequestURI());

    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message, request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)

    public ResponseEntity<ApiError> handleGeneralException(Exception ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR", "An unexpected error occurred",
                request.getRequestURI());
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status,
            String code, String message, String path) {
        ApiError error = new ApiError(LocalDateTime.now(), status.value(),
                code, message, path);
        return ResponseEntity.status(status).body(error);
    }

}
