TO use our app, Firstly Install XAMPP and then In SQl Code DO this operations

Creation of Database:

CREATE TABLE users (
    id VARCHAR(10) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role ENUM('admin', 'cashier') NOT NULL
);

CREATE DATABASE quickmartdb;
USE quickmartdb;

-- Products Table
CREATE TABLE Products (
    product_id VARCHAR(20) PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2),
    stock_quantity INT NOT NULL,
    unit VARCHAR(20),
    barcode VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Sales Table
CREATE TABLE sales (
    sales_detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sales_id BIGINT,
    product_id VARCHAR(20),
    product_name VARCHAR(100) NOT NULL,
    quantity_sold INT NOT NULL,
    sale_price DECIMAL(10,2) NOT NULL,
    sale_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sales_id) REFERENCES main_sales(sales_id),
    FOREIGN KEY (product_id) REFERENCES Products(product_id)
);

ALTER TABLE sales
ADD COLUMN total DECIMAL(12,2) NOT NULL DEFAULT 0.00;

	
CREATE TABLE main_sales (
    sales_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cashier_id VARCHAR(10),
    sales_total DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) DEFAULT 0,
    discount DECIMAL(10,2) DEFAULT 0,
    grand_total DECIMAL(10,2) AS (sales_total + tax - discount) STORED,
    sale_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);

ALTER TABLE main_sales
ADD COLUMN customer_id VARCHAR(10),
ADD COLUMN customer_name VARCHAR(100);

ALTER TABLE main_sales
ADD COLUMN caption VARCHAR(255)

Trigger:
-- 1) Use your DB
USE quickmartdb;

-- 2) Change id to VARCHAR and remove AUTO_INCREMENT
ALTER TABLE users
  MODIFY COLUMN id VARCHAR(10) NOT NULL;

-- If id isn’t the primary key yet, add it (skip if it already is)
ALTER TABLE users
  DROP PRIMARY KEY,
  ADD PRIMARY KEY (id);

-- 3) Drop old trigger (name may already exist)
DROP TRIGGER IF EXISTS before_insert_users;

-- 4) Create trigger to auto-generate id by role
DELIMITER $$

CREATE TRIGGER before_insert_users
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    DECLARE rnd INT;
    DECLARE new_id VARCHAR(10);

    -- Loop a few times to avoid rare collisions
    REPEAT
        SET rnd = FLOOR(RAND() * 9000) + 1000;         -- 1000–9999
        SET new_id = CONCAT(
            CASE WHEN NEW.role = 'admin' THEN 'A-' ELSE 'C-' END,
            LPAD(rnd, 4, '0')
        );
    UNTIL NOT EXISTS (SELECT 1 FROM users WHERE id = new_id)
    END REPEAT;

    SET NEW.id = new_id;
END$$

DELIMITER ;

CREATE TABLE customers (
    c_id VARCHAR(10) PRIMARY KEY,     -- Unique customer ID
    customer_name VARCHAR(100) NOT NULL, 
    phone_no VARCHAR(15) UNIQUE,      -- Unique phone number (can be used for lookup)
    loyalty_points INT DEFAULT 0      -- Loyalty points balance
);

-- Payments Table
CREATE TABLE Payments (
    p_id BIGINT AUTO_INCREMENT PRIMARY KEY,       -- Payment ID
    sales_id BIGINT,                               -- Link to main_sales table
    cashier_id VARCHAR(10),                        -- Who processed the payment (from users table)
    p_code VARCHAR(50) NOT NULL,                   -- Payment code (txn id, reference no.)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Key Constraints
    FOREIGN KEY (sales_id) REFERENCES main_sales(sales_id) ON DELETE CASCADE,
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);



DELIMITER $$

CREATE PROCEDURE update_total_amount()
BEGIN
    UPDATE Products
    SET total_amount = price * stock_quantity;
END $$

DELIMITER ;

-- Settings Table
CREATE TABLE settings (
    setting_id INT AUTO_INCREMENT PRIMARY KEY,
    tax DECIMAL(5,2) DEFAULT 0.00,        -- e.g., 13.00 means 13% tax
    discount DECIMAL(5,2) DEFAULT 0.00,   -- e.g., 5.00 means 5% discount
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
