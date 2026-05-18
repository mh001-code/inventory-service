package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.UpdateStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.ProductNotFoundException;
import com.orderprocessing.inventory.service.domain.model.Product;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateStockService implements UpdateStockUseCase {

    private final ProductPersistencePort productPersistencePort;
    private final StockMovementPersistencePort stockMovementPersistencePort;

    @Override
    public Product execute(UUID id, int newQuantity) {
        Product product = productPersistencePort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        int diff = newQuantity - product.getStockQuantity();
        product.adjustStock(newQuantity);
        Product saved = productPersistencePort.save(product);

        stockMovementPersistencePort.save(
                StockMovement.of(product, null, diff, StockMovementType.ADJUSTMENT));

        return saved;
    }
}
