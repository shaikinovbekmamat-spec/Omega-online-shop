-- Вставка тестовых пользователей: продавец и курьер
-- Пароли захешированы через BCrypt (стоимость = 10)
-- ВНИМАНИЕ: Для тестирования используется один хеш для всех паролей
-- В продакшене создайте уникальные хеши для каждого пользователя!

-- Продавец
-- Логин: seller
-- Пароль: admin123 (временно, для тестирования)
INSERT INTO users (username, email, phone, password, role, is_active, created_at, updated_at)
VALUES
    ('seller',
     'seller@omega.com',
     '+996555123456',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- admin123 (временно)
     'SELLER',
     true,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP
    )
ON CONFLICT (username) DO NOTHING;

-- Курьер 1
-- Логин: courier1
-- Пароль: admin123 (временно, для тестирования)
INSERT INTO users (username, email, phone, password, role, is_active, created_at, updated_at)
VALUES
    ('courier1',
     'courier1@omega.com',
     '+996555654321',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- admin123 (временно)
     'COURIER',
     true,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP
    )
ON CONFLICT (username) DO NOTHING;

-- Курьер 2
-- Логин: courier2
-- Пароль: admin123 (временно, для тестирования)
INSERT INTO users (username, email, phone, password, role, is_active, created_at, updated_at)
VALUES
    ('courier2',
     'courier2@omega.com',
     '+996555987654',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- admin123 (временно)
     'COURIER',
     true,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP
    )
ON CONFLICT (username) DO NOTHING;

-- Комментарий
COMMENT ON COLUMN users.password IS 'Пароль захеширован через BCryptPasswordEncoder';

