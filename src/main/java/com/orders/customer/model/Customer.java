package com.orders.customer.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

@Entity
@Table(
    name = "customers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_customers_customer_number",
            columnNames = "customer_number"
        ),
        @UniqueConstraint(
            name = "uk_customers_email",
            columnNames = "email"
        )
    },
    indexes = {
        @Index(
            name = "idx_customers_first_name",
            columnList = "first_name"
        ),
        @Index(
            name = "idx_customers_last_name",
            columnList = "last_name"
        ),
        @Index(
            name = "idx_customers_active",
            columnList = "active"
        ),
        @Index(
            name = "idx_customers_created_at",
            columnList = "created_at"
        )
    }
)
@Setter 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
@Builder 
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 50)
    @Column(
        name = "customer_number",nullable = false,unique = true,length = 50)
    private String customerNumber;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name",nullable = false,length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name",nullable = false,length = 100)
    private String lastName;

    @NotBlank
    @Email
    @Size(max = 255)
    @Column(name = "email",nullable = false,unique = true,length = 255)
    private String email;

    @Size(max = 30)
    @Column(name = "phone",length = 30)
    private String phone;

    @Size(max = 500)
    @Column(name = "address",length = 500)
    private String address;

    @Size(max = 100)
    @Column(name = "city",length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "country",length = 100)
    private String country;

    @Default 
    @NotNull
    @Column(name = "active",nullable = false)
    private Boolean active = true;

    @Column(name = "created_at",nullable = false,updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at",nullable = false)
    private OffsetDateTime updatedAt;


    @PrePersist
    protected void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt = OffsetDateTime.now();
    }
}
