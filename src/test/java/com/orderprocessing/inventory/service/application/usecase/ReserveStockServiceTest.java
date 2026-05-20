package com.orderprocessing.inventory.service.application.usecase;

import com.orderprocessing.inventory.service.application.port.in.ReserveStockUseCase.Item;
import com.orderprocessing.inventory.service.application.port.out.ProductPersistencePort;
import com.orderprocessing.inventory.service.application.port.out.StockMovementPersistencePort;
import com.orderprocessing.inventory.service.domain.exception.InsufficientStockException;
import com.orderprocessing.inventory.service.domain.exception.ProductNotFoundException;
import com.orderprocessing.inventory.service.domain.model.Product;
import com.orderprocessing.inventory.service.domain.model.StockMovement;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveStockServiceTest {

    @Mock
    private ProductPersistencePort productPersistencePort;

    @Mock
    private StockMovementPersistencePort stockMovementPersistencePort;

    @InjectMocks
    private ReserveStockService reserveStockService;

    private UUID orderId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = Product.create("Widget", "WGT-001", null, BigDecimal.TEN, 10);
    }

    @Test
    void execute_shouldReserveStock_whenSufficientStock() {
        List<Item> items = List.of(new Item(productId, 3));

        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(false);
        when(productPersistencePort.findById(productId)).thenReturn(Optional.of(product));
        when(stockMovementPersistencePort.save(any(StockMovement.class))).thenAnswer(i -> i.getArgument(0));

        reserveStockService.execute(orderId, items);

        verify(productPersistencePort).save(product);
        verify(stockMovementPersistencePort).save(any(StockMovement.class));
        assert product.getStockQuantity() == 7;
    }

    @Test
    void execute_shouldSkip_whenAlreadyProcessed() {
        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(true);

        reserveStockService.execute(orderId, List.of(new Item(productId, 3)));

        verify(productPersistencePort, never()).findById(any());
        verify(stockMovementPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldThrowProductNotFoundException_whenProductNotFound() {
        List<Item> items = List.of(new Item(productId, 3));

        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(false);
        when(productPersistencePort.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reserveStockService.execute(orderId, items))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productPersistencePort, never()).save(any());
        verify(stockMovementPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldThrowInsufficientStockException_whenNotEnoughStock() {
        List<Item> items = List.of(new Item(productId, 15));

        when(stockMovementPersistencePort.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).thenReturn(false);
        when(productPersistencePort.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> reserveStockService.execute(orderId, items))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("WGT-001");

        verify(productPersistencePort, never()).save(any());
        verify(stockMovementPersistencePort, never()).save(any());
    }
}
