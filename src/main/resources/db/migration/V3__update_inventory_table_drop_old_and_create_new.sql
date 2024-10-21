DROP TABLE IF EXISTS inventory;

CREATE TABLE inventory (
    inventory_id INT PRIMARY KEY NOT NULL,
    item_id INT,  -- Foreign key referencing item
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item
        FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
);