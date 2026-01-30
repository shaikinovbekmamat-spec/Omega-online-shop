-- Вставка тестового администратора
-- Пароль: admin123 (захеширован через BCrypt)
INSERT INTO users (username, email, password, role, is_active, created_at, updated_at)
VALUES
    ('admin',
     'admin@omega.com',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'ADMIN',
     true,
     CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP
    );

-- Комментарий
COMMENT ON COLUMN users.password IS 'Пароль захеширован через BCryptPasswordEncoder';