-- Migration V5: Add case-insensitive search index and seed realistic F&B catalog dataset (FF-02-05-2)
-- 1. Index for case-insensitive product name search by store
CREATE INDEX IF NOT EXISTS idx_products_store_active_lower_name
  ON products (store_id, is_active, LOWER(name));

-- 2. Expand F&B Categories
INSERT INTO categories (name, description, is_active, created_at, updated_at)
VALUES
  ('Coffee', 'Freshly brewed Vietnamese and specialty espresso coffee.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Fruit Tea', 'Refreshing real fruit teas and seasonal fruit infusions.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Desserts', 'Sweet treats, puddings, and chilled desserts.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Toppings', 'Delicious additions: boba pearls, jelly, foam, and pudding.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 3. Link categories to FreshFlow Demo Kitchen store
INSERT INTO store_categories (store_id, category_id, is_active, display_order, created_at, updated_at)
SELECT s.id, c.id, TRUE, d.ord, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM categories c
CROSS JOIN stores s
JOIN (VALUES
  ('Beverages', 1),
  ('Bakery', 2),
  ('Coffee', 3),
  ('Fruit Tea', 4),
  ('Desserts', 5),
  ('Toppings', 6)
) AS d(cat_name, ord) ON c.name = d.cat_name
WHERE s.name = 'FreshFlow Demo Kitchen'
ON CONFLICT (store_id, category_id) DO NOTHING;

-- 4. Seed realistic F&B products (34 new products, total 36 products)
INSERT INTO products (store_id, store_category_id, name, description, image_url, is_active, created_at, updated_at)
SELECT
  s.id,
  sc.id,
  v.name,
  v.description,
  v.image_url,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM (VALUES
  -- Beverages (Milk Tea & Tea)
  ('Beverages', 'Oolong Milk Tea', 'Roasted Oolong milk tea with a rich, smoky aroma and silky texture.', 'https://images.freshflow.local/products/oolong-milk-tea.jpg'),
  ('Beverages', 'Matcha Milk Tea', 'Authentic Japanese Uji matcha blended with fresh organic milk.', 'https://images.freshflow.local/products/matcha-milk-tea.jpg'),
  ('Beverages', 'Brown Sugar Pearl Milk Tea', 'Rich brown sugar caramelized tapioca pearls served with fresh cold milk.', 'https://images.freshflow.local/products/brown-sugar-pearl.jpg'),
  ('Beverages', 'Jasmine Green Tea Latte', 'Fragrant jasmine green tea infused with velvety whole milk.', 'https://images.freshflow.local/products/jasmine-green-latte.jpg'),
  ('Beverages', 'Taro Milk Tea', 'Creamy purple taro milk tea with rich natural nutty flavor.', 'https://images.freshflow.local/products/taro-milk-tea.jpg'),
  ('Beverages', 'Earl Grey Milk Tea', 'Classic British Earl Grey black tea scented with bergamot and fresh milk.', 'https://images.freshflow.local/products/earl-grey-milk-tea.jpg'),

  -- Coffee
  ('Coffee', 'Vietnamese Iced Milk Coffee', 'Traditional slow-dripped Robusta phin coffee with sweetened condensed milk and ice.', 'https://images.freshflow.local/products/ca-phe-sua-da.jpg'),
  ('Coffee', 'Vietnamese Black Coffee', 'Bold and intense Vietnamese dark roast Robusta served over ice.', 'https://images.freshflow.local/products/ca-phe-den-da.jpg'),
  ('Coffee', 'Salted Cream Coffee', 'Signature Hue-style salted caramel milk foam poured over iced dark coffee.', 'https://images.freshflow.local/products/ca-phe-muoi.jpg'),
  ('Coffee', 'Bac Xiu Saigon', 'Saigon style creamy sweetened milk with a fragrant drip coffee shot.', 'https://images.freshflow.local/products/bac-xiu.jpg'),
  ('Coffee', 'Iced Americano', 'Smooth double shot espresso diluted with cold filtered water and ice.', 'https://images.freshflow.local/products/iced-americano.jpg'),
  ('Coffee', 'Caramel Macchiato', 'Steamed milk stained with espresso and finished with buttery caramel drizzle.', 'https://images.freshflow.local/products/caramel-macchiato.jpg'),
  ('Coffee', 'Coconut Coffee Smoothie', 'Hai Phong signature blended frozen coconut cream poured over concentrated Robusta coffee.', 'https://images.freshflow.local/products/coconut-coffee.jpg'),

  -- Fruit Tea
  ('Fruit Tea', 'Peach Orange Lemongrass Tea', 'Refreshing black tea infused with sweet peach slices, fresh orange, and fragrant lemongrass.', 'https://images.freshflow.local/products/tra-dao-cam-sa.jpg'),
  ('Fruit Tea', 'Lychee Rose Tea', 'Delicate floral rose black tea paired with whole juicy lychees.', 'https://images.freshflow.local/products/tra-vai-hoa-hong.jpg'),
  ('Fruit Tea', 'Tropical Fruit Tea', 'Jasmine green tea loaded with watermelon, passion fruit, green apple, and orange.', 'https://images.freshflow.local/products/tra-trai-cay-nhiet-doi.jpg'),
  ('Fruit Tea', 'Kumquat Passionfruit Tea', 'Zesty kumquat juice and tangy passionfruit over premium jasmine tea.', 'https://images.freshflow.local/products/tra-quat-chanh-leo.jpg'),
  ('Fruit Tea', 'Guava Pink Tea', 'Sweet pink guava puree shaken with cold brew green tea and crystal boba.', 'https://images.freshflow.local/products/tra-oi-hong.jpg'),
  ('Fruit Tea', 'Mango Passion Chia Tea', 'Tropical sweet mango cubes and passionfruit seeds topped with organic chia seeds.', 'https://images.freshflow.local/products/tra-xoai-chia.jpg'),
  ('Fruit Tea', 'Soursop Iced Tea', 'Refreshing soursop fruit pulp paired with high-mountain black tea.', 'https://images.freshflow.local/products/tra-mang-cau.jpg'),

  -- Bakery
  ('Bakery', 'Garlic Cheese Bread', 'Korean style brioche roll stuffed with cream cheese and soaked in garlic herb butter.', 'https://images.freshflow.local/products/garlic-cheese-bread.jpg'),
  ('Bakery', 'Salted Egg Yolk Pastry', 'Fluffy sponge cake topped with pork floss and savory molten salted egg yolk sauce.', 'https://images.freshflow.local/products/salted-egg-sponge.jpg'),
  ('Bakery', 'Pain Au Chocolat', 'Flaky multi-layered French puff pastry filled with Belgian dark chocolate batons.', 'https://images.freshflow.local/products/pain-au-chocolat.jpg'),
  ('Bakery', 'Almond Danish', 'Crispy puff pastry topped with sweet almond frangipane paste and toasted sliced almonds.', 'https://images.freshflow.local/products/almond-danish.jpg'),
  ('Bakery', 'Ham and Cheese Brioche', 'Warm buttery brioche bun layered with smoked pork ham and melted sharp cheddar cheese.', 'https://images.freshflow.local/products/ham-cheese-brioche.jpg'),

  -- Desserts
  ('Desserts', 'Classic Italian Tiramisu', 'Savoiardi ladyfingers soaked in espresso liqueur layered with mascarpone cream.', 'https://images.freshflow.local/products/tiramisu.jpg'),
  ('Desserts', 'Matcha Panna Cotta', 'Silky smooth Japanese green tea chilled cream topped with sweet red bean paste.', 'https://images.freshflow.local/products/matcha-panna-cotta.jpg'),
  ('Desserts', 'Mango Pudding Chilled', 'Sweet Alphonso mango custard pudding drizzled with creamy coconut milk.', 'https://images.freshflow.local/products/mango-pudding.jpg'),
  ('Desserts', 'Basque Burnt Cheesecake', 'Creamy caramelized Spanish cheesecake with a custard-like molten center.', 'https://images.freshflow.local/products/basque-cheesecake.jpg'),

  -- Toppings
  ('Toppings', 'Golden Boba Pearl', 'Chewy honey-infused golden tapioca pearls cooked fresh daily.', 'https://images.freshflow.local/products/golden-boba.jpg'),
  ('Toppings', 'White Crystal Pearl', 'Crispy 3Q konjac jelly balls with subtle sweet syrup.', 'https://images.freshflow.local/products/white-crystal.jpg'),
  ('Toppings', 'Egg Pudding', 'Soft, silky smooth egg pudding with delicate caramel flavor.', 'https://images.freshflow.local/products/egg-pudding.jpg'),
  ('Toppings', 'Cheese Foam Layer', 'Thick, velvety salted milk cream foam with savory cream cheese flavor.', 'https://images.freshflow.local/products/cheese-foam.jpg'),
  ('Toppings', 'Herbal Grass Jelly', 'Traditional soothing Taiwanese herbal grass jelly cubes.', 'https://images.freshflow.local/products/grass-jelly.jpg')
) AS v(category_name, name, description, image_url)
JOIN categories c ON c.name = v.category_name
JOIN stores s ON s.name = 'FreshFlow Demo Kitchen'
JOIN store_categories sc ON sc.store_id = s.id AND sc.category_id = c.id
ON CONFLICT (store_id, name) DO NOTHING;

-- 5. Seed product variants and daily capacity
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
  updated_at,
  daily_capacity_default
)
SELECT
  p.id,
  v.variant_name,
  v.variant_size,
  v.price,
  v.inventory_mode,
  v.auto_accept_override,
  v.max_quantity_per_order,
  TRUE,
  TRUE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  v.daily_capacity_default
FROM (VALUES
  -- Beverages (M, L)
  ('Oolong Milk Tea', 'M', 'M', 38000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Oolong Milk Tea', 'L', 'L', 48000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Matcha Milk Tea', 'M', 'M', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Matcha Milk Tea', 'L', 'L', 52000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Brown Sugar Pearl Milk Tea', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 120),
  ('Brown Sugar Pearl Milk Tea', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 120),
  ('Jasmine Green Tea Latte', 'M', 'M', 36000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Jasmine Green Tea Latte', 'L', 'L', 46000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Taro Milk Tea', 'M', 'M', 39000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 75),
  ('Taro Milk Tea', 'L', 'L', 49000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 75),
  ('Earl Grey Milk Tea', 'M', 'M', 40000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Earl Grey Milk Tea', 'L', 'L', 50000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),

  -- Coffee (M, L)
  ('Vietnamese Iced Milk Coffee', 'M', 'M', 29000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 150),
  ('Vietnamese Iced Milk Coffee', 'L', 'L', 35000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 150),
  ('Vietnamese Black Coffee', 'M', 'M', 25000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 120),
  ('Vietnamese Black Coffee', 'L', 'L', 30000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 120),
  ('Salted Cream Coffee', 'M', 'M', 35000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Salted Cream Coffee', 'L', 'L', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Bac Xiu Saigon', 'M', 'M', 32000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 130),
  ('Bac Xiu Saigon', 'L', 'L', 39000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 130),
  ('Iced Americano', 'M', 'M', 35000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Iced Americano', 'L', 'L', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Caramel Macchiato', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 70),
  ('Caramel Macchiato', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 70),
  ('Coconut Coffee Smoothie', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 60),
  ('Coconut Coffee Smoothie', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 60),

  -- Fruit Tea (M, L)
  ('Peach Orange Lemongrass Tea', 'M', 'M', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 110),
  ('Peach Orange Lemongrass Tea', 'L', 'L', 52000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 110),
  ('Lychee Rose Tea', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Lychee Rose Tea', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Tropical Fruit Tea', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Tropical Fruit Tea', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 100),
  ('Kumquat Passionfruit Tea', 'M', 'M', 35000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Kumquat Passionfruit Tea', 'L', 'L', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 90),
  ('Guava Pink Tea', 'M', 'M', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 75),
  ('Guava Pink Tea', 'L', 'L', 50000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 75),
  ('Mango Passion Chia Tea', 'M', 'M', 45000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Mango Passion Chia Tea', 'L', 'L', 55000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 80),
  ('Soursop Iced Tea', 'M', 'M', 42000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 70),
  ('Soursop Iced Tea', 'L', 'L', 52000.00, 'MADE_TO_ORDER', NULL::BOOLEAN, 10, 70),

  -- Bakery (STANDARD)
  ('Garlic Cheese Bread', 'STANDARD', NULL::VARCHAR, 38000.00, 'LIMITED_STOCK', FALSE, 4, 40),
  ('Salted Egg Yolk Pastry', 'STANDARD', NULL::VARCHAR, 45000.00, 'LIMITED_STOCK', FALSE, 4, 35),
  ('Pain Au Chocolat', 'STANDARD', NULL::VARCHAR, 32000.00, 'LIMITED_STOCK', FALSE, 4, 30),
  ('Almond Danish', 'STANDARD', NULL::VARCHAR, 35000.00, 'LIMITED_STOCK', FALSE, 4, 25),
  ('Ham and Cheese Brioche', 'STANDARD', NULL::VARCHAR, 42000.00, 'LIMITED_STOCK', FALSE, 4, 30),

  -- Desserts (STANDARD)
  ('Classic Italian Tiramisu', 'STANDARD', NULL::VARCHAR, 48000.00, 'LIMITED_STOCK', FALSE, 4, 30),
  ('Matcha Panna Cotta', 'STANDARD', NULL::VARCHAR, 35000.00, 'LIMITED_STOCK', FALSE, 4, 40),
  ('Mango Pudding Chilled', 'STANDARD', NULL::VARCHAR, 30000.00, 'LIMITED_STOCK', FALSE, 4, 45),
  ('Basque Burnt Cheesecake', 'STANDARD', NULL::VARCHAR, 52000.00, 'LIMITED_STOCK', FALSE, 4, 25),

  -- Toppings (STANDARD)
  ('Golden Boba Pearl', 'STANDARD', NULL::VARCHAR, 10000.00, 'LIMITED_STOCK', FALSE, 10, 200),
  ('White Crystal Pearl', 'STANDARD', NULL::VARCHAR, 12000.00, 'LIMITED_STOCK', FALSE, 10, 180),
  ('Egg Pudding', 'STANDARD', NULL::VARCHAR, 10000.00, 'LIMITED_STOCK', FALSE, 10, 150),
  ('Cheese Foam Layer', 'STANDARD', NULL::VARCHAR, 15000.00, 'LIMITED_STOCK', FALSE, 10, 120),
  ('Herbal Grass Jelly', 'STANDARD', NULL::VARCHAR, 10000.00, 'LIMITED_STOCK', FALSE, 10, 100)
) AS v(product_name, variant_name, variant_size, price, inventory_mode, auto_accept_override, max_quantity_per_order, daily_capacity_default)
JOIN products p ON p.name = v.product_name
JOIN stores s ON s.id = p.store_id AND s.name = 'FreshFlow Demo Kitchen'
ON CONFLICT (product_id, name) DO NOTHING;

-- 6. Ensure existing V2 demo products have default capacity populated if null
UPDATE product_variants
SET daily_capacity_default = 100
WHERE product_id IN (
  SELECT p.id FROM products p
  JOIN stores s ON s.id = p.store_id
  WHERE s.name = 'FreshFlow Demo Kitchen' AND p.name = 'Classic Milk Tea'
) AND daily_capacity_default IS NULL;

UPDATE product_variants
SET daily_capacity_default = 50
WHERE product_id IN (
  SELECT p.id FROM products p
  JOIN stores s ON s.id = p.store_id
  WHERE s.name = 'FreshFlow Demo Kitchen' AND p.name = 'Butter Croissant'
) AND daily_capacity_default IS NULL;
