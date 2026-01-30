-- Создание таблицы категорий
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы товаров
CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
                          quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
                          image_path VARCHAR(500),
                          specifications TEXT,
                          category_id BIGINT NOT NULL,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

-- Индексы для оптимизации
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_is_active ON products(is_active);

-- Комментарии
COMMENT ON TABLE categories IS 'Категории товаров';
COMMENT ON TABLE products IS 'Товары магазина';
COMMENT ON COLUMN products.specifications IS 'Характеристики товара в JSON формате';
COMMENT ON COLUMN products.is_active IS 'Активен ли товар (показывается в каталоге)';