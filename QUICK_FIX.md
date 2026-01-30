# Быстрое исправление проблемы с миграцией

## Проблема
Hibernate не может найти колонку `courier_assigned_at` в таблице `orders`.

## Решение 1: Автоматическое (уже применено)
Я временно изменил `ddl-auto` на `update` в `application.yaml`. 
**Запустите приложение сейчас** - Hibernate создаст недостающие колонки автоматически.

**ВАЖНО:** После успешного запуска верните `ddl-auto: validate` обратно!

## Решение 2: Ручное исправление (рекомендуется)

### Шаг 1: Подключитесь к базе данных
```bash
psql -U postgres -d omegadb
```

### Шаг 2: Выполните SQL скрипт
Выполните содержимое файла `fix_migration.sql` в базе данных.

Или выполните команды напрямую:

```sql
-- Проверьте, была ли выполнена миграция V8
SELECT * FROM flyway_schema_history WHERE version = '8';

-- Если миграция была выполнена, но колонки не созданы, удалите запись:
DELETE FROM flyway_schema_history WHERE version = '8';
```

### Шаг 3: Перезапустите приложение
После выполнения SQL перезапустите приложение - миграция V8 выполнится заново с правильным синтаксисом.

## Решение 3: Использовать update временно

1. Оставьте `ddl-auto: update` в `application.yaml`
2. Запустите приложение - колонки будут созданы
3. После успешного запуска верните `ddl-auto: validate`
4. Перезапустите приложение

## Проверка

После исправления проверьте, что все колонки созданы:

```sql
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
```

Должно быть 9 колонок.





