-- Скрипт для назначения продавца всем товарам без seller_id
-- Выполните этот скрипт в pgAdmin, если товары не имеют seller_id

-- Шаг 1: Проверяем, есть ли продавцы в системе
SELECT id, username, email, role 
FROM users 
WHERE role = 'SELLER';

-- Шаг 2: Назначаем первого продавца всем товарам без seller_id
-- Замените YOUR_SELLER_ID на ID продавца из шага 1 (обычно это ID пользователя с username='seller')
UPDATE products
SET seller_id = (
    SELECT id 
    FROM users 
    WHERE role = 'SELLER' 
    ORDER BY id 
    LIMIT 1
)
WHERE seller_id IS NULL;

-- Шаг 3: Проверяем результат
SELECT 
    COUNT(*) as total_products,
    COUNT(seller_id) as products_with_seller,
    COUNT(*) - COUNT(seller_id) as products_without_seller
FROM products;

-- Шаг 4: Просматриваем товары с назначенным продавцом
SELECT 
    p.id,
    p.name,
    p.seller_id,
    u.username as seller_username,
    u.email as seller_email
FROM products p
LEFT JOIN users u ON p.seller_id = u.id
ORDER BY p.id;

