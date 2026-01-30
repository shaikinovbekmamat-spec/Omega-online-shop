# Инструкция по исправлению проблемы с миграцией

## Проблема
Hibernate не может найти колонку `courier_assigned_at` в таблице `orders`, потому что миграция V8 либо не выполнилась, либо выполнилась с ошибкой.

## Решение 1: Откатить и перевыполнить миграцию (рекомендуется)

1. Подключитесь к базе данных PostgreSQL:
```sql
psql -U postgres -d omegadb
```

2. Проверьте, была ли выполнена миграция V8:
```sql
SELECT * FROM flyway_schema_history WHERE version = '8';
```

3. Если миграция была выполнена, но колонки не созданы, удалите запись:
```sql
DELETE FROM flyway_schema_history WHERE version = '8';
```

4. Перезапустите приложение - миграция выполнится заново.

## Решение 2: Вручную создать колонки

Если не хотите откатывать миграцию, выполните SQL вручную:

```sql
-- Проверьте существование колонок
SELECT column_name 
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
);

-- Если колонок нет, создайте их:
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_id BIGINT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(20) DEFAULT 'NOT_ASSIGNED';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS seller_comment TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_comment TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS courier_assigned_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ready_for_delivery_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_started_at TIMESTAMP;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP;

-- Установите значение по умолчанию
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
    END IF;
END $$;
```

## Решение 3: Временно изменить ddl-auto

Если нужно быстро запустить приложение, временно измените в `application.yaml`:

```yaml
jpa:
  hibernate:
    ddl-auto: update  # Временно, вместо validate
```

После запуска верните обратно на `validate`.

## После исправления

После того как колонки будут созданы, перезапустите приложение. Оно должно запуститься без ошибок.




