package com.orderprocessing.inventory.service;

import com.orderprocessing.inventory.service.infrastructure.persistence.ProductJpaRepository;
import com.orderprocessing.inventory.service.infrastructure.persistence.StockMovementJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
    void createProduct_shouldReturn201() {
        var request = Map.of(
                "name", "Widget",
                "sku", "WGT-001",
                "description", "A test widget",
                "unitPrice", BigDecimal.valueOf(9.99),
                "initialStock", 10
        );

        ResponseEntity<Map> response = restTemplate.postForEntity("/products", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("sku")).isEqualTo("WGT-001");
        assertThat(response.getBody().get("stockQuantity")).isEqualTo(10);
    }

    @Test
    void createProduct_shouldReturn409_whenSkuAlreadyExists() {
        var request = Map.of(
                "name", "Widget",
                "sku", "WGT-DUP",
                "unitPrice", BigDecimal.valueOf(9.99),
                "initialStock", 5
        );
        restTemplate.postForEntity("/products", request, Map.class);

        ResponseEntity<Map> response = restTemplate.postForEntity("/products", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void getProduct_shouldReturn200() {
        var request = Map.of(
                "name", "Gadget",
                "sku", "GDG-001",
                "unitPrice", BigDecimal.valueOf(19.99),
                "initialStock", 5
        );
        ResponseEntity<Map> created = restTemplate.postForEntity("/products", request, Map.class);
        String id = (String) created.getBody().get("id");

        ResponseEntity<Map> response = restTemplate.getForEntity("/products/" + id, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("sku")).isEqualTo("GDG-001");
    }

    @Test
    void getProduct_shouldReturn404_whenNotFound() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/products/" + UUID.randomUUID(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateStock_shouldReturn200_withNewQuantity() {
        var createRequest = Map.of(
                "name", "Bolt",
                "sku", "BLT-001",
                "unitPrice", BigDecimal.valueOf(1.50),
                "initialStock", 100
        );
        ResponseEntity<Map> created = restTemplate.postForEntity("/products", createRequest, Map.class);
        String id = (String) created.getBody().get("id");

        var updateRequest = Map.of("quantity", 50);
        restTemplate.put("/products/" + id + "/stock", updateRequest);

        ResponseEntity<Map> response = restTemplate.getForEntity("/products/" + id, Map.class);
        assertThat(response.getBody().get("stockQuantity")).isEqualTo(50);
    }
}
