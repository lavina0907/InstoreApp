ALTER TABLE inventory DROP CONSTRAINT IF EXISTS fk_item;

ALTER TABLE inventory DROP COLUMN IF EXISTS item_id;

ALTER TABLE inventory ADD COLUMN item_id INT NOT NULL;

ALTER TABLE inventory ADD CONSTRAINT fk_item FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE;

ALTER TABLE inventory ADD COLUMN inventory_id INT NOT NULL;

ALTER TABLE inventory ADD PRIMARY KEY (inventory_id);