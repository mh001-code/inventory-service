package com.orderprocessing.inventory.service.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static Product create(String name, String sku, String description, BigDecimal unitPrice, int initialStock) {
        Product product = new Product();
        product.name = name;
        product.sku = sku;
        product.description = description;
        product.unitPrice = unitPrice;
        product.stockQuantity = initialStock;
        return product;
    }

    public void reserve(int quantity) {
        this.stockQuantity -= quantity;
    }

    public void release(int quantity) {
        this.stockQuantity += quantity;
    }

    public void adjustStock(int newQuantity) {
        this.stockQuantity = newQuantity;
    }

    public boolean hasStock(int quantity) {
        return this.stockQuantity >= quantity;
    }
}
