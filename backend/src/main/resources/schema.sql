-- Amz Express Database Schema
-- Run this script to initialize the database

-- Create database (run as postgres superuser)
-- CREATE DATABASE amzexpress;

-- Connect to amzexpress database and run:

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    product_name VARCHAR(500),
    product_asin VARCHAR(50),
    product_price DECIMAL(10,2),
    weight DECIMAL(10,2),
    shipping_option VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDIENTE_PAGO',
    payment_method VARCHAR(50),
    payment_reference VARCHAR(255),
    total_price DECIMAL(10,2),
    notes TEXT,
    amazon_url VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Add new columns if they don't exist (for existing databases)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);
ALTER TABLE orders ALTER COLUMN status SET DEFAULT 'PENDIENTE_PAGO';

-- Drop old status check constraint if exists and create new one
DO $$ 
BEGIN
    -- Try to drop the old constraint (PostgreSQL syntax)
    ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
EXCEPTION
    WHEN undefined_object THEN
        NULL;
END
$$;

ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN (
    'PENDIENTE_PAGO', 'PAGADO', 'PEDIDO_REALIZADO', 'LLEGO_BODEGA_USA', 
    'EN_TRANSITO_ECUADOR', 'LLEGO_BODEGA_ECUADOR', 'LISTO_ENTREGA', 'ENTREGADO', 'CANCELADO'
));

-- Order status history table
CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    status VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_id ON order_status_history(order_id);

-- Insert default admin users (passwords are BCrypt hashed)
-- Admin password: Admin123!
-- INSERT INTO users (name, email, password, role) VALUES 
-- ('Admin', 'admin@amzexpress.com', '$2a$10$...', 'ADMIN');
-- Note: The password hash above is a placeholder. Generate a real hash using bcrypt.
