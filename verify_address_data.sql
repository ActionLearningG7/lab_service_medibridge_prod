-- Address Fetching Verification Script
-- Run this to check your lab_orders data

USE lab_service;

-- 1. Check what address data exists
SELECT
    id,
    order_number,
    address_line1,
    city,
    state,
    postal_code,
    country,
    delivery_lat,
    delivery_lng,
    CONCAT_WS(', ',
        NULLIF(address_line1, ''),
        NULLIF(city, ''),
        NULLIF(state, ''),
        CONCAT(NULLIF(postal_code, ''), IF(country IS NOT NULL AND country != '', CONCAT(', ', country), ''))
    ) AS formatted_address
FROM lab_orders
ORDER BY created_at DESC
LIMIT 5;

-- 2. Check which orders are missing coordinates
SELECT
    COUNT(*) as total_orders,
    SUM(CASE WHEN delivery_lat IS NULL OR delivery_lng IS NULL THEN 1 ELSE 0 END) as missing_coordinates,
    SUM(CASE WHEN delivery_lat IS NOT NULL AND delivery_lng IS NOT NULL THEN 1 ELSE 0 END) as has_coordinates
FROM lab_orders;

-- 3. Check which orders have incomplete addresses
SELECT
    id,
    order_number,
    CASE WHEN address_line1 IS NULL OR address_line1 = '' THEN '❌' ELSE '✅' END as has_address_line1,
    CASE WHEN city IS NULL OR city = '' THEN '❌' ELSE '✅' END as has_city,
    CASE WHEN state IS NULL OR state = '' THEN '❌' ELSE '✅' END as has_state,
    CASE WHEN postal_code IS NULL OR postal_code = '' THEN '❌' ELSE '✅' END as has_postal_code,
    CASE WHEN country IS NULL OR country = '' THEN '❌' ELSE '✅' END as has_country
FROM lab_orders
ORDER BY created_at DESC
LIMIT 10;

-- 4. Sample order with full details (replace {ORDER_ID} with actual UUID)
-- SELECT * FROM lab_orders WHERE id = '{ORDER_ID}';
