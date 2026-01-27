-- Add organization_settings table for hospital coordinates
CREATE TABLE IF NOT EXISTS organization_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_name VARCHAR(100) NOT NULL UNIQUE,
    address_line1 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    hospital_lat DECIMAL(10, 7),
    hospital_lng DECIMAL(10, 7),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default organization settings (update coordinates as needed)
INSERT INTO organization_settings (organization_name, address_line1, city, state, postal_code, country, hospital_lat, hospital_lng, contact_phone, contact_email)
VALUES ('MediBridge Central Hospital', '123 Healthcare Blvd', 'Paris', 'Île-de-France', '75001', 'France', 48.8566, 2.3522, '+33-1-234-5678', 'contact@medibridge.fr')
ON DUPLICATE KEY UPDATE organization_name = organization_name;

-- Ensure lab_orders table has delivery coordinates with correct column names
-- This is a safe migration that renames columns if they exist with old names
SET @db_name = DATABASE();

SET @alter_lat = (
    SELECT IF(
        EXISTS(
            SELECT * FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
            AND TABLE_NAME = 'lab_orders'
            AND COLUMN_NAME = 'delivery_latitude'
        ),
        'ALTER TABLE lab_orders CHANGE COLUMN delivery_latitude delivery_lat DECIMAL(10, 7);',
        'SELECT "Column delivery_latitude does not exist, skipping rename" AS Status;'
    )
);

PREPARE stmt FROM @alter_lat;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @alter_lng = (
    SELECT IF(
        EXISTS(
            SELECT * FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
            AND TABLE_NAME = 'lab_orders'
            AND COLUMN_NAME = 'delivery_longitude'
        ),
        'ALTER TABLE lab_orders CHANGE COLUMN delivery_longitude delivery_lng DECIMAL(10, 7);',
        'SELECT "Column delivery_longitude does not exist, skipping rename" AS Status;'
    )
);

PREPARE stmt FROM @alter_lng;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- If columns don't exist at all, add them
SET @add_lat = (
    SELECT IF(
        NOT EXISTS(
            SELECT * FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
            AND TABLE_NAME = 'lab_orders'
            AND (COLUMN_NAME = 'delivery_lat' OR COLUMN_NAME = 'delivery_latitude')
        ),
        'ALTER TABLE lab_orders ADD COLUMN delivery_lat DECIMAL(10, 7);',
        'SELECT "Column delivery_lat already exists" AS Status;'
    )
);

PREPARE stmt FROM @add_lat;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_lng = (
    SELECT IF(
        NOT EXISTS(
            SELECT * FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
            AND TABLE_NAME = 'lab_orders'
            AND (COLUMN_NAME = 'delivery_lng' OR COLUMN_NAME = 'delivery_longitude')
        ),
        'ALTER TABLE lab_orders ADD COLUMN delivery_lng DECIMAL(10, 7);',
        'SELECT "Column delivery_lng already exists" AS Status;'
    )
);

PREPARE stmt FROM @add_lng;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
