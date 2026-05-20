package com.orderprocessing.inventory.service.application.port.out;

import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;

import java.util.List;
import java.util.UUID;

public interface StockMovementPersistencePort {
    StockMovement save(StockMovement movement);
    boolean existsByOrderIdAndType(UUID orderId, StockMovementType type);
    List<StockMovement> findByOrderIdAndType(UUID orderId, StockMovementType type);
}
