package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.ReleaseStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReleaseStockService implements ReleaseStockUseCase {

    private final ProductPersistencePort productPersistencePort;
    private final StockMovementPersistencePort stockMovementPersistencePort;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public void execute(UUID orderId) {
        if (stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RELEASE)) {
            log.info("Skipping duplicate order.cancelled: orderId={}", orderId);
            Counter.builder("inventory.stock.released.total")
                    .tag("result", "duplicate_skipped")
                    .register(meterRegistry)
                    .increment();
            return;
        }

        List<StockMovement> reservations = stockMovementPersistencePort
                .findByOrderIdAndType(orderId, StockMovementType.RESERVE);

        for (StockMovement reservation : reservations) {
            reservation.getProduct().release(reservation.getQuantity());
            productPersistencePort.save(reservation.getProduct());

            stockMovementPersistencePort.save(
                    StockMovement.of(reservation.getProduct(), orderId, reservation.getQuantity(), StockMovementType.RELEASE)
            );
        }

        Counter.builder("inventory.stock.released.total")
                .tag("result", "success")
                .register(meterRegistry)
                .increment();
    }
}
