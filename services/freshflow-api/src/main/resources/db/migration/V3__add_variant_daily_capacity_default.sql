ALTER TABLE product_variants
  ADD COLUMN daily_capacity_default INTEGER;

ALTER TABLE product_variants
  ADD CONSTRAINT ck_product_variants_daily_capacity_default_non_negative
    CHECK (daily_capacity_default IS NULL OR daily_capacity_default >= 0);
