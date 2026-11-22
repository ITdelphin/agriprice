USE agriprice;

-- Safe to rerun against an existing database. Product and price rows remain untouched.
SET @product_image_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'products'
      AND column_name = 'image_url'
);
SET @product_image_ddl = IF(
    @product_image_column_exists = 0,
    'ALTER TABLE products ADD COLUMN image_url VARCHAR(255) NULL AFTER description',
    'SELECT ''products.image_url already exists'' AS migration_status'
);
PREPARE product_image_migration FROM @product_image_ddl;
EXECUTE product_image_migration;
DEALLOCATE PREPARE product_image_migration;
