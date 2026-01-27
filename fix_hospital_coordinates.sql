-- Check and Fix Organization Settings Hospital Coordinates

USE lab_service;

-- 1. First, let's see what columns exist
SHOW COLUMNS FROM organization_settings;

-- 2. Check current data
SELECT * FROM organization_settings;

-- 3. If columns are named differently (like latitude/longitude instead of hospital_lat/hospital_lng),
-- we need to either:
-- A) Rename the columns, or
-- B) Copy data to the correct columns

-- Option A: If you have latitude/longitude columns, copy to hospital_lat/hospital_lng
-- UPDATE organization_settings
-- SET hospital_lat = latitude, hospital_lng = longitude
-- WHERE hospital_lat IS NULL OR hospital_lng IS NULL;

-- Option B: If data needs to be inserted/updated correctly
-- Based on your CSV data:
-- id: 390fbfc0-3a00-4775-b258-d083175388ab
-- city: Le Kremlin-Bicêtre
-- type: MAIN_HOSPITAL
-- country: France
-- lat: 48.813893
-- lng: 2.365315
-- name: MediBridge Hospital
-- address: 14-16 Rue Voltaire
-- email: omprakashreddysaggam@gmail.com
-- postalCode: 94270

-- Update the existing record with proper column names
UPDATE organization_settings
SET
    hospital_lat = 48.813893,
    hospital_lng = 2.365315,
    organization_name = 'MediBridge Hospital',
    address_line1 = '14-16 Rue Voltaire',
    city = 'Le Kremlin-Bicêtre',
    state = 'Île-de-France',
    postal_code = '94270',
    country = 'France',
    contact_email = 'omprakashreddysaggam@gmail.com'
WHERE id = 1
   OR organization_name LIKE '%MediBridge%'
   OR city = 'Le Kremlin-Bicêtre';

-- Verify the update
SELECT
    id,
    organization_name,
    city,
    hospital_lat,
    hospital_lng,
    address_line1,
    postal_code,
    country
FROM organization_settings;
