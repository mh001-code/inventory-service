package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.ReleaseStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseStockService implements ReleaseStockUseCase {

    private final ProductPersistencePort productPersistencePort;
    private final StockMovementPersistencePort stockMovementPersistencePort;

    @Override
    @Transactional
    public void execute(UUID orderId) {
        List<StockMovement> reservations = stockMovementPersistencePort
                .findByOrderIdAndType(orderId, StockMovementType.RESERVE);

        for (StockMovement reservation : reservations) {
            reservation.getProduct().release(reservation.getQuantity());
            productPersistencePort.save(reservation.getProduct());

            stockMovementPersistencePort.save(
                    StockMovement.of(reservation.getProduct(), orderId, reservation.getQuantity(), StockMovementType.RELEASE)
            );
        }
    }
}
