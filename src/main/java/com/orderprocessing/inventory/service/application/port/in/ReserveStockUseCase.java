package com.orderprocessing.inventory.service.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ReserveStockUseCase {

    record Item(UUID productId, int quantity) {}

    void execute(UUID orderId, List<Item> items);
}
