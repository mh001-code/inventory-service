package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.ReserveStockUseCase;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.InsufficientStockException;
import com.orderprocessing.inventory.service.domain.exception.ProductNotFoundException;
import com.orderprocessing.inventory.service.domain.model.Product;
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
public class ReserveStockService implements ReserveStockUseCase {

    private final ProductPersistencePort productPersistencePort;
    private final StockMovementPersistencePort stockMovementPersistencePort;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public void execute(UUID orderId, List<Item> items) {
        if (stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)) {
            log.info("Skipping duplicate order.created: orderId={}", orderId);
            Counter.builder("inventory.stock.reserved.total")
                    .tag("result", "duplicate_skipped")
                    .register(meterRegistry)
                    .increment();
            return;
        }

        for (Item item : items) {
            Product product = productPersistencePort.findById(item.productId())
                    .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            if (!product.hasStock(item.quantity())) {
                Counter.builder("inventory.stock.reserved.total")
                        .tag("result", "insufficient_stock")
                        .register(meterRegistry)
                        .increment();
                throw new InsufficientStockException(product.getSku(), item.quantity(), product.getStockQuantity());
            }

            product.reserve(item.quantity());
            productPersistencePort.save(product);

            stockMovementPersistencePort.save(
                    StockMovement.of(product, orderId, item.quantity(), StockMovementType.RESERVE)
            );
        }

        Counter.builder("inventory.stock.reserved.total")
                .tag("result", "success")
                .register(meterRegistry)
                .increment();
    }
}
