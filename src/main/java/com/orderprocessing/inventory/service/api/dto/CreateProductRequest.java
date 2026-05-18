package com.orderprocessing.inventory.service.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String sku,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        @Min(0) int initialStock
) {}
