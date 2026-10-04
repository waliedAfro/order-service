package com.orders.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orders.domain.model.Order;
import com.orders.domain.model.OrderStatus;

@Repository 
public interface OrderRepository extends  JpaRepository<Order,UUID>{

     Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumber(String orderNumber);

    List<Order> findByCustomerId(UUID customerId);

    List<Order> findByStatus(OrderStatus status);

}
