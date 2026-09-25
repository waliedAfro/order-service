package com.orders.inventory.repository;

import com.orders.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    /**
     * Find inventory by product ID.
     */
    Optional<Inventory> findByProductId(UUID productId);

    /**
     * Check whether inventory exists for a product.
     */
    boolean existsByProductId(UUID productId);

    /**
     * Find inventory records where stock is available.
     */
    List<Inventory> findByAvailableQuantityGreaterThan(Integer quantity);

    /**
     * Find inventory records where stock is below or equal
     * to the specified quantity.
     */
    List<Inventory> findByAvailableQuantityLessThanEqual(Integer quantity);

    /**
     * Find inventory records where reserved stock exists.
     */
    List<Inventory> findByReservedQuantityGreaterThan(Integer quantity);
}

