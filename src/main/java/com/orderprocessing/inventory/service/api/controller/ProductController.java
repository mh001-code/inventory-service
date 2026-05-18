package com.orderprocessing.inventory.service.api.controller;

import com.orderprocessing.inventory.service.api.dto.CreateProductRequest;
import com.orderprocessing.inventory.service.api.dto.ProductResponse;
import com.orderprocessing.inventory.service.api.dto.UpdateStockRequest;
import com.orderprocessing.inventory.service.application.port.in.CreateProductUseCase;
import com.orderprocessing.inventory.service.application.port.in.UpdateStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.ProductNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final ProductPersistencePort productPersistencePort;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(
                createProductUseCase.execute(
                        request.name(), request.sku(), request.description(),
                        request.unitPrice(), request.initialStock())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return productPersistencePort.findById(id)
                .map(p -> ResponseEntity.ok(ProductResponse.from(p)))
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productPersistencePort.findAll().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PutMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStockRequest request) {
        return ResponseEntity.ok(ProductResponse.from(updateStockUseCase.execute(id, request.quantity())));
    }
}
