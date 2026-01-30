# Тестовые пользователи системы OMEGA Shop

## Администратор
- **Логин:** `admin`
- **Пароль:** `admin123`
- **Email:** admin@omega.com
- **Роль:** ADMIN
- **Доступ:** `/admin/**`

## Продавец
- **Логин:** `seller`
- **Пароль:** `admin123` ⚠️ (временно для тестирования)
- **Email:** seller@omega.com
- **Телефон:** +996555123456
- **Роль:** SELLER
- **Доступ:** `/seller/**`

## Курьер 1
- **Логин:** `courier1`
- **Пароль:** `admin123` ⚠️ (временно для тестирования)
- **Email:** courier1@omega.com
- **Телефон:** +996555654321
- **Роль:** COURIER
- **Доступ:** `/courier/**`

## Курьер 2
- **Логин:** `courier2`
- **Пароль:** `admin123` ⚠️ (временно для тестирования)
- **Email:** courier2@omega.com
- **Телефон:** +996555987654
- **Роль:** COURIER
- **Доступ:** `/courier/**`

## Обычный клиент
Можно зарегистрировать через форму регистрации на `/register`

---

## Где хранятся пароли?

Пароли хранятся в таблице `users` в базе данных PostgreSQL, захешированные через **BCryptPasswordEncoder** (стоимость = 10).

### Просмотр пользователей в базе данных:

```sql
-- Посмотреть всех пользователей
SELECT id, username, email, role, is_active 
FROM users 
ORDER BY role, username;

-- Посмотреть только продавцов и курьеров
SELECT id, username, email, phone, role, is_active 
FROM users 
WHERE role IN ('SELLER', 'COURIER')
ORDER BY role, username;
```

### Создание новых пользователей:

Если нужно создать новых пользователей, используйте миграцию `V9__insert_seller_and_courier_users.sql` или создайте их через админ-панель (если такая функция есть).

---

## Примечание

⚠️ **ВНИМАНИЕ:** 
- Все тестовые пользователи (seller, courier1, courier2) используют пароль `admin123` для упрощения тестирования
- Эти пароли используются **ТОЛЬКО для тестирования**! 
- В продакшене обязательно:
  1. Измените все пароли на сложные и уникальные
  2. Используйте разные пароли для каждого пользователя
  3. Регулярно обновляйте пароли
  4. Не храните пароли в открытом виде в коде

## Где посмотреть пользователей в базе данных?

Выполните SQL запрос:

```sql
-- Все пользователи с ролями
SELECT id, username, email, phone, role, is_active, created_at
FROM users
ORDER BY role, username;

-- Только продавцы и курьеры
SELECT id, username, email, phone, role, is_active
FROM users
WHERE role IN ('SELLER', 'COURIER')
ORDER BY role, username;
```

Пароли хранятся в захешированном виде в колонке `password` (BCrypt).

