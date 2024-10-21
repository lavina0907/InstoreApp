CREATE TABLE item (
    item_id SERIAL PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    item_price DECIMAL(10, 2) NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE inventory (
    item_id INT PRIMARY KEY,  -- Foreign key referencing item
    total_quantity INT NOT NULL,
    available_quantity INT NOT NULL,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item
        FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
);
CREATE TABLE orders (
    order_id SERIAL PRIMARY KEY,
    creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL
);
CREATE TABLE order_item (
    order_item_id SERIAL PRIMARY KEY,
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    item_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order
        FOREIGN KEY (order_id) REFERENCES "orders"(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_item
        FOREIGN KEY (item_id) REFERENCES item(item_id) ON DELETE CASCADE
);
