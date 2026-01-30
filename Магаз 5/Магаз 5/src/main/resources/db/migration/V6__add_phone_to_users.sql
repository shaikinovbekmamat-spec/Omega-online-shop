-- Добавление поля phone в таблицу users
ALTER TABLE users ADD COLUMN phone VARCHAR(20);

-- Комментарий к полю
COMMENT ON COLUMN users.phone IS 'Телефон пользователя';

