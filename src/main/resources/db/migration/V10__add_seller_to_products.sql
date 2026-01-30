-- Добавление поля seller_id в таблицу products
-- Это поле связывает товар с продавцом (пользователем с ролью SELLER)

-- Добавляем колонку seller_id (может быть NULL, так как старые товары могут не иметь продавца)
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS seller_id BIGINT;

-- Добавляем внешний ключ
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_products_seller' 
        AND table_name = 'products'
    ) THEN
        ALTER TABLE products
        ADD CONSTRAINT fk_products_seller
        FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Добавляем индекс для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_products_seller ON products(seller_id);

-- Комментарий
COMMENT ON COLUMN products.seller_id IS 'Продавец, который добавил товар (пользователь с ролью SELLER)';


