-- Добавление недостающих колонок в таблицу order_items
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS image_path VARCHAR(500);

-- Добавление колонки updated_at в таблицу orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Обновление существующих записей в order_items (если есть)
UPDATE order_items oi
SET product_name = p.name,
    image_path = p.image_path
FROM products p
WHERE oi.product_id = p.id
  AND (oi.product_name IS NULL OR oi.product_name = '');

-- Установка NOT NULL для product_name после заполнения данных
-- Сначала заполняем NULL значения пустой строкой
UPDATE order_items SET product_name = '' WHERE product_name IS NULL;
ALTER TABLE order_items ALTER COLUMN product_name SET NOT NULL;

-- Установка значения по умолчанию для updated_at в orders
UPDATE orders SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE orders ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

-- Комментарии
COMMENT ON COLUMN order_items.product_name IS 'Название товара на момент покупки';
COMMENT ON COLUMN order_items.image_path IS 'Путь к изображению товара на момент покупки';
COMMENT ON COLUMN orders.updated_at IS 'Дата и время последнего обновления заказа';

