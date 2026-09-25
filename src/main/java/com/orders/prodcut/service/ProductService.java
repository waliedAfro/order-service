package com.orders.prodcut.service;

import java.util.List;
import java.util.UUID;

import com.orders.prodcut.dto.ProductRequest;
import com.orders.prodcut.dto.ProductResponse;

public interface ProductService {

     ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(UUID id);

    ProductResponse getProductBySku(String sku);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getActiveProducts();

    List<ProductResponse> searchProducts(String name);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void activateProduct(UUID id);

    void deactivateProduct(UUID id);

    void deleteProduct(UUID id);
}
