package com.orders.customer.service;

import com.orders.customer.dto.CustomerRequest;
import com.orders.customer.dto.CustomerResponse;
import java.util.List;
import java.util.UUID;

public interface CustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(UUID id);

    CustomerResponse getCustomerByCustomerNumber(String customerNumber);

    CustomerResponse getCustomerByEmail(String email);

    List<CustomerResponse> getAllCustomers();

    List<CustomerResponse> getActiveCustomers();

    List<CustomerResponse> searchCustomers(String name);

    CustomerResponse updateCustomer(UUID id,CustomerRequest request);

    void activateCustomer(UUID id);

    void deactivateCustomer(UUID id);

    void deleteCustomer(UUID id);
}

