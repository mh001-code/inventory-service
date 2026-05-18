CREATE TABLE products (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)  NOT NULL,
    sku             VARCHAR(100)  NOT NULL UNIQUE,
    description     TEXT,
    unit_price      DECIMAL(19,2) NOT NULL,
    stock_quantity  INT           NOT NULL CHECK (stock_quantity >= 0),
    created_at      TIMESTAMP     NOT NULL
);

CREATE INDEX idx_products_sku ON products(sku);
