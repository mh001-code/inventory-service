package com.orderprocessing.inventory.service.application.port.in;

import com.orderprocessing.inventory.service.domain.model.Product;

import java.util.UUID;

public interface UpdateStockUseCase {
    Product execute(UUID id, int newQuantity);
}
