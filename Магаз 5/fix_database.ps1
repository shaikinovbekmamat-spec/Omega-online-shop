# PowerShell скрипт для исправления проблемы с миграцией
# Запустите этот скрипт от имени администратора

Write-Host "Исправление проблемы с миграцией V8..." -ForegroundColor Yellow

$dbName = "omegadb"
$dbUser = "postgres"
$dbPassword = "5555"
$dbHost = "localhost"
$dbPort = "5432"

# SQL команды для исправления
$sqlCommands = @"
-- Удаление записи о миграции V8 (если она была выполнена с ошибкой)
DELETE FROM flyway_schema_history WHERE version = '8';

-- Создание недостающих колонок
DO `$`$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_id') THEN
        ALTER TABLE orders ADD COLUMN courier_id BIGINT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_status') THEN
        ALTER TABLE orders ADD COLUMN delivery_status VARCHAR(20) DEFAULT 'NOT_ASSIGNED';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'invoice_number') THEN
        ALTER TABLE orders ADD COLUMN invoice_number VARCHAR(50);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'seller_comment') THEN
        ALTER TABLE orders ADD COLUMN seller_comment TEXT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_comment') THEN
        ALTER TABLE orders ADD COLUMN courier_comment TEXT;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'courier_assigned_at') THEN
        ALTER TABLE orders ADD COLUMN courier_assigned_at TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'ready_for_delivery_at') THEN
        ALTER TABLE orders ADD COLUMN ready_for_delivery_at TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivery_started_at') THEN
        ALTER TABLE orders ADD COLUMN delivery_started_at TIMESTAMP;
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'orders' AND column_name = 'delivered_at') THEN
        ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMP;
    END IF;
END `$`$;

-- Установка значения по умолчанию
UPDATE orders SET delivery_status = 'NOT_ASSIGNED' WHERE delivery_status IS NULL;
"@

# Проверка наличия psql
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue

if (-not $psqlPath) {
    Write-Host "ОШИБКА: psql не найден в PATH. Установите PostgreSQL или добавьте его в PATH." -ForegroundColor Red
    Write-Host "Альтернатива: Выполните SQL команды вручную через pgAdmin или другой клиент PostgreSQL" -ForegroundColor Yellow
    Write-Host "SQL команды сохранены в файле fix_migration.sql" -ForegroundColor Yellow
    exit 1
}

# Выполнение SQL
Write-Host "Выполнение SQL команд..." -ForegroundColor Green

$env:PGPASSWORD = $dbPassword
$sqlCommands | & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nУспешно! Колонки созданы." -ForegroundColor Green
    Write-Host "Теперь верните ddl-auto: validate в application.yaml и перезапустите приложение." -ForegroundColor Yellow
} else {
    Write-Host "`nОШИБКА при выполнении SQL. Проверьте подключение к базе данных." -ForegroundColor Red
    Write-Host "Выполните SQL команды вручную из файла fix_migration.sql" -ForegroundColor Yellow
}




