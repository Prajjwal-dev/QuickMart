-- Create database
CREATE DATABASE QuickMartDB;

-- Use the database
USE QuickMartDB;

-- Create Users table
CREATE TABLE Users (
    u_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin', 'cashier') NOT NULL
);

-- Insert sample data
INSERT INTO Users (username, password, role) VALUES
('admin', 'admin123', 'admin'),
('cashier', 'cashier123', 'cashier');
