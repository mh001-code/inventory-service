package com.orderprocessing.inventory.service.infrastructure.messaging;

import com.orderprocessing.inventory.service.application.port.in.ReleaseStockUseCase;
import com.orderprocessing.inventory.service.application.port.in.ReserveStockUseCase;
import com.orderprocessing.inventory.service.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created: orderId={}", event.orderId());

        List<ReserveStockUseCase.Item> items = event.items().stream()
                .map(i -> new ReserveStockUseCase.Item(i.productId(), i.quantity()))
                .toList();

        reserveStockUseCase.execute(event.orderId(), items);
        log.info("Stock reserved for orderId={}", event.orderId());
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_CANCELLED_QUEUE)
    public void onOrderCancelled(OrderCancelledEvent event) {
        log.info("Received order.cancelled: orderId={}", event.orderId());

        releaseStockUseCase.execute(event.orderId());
        log.info("Stock released for orderId={}", event.orderId());
    }
}
