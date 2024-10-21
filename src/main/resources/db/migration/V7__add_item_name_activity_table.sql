ALTER TABLE inventory_activity ADD COLUMN IF NOT EXISTS item_name varchar(100);
ALTER TABLE inventory_activity ADD COLUMN IF NOT EXISTS item_id BIGINT;