package com.orderprocessing.inventory.service.application.port.in;

import java.util.UUID;

public interface ReleaseStockUseCase {
    void execute(UUID orderId);
}
