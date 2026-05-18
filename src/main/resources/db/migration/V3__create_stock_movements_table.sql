CREATE TABLE stock_movements (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL REFERENCES products(id),
    order_id    UUID,
    quantity    INT         NOT NULL,
    type        VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_stock_movements_product_id ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_order_id ON stock_movements(order_id);
