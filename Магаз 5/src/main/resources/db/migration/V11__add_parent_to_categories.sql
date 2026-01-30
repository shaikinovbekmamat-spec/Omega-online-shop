-- Добавление поддержки иерархии категорий
-- Добавляем поле parent_id для создания подкатегорий

-- Удаляем уникальное ограничение на name (теперь уникальность в рамках родителя)
ALTER TABLE categories 
DROP CONSTRAINT IF EXISTS categories_name_key;

-- Добавляем колонку parent_id
ALTER TABLE categories 
ADD COLUMN IF NOT EXISTS parent_id BIGINT;

-- Добавляем внешний ключ для parent_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_categories_parent' 
        AND table_name = 'categories'
    ) THEN
        ALTER TABLE categories
        ADD CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Добавляем индекс для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories(parent_id);

-- Добавляем уникальный индекс для name в рамках родителя
-- (одна и та же категория может существовать в разных родителях)
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_name_parent 
ON categories(name, COALESCE(parent_id, 0));

-- Комментарий
COMMENT ON COLUMN categories.parent_id IS 'Родительская категория (для создания иерархии подкатегорий)';

