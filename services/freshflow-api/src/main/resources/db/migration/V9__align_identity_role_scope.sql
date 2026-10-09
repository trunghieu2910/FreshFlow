-- Preserve identity IDs and historical foreign keys. Never merge colliding accounts.
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM users GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'Identity migration: normalized email collision';
  END IF;
END $$;
UPDATE users SET email = lower(btrim(email));
ALTER TABLE users ADD CONSTRAINT ck_users_email_canonical
  CHECK (email = lower(btrim(email)));
ALTER TABLE user_store_roles ALTER COLUMN store_id DROP NOT NULL;
CREATE UNIQUE INDEX uk_user_roles_global ON user_store_roles (user_id, role_id)
  WHERE store_id IS NULL;
INSERT INTO roles (code, name, created_at) VALUES
 ('CUSTOMER', 'Customer', now()), ('MERCHANT', 'Merchant', now()), ('DRIVER', 'Driver', now())
ON CONFLICT (code) DO NOTHING;
INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at)
SELECT s.owner_user_id, s.id, r.id, 'ACTIVE', now(), now()
FROM stores s CROSS JOIN roles r WHERE r.code = 'MERCHANT'
ON CONFLICT DO NOTHING;
INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at)
SELECT DISTINCT o.customer_user_id, NULL::bigint, r.id, 'ACTIVE', now(), now()
FROM orders o CROSS JOIN roles r WHERE r.code = 'CUSTOMER'
ON CONFLICT DO NOTHING;
INSERT INTO user_store_roles (user_id, store_id, role_id, status, created_at, updated_at)
SELECT dp.user_id, dp.store_id, r.id, 'ACTIVE', now(), now()
FROM driver_profiles dp CROSS JOIN roles r WHERE r.code = 'DRIVER'
ON CONFLICT DO NOTHING;
