-- Распределение товаров между двумя продавцами пополам

-- 1. Сначала обнуляем всех продавцов у существующих товаров
UPDATE products
SET seller_id = NULL
WHERE seller_id IS NOT NULL;

-- 2. Распределяем товары пополам между двумя продавцами
WITH numbered_products AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY id) as row_num,
        COUNT(*) OVER () as total_count
    FROM products
)
UPDATE products p
SET seller_id = CASE
                    WHEN np.row_num <= np.total_count / 2 THEN 35  -- Первая половина - prodaves (ID 35)
                    ELSE 37                                        -- Вторая половина - seller (ID 37)
    END
    FROM numbered_products np
WHERE p.id = np.id;

-- 3. Проверяем распределение
SELECT
    u.username as seller_name,
    COUNT(p.id) as product_count,
    ROUND(COUNT(p.id) * 100.0 / (SELECT COUNT(*) FROM products), 2) as percentage
FROM products p
         JOIN users u ON p.seller_id = u.id
GROUP BY u.id, u.username
ORDER BY u.id;

-- Или более детальная проверка:
SELECT
    CASE
        WHEN p.seller_id = 35 THEN 'prodaves (ID 35)'
        WHEN p.seller_id = 37 THEN 'seller (ID 37)'
        ELSE 'Без продавца'
        END as seller,
    COUNT(*) as product_count,
    STRING_AGG(p.id::text, ', ' ORDER BY p.id) as product_ids
FROM products p
GROUP BY p.seller_id
ORDER BY p.seller_id;