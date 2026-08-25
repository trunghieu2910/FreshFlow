-- Idempotent catalog demo seed for FF-02-01-2.
-- V1__create_full_schema.sql must already be applied.
-- Natural keys used for repeat-safe execution:
--   user email, store owner, category name, product (store_id, name), variant (product_id, name).

DO $$
DECLARE
demo_owner_user_id BIGINT;
    demo_store_id BIGINT;
    beverage_category_id BIGINT;
    bakery_category_id BIGINT;
    beverage_store_category_id BIGINT;
    bakery_store_category_id BIGINT;
    milk_tea_product_id BIGINT;
    croissant_product_id BIGINT;
BEGIN
INSERT INTO users (
  email,
  password_hash,
  full_name,
  phone,
  status,
  created_at,
  updated_at
)
VALUES (
         'demo.owner@freshflow.local',
         'demo-only-seed-hash-not-for-authentication',
         'FreshFlow Demo Owner',
         '0900000000',
         'ACTIVE',
         CURRENT_TIMESTAMP,
         CURRENT_TIMESTAMP
       )
  ON CONFLICT (email) DO NOTHING;

SELECT id
INTO demo_owner_user_id
FROM users
WHERE email = 'demo.owner@freshflow.local';

INSERT INTO stores (
  owner_user_id,
  name,
  phone,
  address_line,
  auto_accept_default,
  status,
  created_at,
  updated_at
)
SELECT
  demo_owner_user_id,
  'FreshFlow Demo Kitchen',
  '0900000000',
  '1 FreshFlow Street, District 1, Ho Chi Minh City',
  TRUE,
  'ACTIVE',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
  WHERE NOT EXISTS (
        SELECT 1
          FROM stores
         WHERE owner_user_id = demo_owner_user_id
    );

SELECT id
INTO demo_store_id
FROM stores
WHERE owner_user_id = demo_owner_user_id;

INSERT INTO categories (
  name,
  description,
  is_active,
  created_at,
  updated_at
)
VALUES
  (
    'Beverages',
    'FreshFlow demo drinks and milk tea.',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    'Bakery',
    'FreshFlow demo baked products.',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  )
  ON CONFLICT (name) DO NOTHING;

SELECT id
INTO beverage_category_id
FROM categories
WHERE name = 'Beverages';

SELECT id
INTO bakery_category_id
FROM categories
WHERE name = 'Bakery';

INSERT INTO store_categories (
  store_id,
  category_id,
  is_active,
  display_order,
  created_at,
  updated_at
)
SELECT
  demo_store_id,
  beverage_category_id,
  TRUE,
  1,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
  WHERE NOT EXISTS (
        SELECT 1
          FROM store_categories
         WHERE store_id = demo_store_id
           AND category_id = beverage_category_id
    );

INSERT INTO store_categories (
  store_id,
  category_id,
  is_active,
  display_order,
  created_at,
  updated_at
)
SELECT
  demo_store_id,
  bakery_category_id,
  TRUE,
  2,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
  WHERE NOT EXISTS (
        SELECT 1
          FROM store_categories
         WHERE store_id = demo_store_id
           AND category_id = bakery_category_id
    );

SELECT id
INTO beverage_store_category_id
FROM store_categories
WHERE store_id = demo_store_id
  AND category_id = beverage_category_id;

SELECT id
INTO bakery_store_category_id
FROM store_categories
WHERE store_id = demo_store_id
  AND category_id = bakery_category_id;

INSERT INTO products (
  store_id,
  store_category_id,
  name,
  description,
  image_url,
  is_active,
  created_at,
  updated_at
)
SELECT
  demo_store_id,
  beverage_store_category_id,
  'Classic Milk Tea',
  'Demo made-to-order milk tea for catalog and checkout testing.',
  NULL,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
  WHERE NOT EXISTS (
        SELECT 1
          FROM products
         WHERE store_id = demo_store_id
           AND name = 'Classic Milk Tea'
    );

INSERT INTO products (
  store_id,
  store_category_id,
  name,
  description,
  image_url,
  is_active,
  created_at,
  updated_at
)
SELECT
  demo_store_id,
  bakery_store_category_id,
  'Butter Croissant',
  'Demo standard-size bakery product for catalog testing.',
  NULL,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
  WHERE NOT EXISTS (
        SELECT 1
          FROM products
         WHERE store_id = demo_store_id
           AND name = 'Butter Croissant'
    );

SELECT id
INTO milk_tea_product_id
FROM products
WHERE store_id = demo_store_id
  AND name = 'Classic Milk Tea';

SELECT id
INTO croissant_product_id
FROM products
WHERE store_id = demo_store_id
  AND name = 'Butter Croissant';

INSERT INTO product_variants (
  product_id,
  name,
  size,
  price,
  inventory_mode,
  auto_accept_override,
  max_quantity_per_order,
  is_available,
  is_active,
  created_at,
  updated_at
)
VALUES
  (
    milk_tea_product_id,
    'M',
    'M',
    35000.00,
    'MADE_TO_ORDER',
    NULL,
    10,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    milk_tea_product_id,
    'L',
    'L',
    45000.00,
    'MADE_TO_ORDER',
    NULL,
    10,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  ),
  (
    croissant_product_id,
    'STANDARD',
    NULL,
    25000.00,
    'LIMITED_STOCK',
    FALSE,
    4,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
  )
  ON CONFLICT (product_id, name) DO NOTHING;
END $$;
