USE agriprice;

SET @market_image_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'markets'
      AND column_name = 'image_url'
);
SET @market_image_ddl = IF(
    @market_image_column_exists = 0,
    'ALTER TABLE markets ADD COLUMN image_url VARCHAR(255) NULL AFTER location_description',
    'SELECT ''markets.image_url already exists'' AS migration_status'
);
PREPARE market_image_migration FROM @market_image_ddl;
EXECUTE market_image_migration;
DEALLOCATE PREPARE market_image_migration;
