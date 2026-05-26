package com.orderprocessing.inventory.service.api.controller;

import com.orderprocessing.inventory.service.api.dto.CreateProductRequest;
import com.orderprocessing.inventory.service.api.dto.ProductResponse;
import com.orderprocessing.inventory.service.api.dto.UpdateStockRequest;
import com.orderprocessing.inventory.service.application.port.in.CreateProductUseCase;
import com.orderprocessing.inventory.service.application.port.in.UpdateStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Products", description = "Gestão de produtos e estoque")
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final ProductPersistencePort productPersistencePort;

    @Operation(summary = "Criar produto", description = "Cadastra um novo produto com estoque inicial")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Produto criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "SKU já cadastrado")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponse.from(
                createProductUseCase.execute(
                        request.name(), request.sku(), request.description(),
                        request.unitPrice(), request.initialStock())));
    }

    @Operation(summary = "Buscar produto por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Produto encontrado"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return productPersistencePort.findById(id)
                .map(p -> ResponseEntity.ok(ProductResponse.from(p)))
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Operation(summary = "Listar todos os produtos")
    @ApiResponse(responseCode = "200", description = "Lista de produtos com estoque atual")
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getProducts() {
        List<ProductResponse> products = productPersistencePort.findAll().stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Atualizar estoque", description = "Ajuste manual de estoque (reabastecimento)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estoque atualizado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Produto não encontrado")
    })
    @PutMapping("/{id}/stock")
    public ResponseEntity<ProductResponse> updateStock(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateStockRequest request) {
        return ResponseEntity.ok(ProductResponse.from(updateStockUseCase.execute(id, request.quantity())));
    }
}
