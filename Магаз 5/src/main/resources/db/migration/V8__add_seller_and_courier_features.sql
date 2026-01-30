-- Добавление новых ролей в комментарий (роли хранятся в enum Java)
-- Обновление комментария к таблице users
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'role') THEN
        COMMENT ON COLUMN users.role IS 'Роль пользователя: CLIENT, ADMIN, SELLER или COURIER';
    END IF;
END $$;

-- Добавление полей для курьера и доставки в таблицу orders
DO $$ 
BEGIN
    -- courier_id
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_id') THEN
        ALTER TABLE orders ADD COLUMN courier_id BIGINT;
    END IF;
    
    -- delivery_status
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_status') THEN
        ALTER TABLE orders ADD COLUMN delivery_status VARCHAR(20) DEFAULT 'NOT_ASSIGNED';
    END IF;
    
    -- invoice_number
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'invoice_number') THEN
        ALTER TABLE orders ADD COLUMN invoice_number VARCHAR(50);
    END IF;
    
    -- seller_comment
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'seller_comment') THEN
        ALTER TABLE orders ADD COLUMN seller_comment TEXT;
    END IF;
    
    -- courier_comment
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_comment') THEN
        ALTER TABLE orders ADD COLUMN courier_comment TEXT;
    END IF;
    
    -- courier_assigned_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_assigned_at') THEN
        ALTER TABLE orders ADD COLUMN courier_assigned_at TIMESTAMP;
    END IF;
    
    -- ready_for_delivery_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'ready_for_delivery_at') THEN
        ALTER TABLE orders ADD COLUMN ready_for_delivery_at TIMESTAMP;
    END IF;
    
    -- delivery_started_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_started_at') THEN
        ALTER TABLE orders ADD COLUMN delivery_started_at TIMESTAMP;
    END IF;
    
    -- delivered_at
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivered_at') THEN
        ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;
    END IF;
END $$;

-- Установка значения по умолчанию для существующих записей
UPDATE orders SET delivery_status = 'NOT_ASSIGNED' WHERE delivery_status IS NULL;

-- Добавление внешнего ключа для курьера
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
    END IF;
END $$;

-- Создание индексов для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_orders_courier_id ON orders(courier_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_status ON orders(delivery_status);
CREATE INDEX IF NOT EXISTS idx_orders_invoice_number ON orders(invoice_number);

-- Обновление статуса заказа: добавление нового статуса READY_FOR_DELIVERY
-- (Это изменение в enum Java, в БД просто VARCHAR, поэтому миграция не требуется)

-- Комментарии к новым полям
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_id') THEN
        COMMENT ON COLUMN orders.courier_id IS 'ID курьера, назначенного на доставку';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_status') THEN
        COMMENT ON COLUMN orders.delivery_status IS 'Статус доставки: NOT_ASSIGNED, ASSIGNED, READY, IN_TRANSIT, DELIVERED, FAILED, CANCELLED';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'invoice_number') THEN
        COMMENT ON COLUMN orders.invoice_number IS 'Номер накладной';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'seller_comment') THEN
        COMMENT ON COLUMN orders.seller_comment IS 'Комментарий продавца';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_comment') THEN
        COMMENT ON COLUMN orders.courier_comment IS 'Комментарий курьера';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_assigned_at') THEN
        COMMENT ON COLUMN orders.courier_assigned_at IS 'Дата и время назначения курьера';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'ready_for_delivery_at') THEN
        COMMENT ON COLUMN orders.ready_for_delivery_at IS 'Дата и время готовности к отправке';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_started_at') THEN
        COMMENT ON COLUMN orders.delivery_started_at IS 'Дата и время начала доставки';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivered_at') THEN
        COMMENT ON COLUMN orders.delivered_at IS 'Дата и время доставки';
    END IF;
END $$;

