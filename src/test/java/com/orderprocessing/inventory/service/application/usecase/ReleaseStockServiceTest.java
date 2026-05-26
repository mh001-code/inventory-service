package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.model.Product;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReleaseStockServiceTest {

    @Mock
    private ProductPersistencePort productPersistencePort;

    @Mock
    private StockMovementPersistencePort stockMovementPersistencePort;

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ReleaseStockService releaseStockService;

    private UUID orderId;
    private Product product;

    @BeforeEach
    void setUp() {
        releaseStockService = new ReleaseStockService(productPersistencePort, stockMovementPersistencePort, meterRegistry);
        orderId = UUID.randomUUID();
        product = Product.create("Widget", "WGT-001", null, BigDecimal.TEN, 7);
    }

    @Test
    void execute_shouldReleaseStock_whenReservationsExist() {
        StockMovement reservation = StockMovement.of(product, orderId, 3, StockMovementType.RESERVE);

        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RELEASE)).thenReturn(false);
        when(stockMovementPersistencePort.findByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(List.of(reservation));
        when(stockMovementPersistencePort.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        releaseStockService.execute(orderId);

        verify(productPersistencePort).save(product);
        verify(stockMovementPersistencePort).save(any(StockMovement.class));
        assert product.getStockQuantity() == 10;
    }

    @Test
    void execute_shouldSkip_whenAlreadyProcessed() {
        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RELEASE)).thenReturn(true);

        releaseStockService.execute(orderId);

        verify(stockMovementPersistencePort, never()).findByOrderIdAndType(any(), any());
        verify(productPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldDoNothing_whenNoReservationsFound() {
        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RELEASE)).thenReturn(false);
        when(stockMovementPersistencePort.findByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(List.of());

        releaseStockService.execute(orderId);

        verify(productPersistencePort, never()).save(any());
        verify(stockMovementPersistencePort, never()).save(any());
    }
}
