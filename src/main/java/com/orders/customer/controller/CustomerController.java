package com.orders.customer.controller;

import com.orders.customer.dto.CustomerRequest;
import com.orders.customer.dto.CustomerResponse;
import com.orders.customer.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor 
public class CustomerController {

    private final CustomerService customerService;

    // =========================================================
    // CREATE CUSTOMER
    // =========================================================

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================
    // GET ALL CUSTOMERS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {

        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // =========================================================
    // GET CUSTOMER BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable UUID id) {

        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    // =========================================================
    // GET CUSTOMER BY CUSTOMER NUMBER
    // =========================================================

    @GetMapping("/number/{customerNumber}")
    public ResponseEntity<CustomerResponse> getCustomerByCustomerNumber(@PathVariable String customerNumber) {

        return ResponseEntity.ok(customerService.getCustomerByCustomerNumber(customerNumber)
        );
    }

    // =========================================================
    // GET CUSTOMER BY EMAIL
    // =========================================================

    @GetMapping("/email")
    public ResponseEntity<CustomerResponse> getCustomerByEmail(@RequestParam String email) {

        return ResponseEntity.ok(customerService.getCustomerByEmail(email));
    }

    // =========================================================
    // GET ACTIVE CUSTOMERS
    // =========================================================

    @GetMapping("/active")
    public ResponseEntity<List<CustomerResponse>> getActiveCustomers() {

        return ResponseEntity.ok(customerService.getActiveCustomers());
    }

    // =========================================================
    // SEARCH CUSTOMERS
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(@RequestParam String name) {

        return ResponseEntity.ok(customerService.searchCustomers(name));
    }

    // =========================================================
    // UPDATE CUSTOMER
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {

        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    // =========================================================
    // ACTIVATE CUSTOMER
    // =========================================================

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateCustomer(@PathVariable UUID id) {

        customerService.activateCustomer(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // DEACTIVATE CUSTOMER
    // =========================================================

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable UUID id) {

        customerService.deactivateCustomer(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // DELETE CUSTOMER
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable UUID id) {

        customerService.deleteCustomer(id);

        return ResponseEntity.noContent().build();
    }
}

