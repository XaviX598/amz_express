-- Fix the orders status check constraint
-- Run this in your Supabase SQL editor

-- Drop the old constraint
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- Create new constraint with all statuses
ALTER TABLE orders ADD CONSTRAINT orders_status_check CHECK (status IN (
    'PENDIENTE_PAGO', 
    'PAGADO', 
    'PEDIDO_REALIZADO', 
    'LLEGO_BODEGA_USA', 
    'EN_TRANSITO_ECUADOR', 
    'LLEGO_BODEGA_ECUADOR', 
    'LISTO_ENTREGA', 
    'ENTREGADO', 
    'CANCELADO'
));

-- Add missing columns if they don't exist
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(255);
