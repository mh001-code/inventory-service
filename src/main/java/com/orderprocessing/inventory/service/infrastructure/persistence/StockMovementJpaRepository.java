package com.orderprocessing.inventory.service.infrastructure.persistence;

import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementJpaRepository extends JpaRepository<StockMovement, UUID> {
    boolean existsByOrderIdAndType(UUID orderId, StockMovementType type);
}
