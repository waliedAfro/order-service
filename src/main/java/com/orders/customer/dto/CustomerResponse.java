package com.orders.customer.dto;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor 
@RequiredArgsConstructor 
@Builder 
public class CustomerResponse {

    private UUID id;

    private String customerNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String address;

    private String city;

    private String country;

    private Boolean active;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

}


