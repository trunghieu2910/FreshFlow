-- Migration V6: Seed Merchant Ownership Model and Realistic Orders for Multi-tenant isolation (FF-04-01-2, DB-04-A)

DO $$
DECLARE
  v_customer_user_id BIGINT;
  v_merchant1_user_id BIGINT;
  v_merchant2_user_id BIGINT;
  v_store1_id BIGINT;
  v_store2_id BIGINT;
  v_bakery_category_id BIGINT;
  v_store2_category_id BIGINT;
  v_banh_mi_product_id BIGINT;
  v_banh_mi_variant_id BIGINT;
  v_tea_product_id BIGINT;
  v_tea_variant_l_id BIGINT;
  v_coffee_product_id BIGINT;
  v_coffee_variant_id BIGINT;
  v_order1_id BIGINT;
  v_order2_id BIGINT;
  v_order3_id BIGINT;
  v_order101_id BIGINT;
BEGIN
  -- 1. Create Demo Customer & Merchants
  INSERT INTO users (email, password_hash, full_name, phone, status, created_at, updated_at)
  VALUES
    ('customer.demo@freshflow.vn', 'demo-hash', 'Nguyễn Văn An', '0901234456', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('merchant.tea@freshflow.vn', 'demo-hash', 'Lê Thị Thu Trà', '0912345678', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('merchant.bakery@freshflow.vn', 'demo-hash', 'Trần Văn Bánh Mì', '0987654321', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (email) DO NOTHING;

  SELECT id INTO v_customer_user_id FROM users WHERE email = 'customer.demo@freshflow.vn';
  SELECT id INTO v_merchant1_user_id FROM users WHERE email = 'merchant.tea@freshflow.vn';
  SELECT id INTO v_merchant2_user_id FROM users WHERE email = 'merchant.bakery@freshflow.vn';

  -- 2. Link Store 1 (FreshFlow Demo Kitchen) to Merchant 1
  SELECT id INTO v_store1_id FROM stores WHERE name = 'FreshFlow Demo Kitchen' LIMIT 1;
  IF v_store1_id IS NOT NULL AND v_merchant1_user_id IS NOT NULL THEN
    UPDATE stores SET owner_user_id = v_merchant1_user_id WHERE id = v_store1_id;
  END IF;

  -- 3. Create Store 2 owned by Merchant 2
  IF v_merchant2_user_id IS NOT NULL THEN
    INSERT INTO stores (owner_user_id, name, phone, address_line, auto_accept_default, status, created_at, updated_at)
    VALUES (
      v_merchant2_user_id,
      'Tiệm Bánh Mì Sài Gòn & Điểm Tâm Sáng',
      '0987654321',
      '88 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh',
      FALSE,
      'ACTIVE',
      CURRENT_TIMESTAMP,
      CURRENT_TIMESTAMP
    )
    ON CONFLICT (owner_user_id) DO NOTHING;
  END IF;

  SELECT id INTO v_store2_id FROM stores WHERE owner_user_id = v_merchant2_user_id;

  -- 4. Create Category & Product for Store 2
  SELECT id INTO v_bakery_category_id FROM categories WHERE name = 'Bakery' LIMIT 1;
  IF v_store2_id IS NOT NULL AND v_bakery_category_id IS NOT NULL THEN
    INSERT INTO store_categories (store_id, category_id, is_active, display_order, created_at, updated_at)
    VALUES (v_store2_id, v_bakery_category_id, TRUE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (store_id, category_id) DO NOTHING;

    SELECT id INTO v_store2_category_id FROM store_categories WHERE store_id = v_store2_id AND category_id = v_bakery_category_id;

    INSERT INTO products (store_id, store_category_id, name, description, image_url, is_active, created_at, updated_at)
    VALUES (v_store2_id, v_store2_category_id, 'Bánh Mì Thịt Nướng Đặc Biệt', 'Bánh mì giòn rụm kẹp thịt nướng sả thơm lừng, pate gan tươi và đồ chua giòn ngọt.', 'https://images.freshflow.local/products/banh-mi-thit-nuong.jpg', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT (store_id, name) DO NOTHING;

    SELECT id INTO v_banh_mi_product_id FROM products WHERE store_id = v_store2_id AND name = 'Bánh Mì Thịt Nướng Đặc Biệt';

    IF v_banh_mi_product_id IS NOT NULL THEN
      INSERT INTO product_variants (product_id, name, price, is_available, is_active, auto_accept_override, inventory_mode, daily_capacity_default, created_at, updated_at)
      VALUES (v_banh_mi_product_id, 'STANDARD', 35000.00, TRUE, TRUE, NULL, 'MADE_TO_ORDER', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
      ON CONFLICT (product_id, name) DO NOTHING;

      SELECT id INTO v_banh_mi_variant_id FROM product_variants WHERE product_id = v_banh_mi_product_id AND name = 'STANDARD';
    END IF;
  END IF;

  -- 5. Seed Orders for Store 1
  -- Find sample variants in Store 1
  SELECT pv.id, p.id INTO v_tea_variant_l_id, v_tea_product_id
  FROM product_variants pv
  JOIN products p ON pv.product_id = p.id
  WHERE p.store_id = v_store1_id AND p.name ILIKE '%Oolong%'
  LIMIT 1;

  SELECT pv.id, p.id INTO v_coffee_variant_id, v_coffee_product_id
  FROM product_variants pv
  JOIN products p ON pv.product_id = p.id
  WHERE p.store_id = v_store1_id AND p.name ILIKE '%Coffee%'
  LIMIT 1;

  IF v_store1_id IS NOT NULL AND v_customer_user_id IS NOT NULL THEN
    -- Order 1: Awaiting Confirmation
    INSERT INTO orders (
      order_number, customer_user_id, store_id, status, payment_method, merchant_acceptance_status,
      subtotal, delivery_fee, discount_amount, total_amount, created_at, updated_at
    )
    VALUES (
      'ORD-2026-001', v_customer_user_id, v_store1_id, 'AWAITING_MERCHANT_CONFIRMATION', 'CASH_ON_DELIVERY', 'PENDING',
      145000.00, 15000.00, 0.00, 160000.00, CURRENT_TIMESTAMP - INTERVAL '10 minutes', CURRENT_TIMESTAMP - INTERVAL '10 minutes'
    )
    ON CONFLICT (order_number) DO NOTHING;

    SELECT id INTO v_order1_id FROM orders WHERE order_number = 'ORD-2026-001';
    IF v_order1_id IS NOT NULL AND v_tea_variant_l_id IS NOT NULL THEN
      INSERT INTO order_items (order_id, product_id, product_variant_id, product_name_snapshot, variant_name_snapshot, unit_price_snapshot, quantity, line_total)
      VALUES (v_order1_id, v_tea_product_id, v_tea_variant_l_id, 'Trà Đào Cam Sả Tươi', 'Size L', 45000.00, 2, 90000.00)
      ON CONFLICT DO NOTHING;
    END IF;

    -- Order 2: Processing
    INSERT INTO orders (
      order_number, customer_user_id, store_id, status, payment_method, merchant_acceptance_status,
      subtotal, delivery_fee, discount_amount, total_amount, created_at, accepted_at, processing_at, updated_at
    )
    VALUES (
      'ORD-2026-002', v_customer_user_id, v_store1_id, 'PROCESSING', 'ONLINE_MOCK', 'ACCEPTED',
      28000.00, 15000.00, 0.00, 43000.00,
      CURRENT_TIMESTAMP - INTERVAL '25 minutes', CURRENT_TIMESTAMP - INTERVAL '20 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes', CURRENT_TIMESTAMP - INTERVAL '15 minutes'
    )
    ON CONFLICT (order_number) DO NOTHING;

    SELECT id INTO v_order2_id FROM orders WHERE order_number = 'ORD-2026-002';
    IF v_order2_id IS NOT NULL AND v_coffee_variant_id IS NOT NULL THEN
      INSERT INTO order_items (order_id, product_id, product_variant_id, product_name_snapshot, variant_name_snapshot, unit_price_snapshot, quantity, line_total)
      VALUES (v_order2_id, v_coffee_product_id, v_coffee_variant_id, 'Cà Phê Sữa Đá Sài Gòn', 'STANDARD', 28000.00, 1, 28000.00)
      ON CONFLICT DO NOTHING;
    END IF;

    -- Order 3: Shipping
    INSERT INTO orders (
      order_number, customer_user_id, store_id, status, payment_method, merchant_acceptance_status,
      subtotal, delivery_fee, discount_amount, total_amount, created_at, accepted_at, processing_at, updated_at
    )
    VALUES (
      'ORD-2026-003', v_customer_user_id, v_store1_id, 'SHIPPING', 'BANK_TRANSFER_ON_DELIVERY', 'ACCEPTED',
      120000.00, 15000.00, 10000.00, 125000.00,
      CURRENT_TIMESTAMP - INTERVAL '45 minutes', CURRENT_TIMESTAMP - INTERVAL '40 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    )
    ON CONFLICT (order_number) DO NOTHING;

    SELECT id INTO v_order3_id FROM orders WHERE order_number = 'ORD-2026-003';
    IF v_order3_id IS NOT NULL AND v_tea_variant_l_id IS NOT NULL THEN
      INSERT INTO order_items (order_id, product_id, product_variant_id, product_name_snapshot, variant_name_snapshot, unit_price_snapshot, quantity, line_total)
      VALUES (v_order3_id, v_tea_product_id, v_tea_variant_l_id, 'Trà Ô Long Sen Vàng', 'Size M', 40000.00, 3, 120000.00)
      ON CONFLICT DO NOTHING;
    END IF;
  END IF;

  -- 6. Seed Order for Store 2 (Merchant 2) to test isolation
  IF v_store2_id IS NOT NULL AND v_customer_user_id IS NOT NULL THEN
    INSERT INTO orders (
      order_number, customer_user_id, store_id, status, payment_method, merchant_acceptance_status,
      subtotal, delivery_fee, discount_amount, total_amount, created_at, updated_at
    )
    VALUES (
      'ORD-2026-101', v_customer_user_id, v_store2_id, 'AWAITING_MERCHANT_CONFIRMATION', 'CASH_ON_DELIVERY', 'PENDING',
      70000.00, 15000.00, 0.00, 85000.00, CURRENT_TIMESTAMP - INTERVAL '5 minutes', CURRENT_TIMESTAMP - INTERVAL '5 minutes'
    )
    ON CONFLICT (order_number) DO NOTHING;

    SELECT id INTO v_order101_id FROM orders WHERE order_number = 'ORD-2026-101';
    IF v_order101_id IS NOT NULL AND v_banh_mi_variant_id IS NOT NULL THEN
      INSERT INTO order_items (order_id, product_id, product_variant_id, product_name_snapshot, variant_name_snapshot, unit_price_snapshot, quantity, line_total)
      VALUES (v_order101_id, v_banh_mi_product_id, v_banh_mi_variant_id, 'Bánh Mì Thịt Nướng Đặc Biệt', 'STANDARD', 35000.00, 2, 70000.00)
      ON CONFLICT DO NOTHING;
    END IF;
  END IF;
END $$;
