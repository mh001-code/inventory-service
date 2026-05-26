package com.orderprocessing.inventory.service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("Serviço de gestão de estoque. Consome eventos do RabbitMQ " +
                                "(order.created → reserva estoque, order.cancelled → libera estoque) " +
                                "com idempotência garantida via StockMovement lookup por orderId.")
                        .version("1.0.0")
                        .contact(new Contact().name("Order Processing System")))
                .servers(List.of(new Server().url("http://localhost:8081").description("Local")));
    }
}
