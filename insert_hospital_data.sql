-- Direct INSERT/UPDATE for Hospital Coordinates
-- Based on your data: Le Kremlin-Bicêtre, France, lat=48.813893, lng=2.365315

USE lab_service;

-- First, check if table exists and what data is there
SELECT 'Current organization_settings data:' AS info;
SELECT * FROM organization_settings;

-- Delete any incorrect data (optional - uncomment if needed)
-- DELETE FROM organization_settings;

-- Insert or update the correct hospital data
INSERT INTO organization_settings (
    organization_name,
    address_line1,
    city,
    state,
    postal_code,
    country,
    hospital_lat,
    hospital_lng,
    contact_email,
    contact_phone
) VALUES (
    'MediBridge Hospital',
    '14-16 Rue Voltaire',
    'Le Kremlin-Bicêtre',
    'Île-de-France',
    '94270',
    'France',
    48.813893,
    2.365315,
    'omprakashreddysaggam@gmail.com',
    '+33-1-234-5678'
)
ON DUPLICATE KEY UPDATE
    address_line1 = '14-16 Rue Voltaire',
    city = 'Le Kremlin-Bicêtre',
    state = 'Île-de-France',
    postal_code = '94270',
    country = 'France',
    hospital_lat = 48.813893,
    hospital_lng = 2.365315,
    contact_email = 'omprakashreddysaggam@gmail.com';

-- Verify the data is correct
SELECT 'After update:' AS info;
SELECT
    id,
    organization_name,
    address_line1,
    city,
    state,
    postal_code,
    country,
    hospital_lat,
    hospital_lng,
    contact_email
FROM organization_settings;

-- Check if coordinates are properly set
SELECT
    CASE
        WHEN hospital_lat IS NOT NULL AND hospital_lng IS NOT NULL
        THEN '✅ Hospital coordinates are set!'
        ELSE '❌ Hospital coordinates are NULL - needs fixing'
    END AS status,
    hospital_lat,
    hospital_lng
FROM organization_settings;
