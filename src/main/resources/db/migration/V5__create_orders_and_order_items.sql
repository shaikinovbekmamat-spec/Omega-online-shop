-- Таблица заказов
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'NEW',
    phone           VARCHAR(20) NOT NULL,
    delivery_address VARCHAR(500) NOT NULL,
    comment         TEXT,
    total_amount    DECIMAL(10, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT
);

-- Таблица позиций заказа
CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT       NOT NULL,
    product_id   BIGINT       NOT NULL,
    quantity     INTEGER      NOT NULL CHECK (quantity >= 1),
    price        DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    total_price  DECIMAL(10, 2) NOT NULL CHECK (total_price >= 0),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

-- Индексы
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- Комментарии
COMMENT ON TABLE orders IS 'Заказы пользователей';
COMMENT ON TABLE order_items IS 'Товары в заказах';


