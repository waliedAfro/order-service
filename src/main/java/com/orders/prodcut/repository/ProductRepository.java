package com.orders.prodcut.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.orders.prodcut.model.Product;

@Repository 
public interface ProductRepository extends JpaRepository<Product,UUID>{

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    List<Product> findByActiveTrue();

    List<Product> findByActive(Boolean active);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String name);

}
