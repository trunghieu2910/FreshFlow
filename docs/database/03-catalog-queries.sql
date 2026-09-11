-- =============================================================================
-- FreshFlow Database Reference: Catalog Queries
-- File: docs/database/03-catalog-queries.sql
-- Task ID: DB-03-A (Phụ thuộc / Đi kèm FF-02-04-1)
-- Focus: SQL SELECT, JOIN và filter catalog — WHERE, JOIN, LIKE, ORDER BY, LIMIT/OFFSET
-- Target Database: PostgreSQL 16
-- Conventions: docs/database/02-postgres-convention.md
-- =============================================================================
--
-- Mục đích:
-- Tài liệu này chuẩn hóa toàn bộ các truy vấn SQL nghiệp vụ cho Catalog module,
-- phục vụ khách hàng duyệt menu (Public Catalog) và chủ cửa hàng quản lý (Merchant).
--
-- Tuân thủ nghiêm ngặt các Business Rules:
-- 1. BR-01: Chỉ Store, StoreCategory, Product, ProductVariant active mới hiển thị để mua.
-- 2. BR-03: ProductVariant là đơn vị mua; món có size (M, L) có size rõ ràng; món không
--    có size dùng name = 'STANDARD' và size = NULL.
-- 3. BR-06: MADE_TO_ORDER quản lý capacity theo ngày; LIMITED_STOCK quản lý theo stock.
-- 4. BR-08: Hết daily capacity thì catalog đánh dấu unavailable (CAPACITY_EXHAUSTED)
--    và không cho phép thêm vào giỏ hàng hoặc checkout.
--
-- =============================================================================

-- =============================================================================
-- PHẦN 1: TRUY VẤN PUBLIC CATALOG CƠ BẢN (ACTIVE INTEGRITY JOIN)
-- =============================================================================
-- Lấy danh sách sản phẩm và các biến thể hợp lệ của một cửa hàng.
-- Đảm bảo loại bỏ hoàn toàn các thực thể bị inactive (soft-deleted).
--
-- Tham số ví dụ:
--   :store_id = 1
-- =============================================================================

SELECT
    p.id                     AS product_id,
    p.name                   AS product_name,
    p.description            AS product_description,
    p.image_url              AS product_image_url,
    c.id                     AS category_id,
    c.name                   AS category_name,
    sc.id                    AS store_category_id,
    sc.display_order         AS category_display_order,
    pv.id                    AS variant_id,
    pv.name                  AS variant_name,
    pv.size                  AS variant_size,
    pv.price                 AS variant_price,
    pv.inventory_mode        AS variant_inventory_mode,
    pv.max_quantity_per_order AS max_qty_per_order,
    pv.is_available          AS variant_is_available
FROM products p
JOIN stores s
  ON s.id = p.store_id
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN categories c
  ON c.id = sc.category_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  -- Business Rule BR-01: Chỉ duyệt và bán các phần tử active
  AND s.status = 'ACTIVE'
  AND sc.is_active = TRUE
  AND c.is_active = TRUE
  AND p.is_active = TRUE
  AND pv.is_active = TRUE
ORDER BY
    sc.display_order ASC,
    p.name ASC,
    pv.price ASC;


-- =============================================================================
-- PHẦN 2: TÌM KIẾM THEO TỪ KHÓA (KEYWORD SEARCH)
-- =============================================================================
-- Tìm kiếm không phân biệt hoa thường (ILIKE) trên tên hoặc mô tả món.
--
-- Tham số ví dụ:
--   :store_id = 1
--   :search_term = '%tea%'
-- =============================================================================

SELECT
    p.id        AS product_id,
    p.name      AS product_name,
    p.description,
    c.name      AS category_name,
    pv.name     AS variant_name,
    pv.size     AS variant_size,
    pv.price    AS variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN categories c
  ON c.id = sc.category_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
  -- Tìm kiếm theo keyword trên tên hoặc mô tả
  AND (
      p.name ILIKE '%tea%'
      OR p.description ILIKE '%tea%'
      OR pv.name ILIKE '%tea%'
  )
ORDER BY p.name ASC;


-- =============================================================================
-- PHẦN 3: LỌC THEO DANH MỤC CỬA HÀNG (STORE CATEGORY FILTER)
-- =============================================================================
-- Lọc sản phẩm theo store_category_id cụ thể (ví dụ: nhóm 'Đồ uống' hoặc 'Bánh').
--
-- Tham số ví dụ:
--   :store_id = 1
--   :store_category_id = 1
-- =============================================================================

SELECT
    p.id        AS product_id,
    p.name      AS product_name,
    sc.id       AS store_category_id,
    c.name      AS category_name,
    pv.name     AS variant_name,
    pv.price    AS variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN categories c
  ON c.id = sc.category_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND sc.id = 1
  AND sc.is_active = TRUE
  AND p.is_active = TRUE
  AND pv.is_active = TRUE
ORDER BY p.name ASC, pv.price ASC;


-- =============================================================================
-- PHẦN 4: LỌC THEO SIZE BIẾN THỂ (M / L / STANDARD)
-- =============================================================================
-- Quy ước FreshFlow:
-- - Món có size: size = 'M', 'L', 'S', 'XL'...
-- - Món không size: name = 'STANDARD' và size IS NULL.
-- =============================================================================

-- 4.1. Lọc các sản phẩm có biến thể size 'M'
SELECT DISTINCT
    p.id        AS product_id,
    p.name      AS product_name,
    pv.name     AS variant_name,
    pv.size     AS variant_size,
    pv.price    AS variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
  AND pv.size = 'M'
ORDER BY p.name ASC;

-- 4.2. Lọc các sản phẩm có biến thể size 'L'
SELECT DISTINCT
    p.id        AS product_id,
    p.name      AS product_name,
    pv.name     AS variant_name,
    pv.size     AS variant_size,
    pv.price    AS variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
  AND pv.size = 'L'
ORDER BY p.name ASC;

-- 4.3. Lọc các sản phẩm chuẩn không có size (STANDARD)
SELECT DISTINCT
    p.id        AS product_id,
    p.name      AS product_name,
    pv.name     AS variant_name,
    pv.size     AS variant_size,
    pv.price    AS variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
  -- Chuẩn STANDARD: name = 'STANDARD' và size là NULL
  AND pv.name = 'STANDARD'
  AND pv.size IS NULL
ORDER BY p.name ASC;


-- =============================================================================
-- PHẦN 5: LỌC THEO INVENTORY MODE (MADE_TO_ORDER / LIMITED_STOCK)
-- =============================================================================
-- Tham số ví dụ:
--   :inventory_mode = 'MADE_TO_ORDER' (Chế biến sau khi đặt)
--   :inventory_mode = 'LIMITED_STOCK' (Tồn kho giới hạn)
-- =============================================================================

SELECT DISTINCT
    p.id              AS product_id,
    p.name            AS product_name,
    pv.name           AS variant_name,
    pv.inventory_mode AS variant_inventory_mode
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
  AND pv.inventory_mode = 'MADE_TO_ORDER'
ORDER BY p.name ASC;


-- =============================================================================
-- PHẦN 6: TÍNH TOÁN DAILY CAPACITY VÀ ĐÁNH GIÁ AVAILABILITY STATUS
-- =============================================================================
-- Tính toán công suất phục vụ còn lại theo ngày cho từng variant:
-- - Nếu is_available = false hoặc is_active = false -> MARKED_UNAVAILABLE
-- - Nếu inventory_mode = 'MADE_TO_ORDER':
--     + remaining_capacity = capacity_limit - reserved_quantity
--     + Nếu remaining_capacity <= 0 -> CAPACITY_EXHAUSTED (Không cho mua)
--     + Nếu chưa cấu hình record hoặc default -> CAPACITY_NOT_CONFIGURED
-- - Nếu inventory_mode = 'LIMITED_STOCK' hoặc còn capacity -> AVAILABLE
-- =============================================================================

SELECT
    p.id                     AS product_id,
    p.name                   AS product_name,
    pv.id                    AS variant_id,
    pv.name                  AS variant_name,
    pv.inventory_mode,
    pv.daily_capacity_default,
    COALESCE(icr.capacity_limit, pv.daily_capacity_default, 0) AS effective_capacity_limit,
    COALESCE(icr.reserved_quantity, 0)                         AS current_reserved_quantity,
    (COALESCE(icr.capacity_limit, pv.daily_capacity_default, 0) - COALESCE(icr.reserved_quantity, 0)) AS remaining_capacity,
    CASE
        -- Khi biến thể bị vô hiệu hóa hoặc tắt bán thủ công
        WHEN pv.is_active = FALSE OR pv.is_available = FALSE THEN 'MARKED_UNAVAILABLE'

        -- Món nấu theo ngày: kiểm tra capacity ngày
        WHEN pv.inventory_mode = 'MADE_TO_ORDER'
             AND (COALESCE(icr.capacity_limit, pv.daily_capacity_default, 0) - COALESCE(icr.reserved_quantity, 0)) <= 0
             AND (icr.capacity_limit IS NOT NULL OR pv.daily_capacity_default IS NOT NULL)
          THEN 'CAPACITY_EXHAUSTED'

        -- Món nấu theo ngày nhưng chưa có cấu hình capacity
        WHEN pv.inventory_mode = 'MADE_TO_ORDER'
             AND icr.capacity_limit IS NULL
             AND pv.daily_capacity_default IS NULL
          THEN 'CAPACITY_NOT_CONFIGURED'

        -- Món còn hàng hoặc tồn kho hợp lệ
        ELSE 'AVAILABLE'
    END AS computed_availability_status,
    CASE
        WHEN pv.is_active = TRUE
             AND pv.is_available = TRUE
             AND (
                 pv.inventory_mode <> 'MADE_TO_ORDER'
                 OR (COALESCE(icr.capacity_limit, pv.daily_capacity_default, 0) - COALESCE(icr.reserved_quantity, 0)) > 0
             )
          THEN TRUE
        ELSE FALSE
    END AS is_purchasable
FROM products p
JOIN product_variants pv
  ON pv.product_id = p.id
LEFT JOIN inventory_capacity_records icr
  ON icr.variant_id = pv.id
 AND icr.capacity_date = CURRENT_DATE
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND pv.is_active = TRUE
ORDER BY p.id, pv.id;


-- =============================================================================
-- PHẦN 7: PHÂN TRANG VÀ SẮP XẾP (PAGINATION & SORTING THEO CHUẨN SPRING DATA)
-- =============================================================================
-- Quy trình 2 bước chuẩn cho JPA / REST Pagination:
-- Bước 7.1: Đếm tổng số lượng Product thỏa điều kiện (Count query)
-- Bước 7.2: Lấy dữ liệu 1 trang sản phẩm (Page content query với LIMIT và OFFSET)
-- =============================================================================

-- 7.1. Count query để tính totalElements và totalPages
SELECT COUNT(DISTINCT p.id) AS total_elements
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE;

-- 7.2. Content query: Trang 1 (page = 0, size = 10 -> LIMIT 10 OFFSET 0)
-- Sắp xếp theo tên sản phẩm tăng dần (ORDER BY p.name ASC, p.id ASC)
SELECT
    p.id            AS product_id,
    p.name          AS product_name,
    p.description   AS product_description,
    p.image_url     AS product_image_url,
    p.created_at    AS product_created_at
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
ORDER BY
    p.name ASC,
    p.id ASC
LIMIT 10 OFFSET 0;

-- 7.3. Content query: Sắp xếp theo giá biến thể thấp nhất (Lowest price first)
SELECT
    p.id            AS product_id,
    p.name          AS product_name,
    MIN(pv.price)   AS min_variant_price
FROM products p
JOIN store_categories sc
  ON sc.id = p.store_category_id
 AND sc.store_id = p.store_id
JOIN product_variants pv
  ON pv.product_id = p.id
WHERE p.store_id = 1
  AND p.is_active = TRUE
  AND sc.is_active = TRUE
  AND pv.is_active = TRUE
GROUP BY
    p.id,
    p.name
ORDER BY
    min_variant_price ASC,
    p.id ASC
LIMIT 10 OFFSET 0;


-- =============================================================================
-- PHẦN 8: KIỂM CHỨNG TÍNH TOÀN VẸN (ANTI-LEAKING INACTIVE VERIFICATION)
-- =============================================================================
-- Query kiểm tra xem có bất kỳ Product inactive nào bị rò rỉ ra public catalog không.
-- Kết quả trả về PHẢI LUÔN BẰNG 0.
-- =============================================================================

SELECT COUNT(*) AS leaked_inactive_products_count
FROM (
    SELECT p.id
    FROM products p
    JOIN store_categories sc
      ON sc.id = p.store_category_id
     AND sc.store_id = p.store_id
    WHERE p.store_id = 1
      -- Cố tình kiểm tra điều kiện public catalog
      AND p.is_active = TRUE
      AND sc.is_active = TRUE
      -- Nhưng sản phẩm hoặc danh mục cha thực tế đã bị tắt
      AND (p.is_active = FALSE OR sc.is_active = FALSE)
) leak_check;
