-- Legacy demo orders have no delivery address; new checkout orders populate all six columns.
ALTER TABLE orders
  ADD COLUMN recipient_name_snapshot VARCHAR(150),
  ADD COLUMN recipient_phone_snapshot VARCHAR(30),
  ADD COLUMN address_line_snapshot VARCHAR(255),
  ADD COLUMN ward_snapshot VARCHAR(100),
  ADD COLUMN district_snapshot VARCHAR(100),
  ADD COLUMN province_snapshot VARCHAR(100),
  ADD CONSTRAINT ck_orders_address_snapshot_complete CHECK (
    (recipient_name_snapshot IS NULL AND recipient_phone_snapshot IS NULL
      AND address_line_snapshot IS NULL AND ward_snapshot IS NULL
      AND district_snapshot IS NULL AND province_snapshot IS NULL)
    OR
    (NULLIF(btrim(recipient_name_snapshot), '') IS NOT NULL
      AND NULLIF(btrim(recipient_phone_snapshot), '') IS NOT NULL
      AND NULLIF(btrim(address_line_snapshot), '') IS NOT NULL
      AND NULLIF(btrim(ward_snapshot), '') IS NOT NULL
      AND NULLIF(btrim(district_snapshot), '') IS NOT NULL
      AND NULLIF(btrim(province_snapshot), '') IS NOT NULL)
  );
