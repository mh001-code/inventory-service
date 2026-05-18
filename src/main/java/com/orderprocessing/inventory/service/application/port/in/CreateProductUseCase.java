package com.orderprocessing.inventory.service.application.port.in;

import com.orderprocessing.inventory.service.domain.model.Product;

import java.math.BigDecimal;

public interface CreateProductUseCase {
    Product execute(String name, String sku, String description, BigDecimal unitPrice, int initialStock);
}
