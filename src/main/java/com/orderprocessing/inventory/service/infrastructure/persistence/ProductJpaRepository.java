package com.orderprocessing.inventory.service.infrastructure.persistence;

import com.orderprocessing.inventory.service.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(String sku);
}
