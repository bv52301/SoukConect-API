-- 1. Migrate data from customer_addresses
INSERT INTO addresses (
    user_id, owner_id, owner_type,
    label, address_type,
    street, unit, city, postal_code, country,
    is_default, metadata,
    created_at, updated_at
)
SELECT
    c.user_id, ca.customer_id, 'CUSTOMER',
    ca.type, ca.type,
    ca.street, ca.unit, ca.city, ca.postal, ca.country,
    ca.is_default, ca.metadata,
    ca.created_at, ca.updated_at
FROM customer_addresses ca
JOIN customers c ON ca.customer_id = c.customer_id
WHERE c.user_id IS NOT NULL;

-- 2. Migrate data from vendor_locations (Branches)
INSERT INTO addresses (
    user_id, owner_id, owner_type,
    label, address_type,
    street, unit, state, postal_code, landmark,
    latitude, longitude,
    is_active, metadata,
    created_at, updated_at
)
SELECT
    v.user_id, vl.vendor_id, 'VENDOR',
    vl.location_name, 'BRANCH',
    vl.address1, vl.address2, vl.state, vl.pincode, vl.landmark,
    vl.latitude, vl.longitude,
    (vl.status = 'ACTIVE'),
    JSON_OBJECT('schedule', vl.schedule),
    vl.created_at, vl.updated_at
FROM vendor_locations vl
JOIN vendor_details v ON vl.vendor_id = v.vendor_id
WHERE v.user_id IS NOT NULL;

-- 3. Migrate primary address from vendor_details (Main Profile)
INSERT INTO addresses (
    user_id, owner_id, owner_type,
    label, address_type,
    street, unit, state, postal_code, landmark,
    latitude, longitude,
    is_default, metadata,
    created_at, updated_at
)
SELECT
    user_id, vendor_id, 'VENDOR',
    'Primary Location', 'PRIMARY',
    address1, address2, state, pincode, landmark,
    latitude, longitude,
    TRUE, -- is_default
    JSON_OBJECT('schedule', schedule),
    created_at, created_at  -- vendor_details has no updated_at, use created_at
FROM vendor_details
WHERE user_id IS NOT NULL
  AND (address1 IS NOT NULL OR latitude IS NOT NULL);
