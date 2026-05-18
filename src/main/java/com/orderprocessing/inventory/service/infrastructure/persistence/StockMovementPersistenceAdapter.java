package com.orderprocessing.inventory.service.infrastructure.persistence;

import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StockMovementPersistenceAdapter implements StockMovementPersistencePort {

    private final StockMovementJpaRepository repository;

    @Override
    public StockMovement save(StockMovement movement) {
        return repository.save(movement);
    }

    @Override
    public boolean existsByOrderIdAndType(UUID orderId, StockMovementType type) {
        return repository.existsByOrderIdAndType(orderId, type);
    }
}
