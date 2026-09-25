package com.orders.prodcut.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orders.prodcut.dto.ProductRequest;
import com.orders.prodcut.dto.ProductResponse;
import com.orders.prodcut.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor 
public class ProductController {

    private final ProductService productService ;

     // =========================================================
    // CREATE PRODUCT
    // =========================================================

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET PRODUCT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable UUID id) {

        ProductResponse response = productService.getProductById(id);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET PRODUCT BY SKU
    // =========================================================

    @GetMapping("/sku/{sku}")
    public ResponseEntity<ProductResponse> getProductBySku(
            @PathVariable String sku) {

        ProductResponse response = productService.getProductBySku(sku);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET ALL PRODUCTS
    // =========================================================

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        return ResponseEntity.ok(productService.getAllProducts());
    }

    // =========================================================
    // GET ACTIVE PRODUCTS
    // =========================================================

    @GetMapping("/active")
    public ResponseEntity<List<ProductResponse>> getActiveProducts() {

        return ResponseEntity.ok(productService.getActiveProducts());
    }

    // =========================================================
    // SEARCH PRODUCTS
    // =========================================================

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam String name) {

        return ResponseEntity.ok(productService.searchProducts(name));
    }

    // =========================================================
    // UPDATE PRODUCT
    // =========================================================

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.updateProduct(id, request);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // ACTIVATE PRODUCT
    // =========================================================

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateProduct(
            @PathVariable UUID id) {

        productService.activateProduct(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // DEACTIVATE PRODUCT
    // =========================================================

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateProduct(
            @PathVariable UUID id) {

        productService.deactivateProduct(id);

        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // DELETE PRODUCT
    // =========================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID id) {

        productService.deleteProduct(id);

        return ResponseEntity.noContent().build();
    }

}
