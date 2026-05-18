package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.CreateProductUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.ProductAlreadyExistsException;
import com.orderprocessing.inventory.service.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateProductService implements CreateProductUseCase {

    private final ProductPersistencePort persistencePort;

    @Override
    public Product execute(String name, String sku, String description, BigDecimal unitPrice, int initialStock) {
        if (persistencePort.findBySku(sku).isPresent()) {
            throw new ProductAlreadyExistsException(sku);
        }
        return persistencePort.save(Product.create(name, sku, description, unitPrice, initialStock));
    }
}
