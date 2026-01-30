-- Скрипт для исправления проблемы с миграцией V8
-- Выполните этот SQL в базе данных PostgreSQL перед запуском приложения

-- 1. Проверьте, была ли выполнена миграция V8
-- SELECT * FROM flyway_schema_history WHERE version = '8';

-- 2. Если миграция была выполнена, но колонки не созданы, удалите запись:
-- DELETE FROM flyway_schema_history WHERE version = '8';

-- 3. Создайте недостающие колонки вручную (если они не существуют):
DO $$ 
BEGIN
    -- courier_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_id') THEN
        ALTER TABLE orders ADD COLUMN courier_id BIGINT;
        RAISE NOTICE 'Создана колонка courier_id';
    END IF;
    
    -- delivery_status
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_status') THEN
        ALTER TABLE orders ADD COLUMN delivery_status VARCHAR(20) DEFAULT 'NOT_ASSIGNED';
        RAISE NOTICE 'Создана колонка delivery_status';
    END IF;
    
    -- invoice_number
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'invoice_number') THEN
        ALTER TABLE orders ADD COLUMN invoice_number VARCHAR(50);
        RAISE NOTICE 'Создана колонка invoice_number';
    END IF;
    
    -- seller_comment
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'seller_comment') THEN
        ALTER TABLE orders ADD COLUMN seller_comment TEXT;
        RAISE NOTICE 'Создана колонка seller_comment';
    END IF;
    
    -- courier_comment
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_comment') THEN
        ALTER TABLE orders ADD COLUMN courier_comment TEXT;
        RAISE NOTICE 'Создана колонка courier_comment';
    END IF;
    
    -- courier_assigned_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_assigned_at') THEN
        ALTER TABLE orders ADD COLUMN courier_assigned_at TIMESTAMP;
        RAISE NOTICE 'Создана колонка courier_assigned_at';
    END IF;
    
    -- ready_for_delivery_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'ready_for_delivery_at') THEN
        ALTER TABLE orders ADD COLUMN ready_for_delivery_at TIMESTAMP;
        RAISE NOTICE 'Создана колонка ready_for_delivery_at';
    END IF;
    
    -- delivery_started_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_started_at') THEN
        ALTER TABLE orders ADD COLUMN delivery_started_at TIMESTAMP;
        RAISE NOTICE 'Создана колонка delivery_started_at';
    END IF;
    
    -- delivered_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivered_at') THEN
        ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;
        RAISE NOTICE 'Создана колонка delivered_at';
    END IF;
END $$;

-- Установите значение по умолчанию для существующих записей
UPDATE orders SET delivery_status = 'NOT_ASSIGNED' WHERE delivery_status IS NULL;

-- Добавьте внешний ключ (если еще нет)
DO $$ 
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_orders_courier' 
        AND table_name = 'orders'
    ) THEN
        ALTER TABLE orders 
            ADD CONSTRAINT fk_orders_courier 
            FOREIGN KEY (courier_id) 
            REFERENCES users(id) 
            ON DELETE SET NULL;
        RAISE NOTICE 'Создан внешний ключ fk_orders_courier';
    END IF;
END $$;

-- Создайте индексы (если еще нет)
CREATE INDEX IF NOT EXISTS idx_orders_courier_id ON orders(courier_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_status ON orders(delivery_status);
CREATE INDEX IF NOT EXISTS idx_orders_invoice_number ON orders(invoice_number);

-- Проверьте результат
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'orders' 
AND column_name IN (
    'courier_id', 
    'delivery_status', 
    'invoice_number', 
    'seller_comment', 
    'courier_comment', 
    'courier_assigned_at', 
    'ready_for_delivery_at', 
    'delivery_started_at', 
    'delivered_at'
)
ORDER BY column_name;




