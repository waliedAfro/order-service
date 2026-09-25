package com.orders.prodcut.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orders.common.exception.DuplicateProductException;
import com.orders.common.exception.ProductNotFoundException;
import com.orders.prodcut.dto.ProductRequest;
import com.orders.prodcut.dto.ProductResponse;
import com.orders.prodcut.model.Product;
import com.orders.prodcut.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // =========================================================
    // CREATE
    // =========================================================
    @Transactional
    @Override
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateProductException(request.getSku());
        }

        Product product = new Product();

        mapRequestToEntity(request, product);

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    // =========================================================
    // GET BY ID
    // =========================================================
    @Override
    public ProductResponse getProductById(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return mapToResponse(product);
    }

    // =========================================================
    // GET BY SKU
    // =========================================================
    @Override
    public ProductResponse getProductBySku(String sku) {

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));

        return mapToResponse(product);
    }

    // =========================================================
    // GET ALL
    // =========================================================
    @Override
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET ACTIVE PRODUCTS
    // =========================================================
    @Override
    public List<ProductResponse> getActiveProducts() {

        return productRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // SEARCH BY NAME
    // =========================================================
    @Override
    public List<ProductResponse> searchProducts(String name) {

        return productRepository
                .findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================
    @Transactional
    @Override
    public ProductResponse updateProduct(UUID id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        /*
         * SKU is unique.
         *
         * If the request contains a different SKU,
         * make sure it does not already belong to another product.
         */
        if (!product.getSku().equals(request.getSku())
                && productRepository.existsBySku(request.getSku())) {

            throw new DuplicateProductException(request.getSku());
        }

        mapRequestToEntity(request, product);

        Product updatedProduct = productRepository.save(product);

        return mapToResponse(updatedProduct);
    }

    // =========================================================
    // ACTIVATE
    // =========================================================
    @Transactional
    @Override
    public void activateProduct(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(true);

        productRepository.save(product);
    }

    // =========================================================
    // DEACTIVATE
    // =========================================================
    @Transactional
    @Override
    public void deactivateProduct(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setActive(false);

        productRepository.save(product);
    }

    // =========================================================
    // DELETE
    // =========================================================
    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
    }

    // =========================================================
    // MAPPING
    // =========================================================

    private void mapRequestToEntity(
            ProductRequest request,
            Product product) {

        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setCurrency(request.getCurrency());
        product.setStockQuantity(request.getStockQuantity());
        product.setActive(request.getActive());
    }

    private ProductResponse mapToResponse(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId());
        response.setSku(product.getSku());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setPrice(product.getPrice());
        response.setCurrency(product.getCurrency());
        response.setStockQuantity(product.getStockQuantity());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        return response;
    }

}
