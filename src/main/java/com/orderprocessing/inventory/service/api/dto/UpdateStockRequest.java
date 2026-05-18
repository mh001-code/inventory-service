package com.orderprocessing.inventory.service.api.dto;

import jakarta.validation.constraints.Min;

public record UpdateStockRequest(
        @Min(0) int quantity
) {}
