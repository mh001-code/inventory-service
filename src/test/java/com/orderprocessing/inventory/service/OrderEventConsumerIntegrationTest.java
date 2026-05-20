package com.orderprocessing.inventory.service;

import com.orderprocessing.inventory.service.domain.model.Product;
import com.orderprocessing.inventory.service.domain.model.StockMovementType;
import com.orderprocessing.inventory.service.infrastructure.config.RabbitMQConfig;
import com.orderprocessing.inventory.service.infrastructure.messaging.OrderCancelledEvent;
import com.orderprocessing.inventory.service.infrastructure.messaging.OrderCreatedEvent;
import com.orderprocessing.inventory.service.infrastructure.persistence.ProductJpaRepository;
import com.orderprocessing.inventory.service.infrastructure.persistence.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OrderEventConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private StockMovementJpaRepository stockMovementJpaRepository;

    @BeforeEach
    void setUp() {
        stockMovementJpaRepository.deleteAll();
        productJpaRepository.deleteAll();
    }

    @Test
    void onOrderCreated_shouldReserveStock() {
        Product product = productJpaRepository.save(Product.create("Widget", "WGT-001", null, BigDecimal.TEN, 10));
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Item(product.getId(), "Widget", 3, BigDecimal.TEN)),
                BigDecimal.valueOf(30)
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(7);
            assertThat(stockMovementJpaRepository.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).isTrue();
        });
    }

    @Test
    void onOrderCreated_shouldBeIdempotent_whenEventReceivedTwice() {
        Product product = productJpaRepository.save(Product.create("Widget", "WGT-002", null, BigDecimal.TEN, 10));
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Item(product.getId(), "Widget", 3, BigDecimal.TEN)),
                BigDecimal.valueOf(30)
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(7);
            assertThat(stockMovementJpaRepository.findByOrderIdAndType(orderId, StockMovementType.RESERVE)).hasSize(1);
        });
    }

    @Test
    void onOrderCancelled_shouldReleaseStock() {
        Product product = productJpaRepository.save(Product.create("Widget", "WGT-003", null, BigDecimal.TEN, 10));
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent createEvent = new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Item(product.getId(), "Widget", 3, BigDecimal.TEN)),
                BigDecimal.valueOf(30)
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, createEvent);

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(stockMovementJpaRepository.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).isTrue()
        );

        OrderCancelledEvent cancelEvent = new OrderCancelledEvent(orderId, UUID.randomUUID(), "customer request");
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, cancelEvent);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(10);
            assertThat(stockMovementJpaRepository.existsByOrderIdAndType(orderId, StockMovementType.RELEASE)).isTrue();
        });
    }

    @Test
    void onOrderCancelled_shouldBeIdempotent_whenEventReceivedTwice() {
        Product product = productJpaRepository.save(Product.create("Widget", "WGT-004", null, BigDecimal.TEN, 10));
        UUID orderId = UUID.randomUUID();

        OrderCreatedEvent createEvent = new OrderCreatedEvent(
                orderId,
                UUID.randomUUID(),
                List.of(new OrderCreatedEvent.Item(product.getId(), "Widget", 3, BigDecimal.TEN)),
                BigDecimal.valueOf(30)
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, createEvent);

        await().atMost(5, SECONDS).untilAsserted(() ->
                assertThat(stockMovementJpaRepository.existsByOrderIdAndType(orderId, StockMovementType.RESERVE)).isTrue()
        );

        OrderCancelledEvent cancelEvent = new OrderCancelledEvent(orderId, UUID.randomUUID(), "customer request");
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, cancelEvent);
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, cancelEvent);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            Product updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(10);
            assertThat(stockMovementJpaRepository.findByOrderIdAndType(orderId, StockMovementType.RELEASE)).hasSize(1);
        });
    }
}
