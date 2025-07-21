-- Migration to add discount_code column to orders table
-- This allows storing the promotion code that was applied to each order

-- Add discount_code column to orders table
ALTER TABLE orders 
ADD discount_code VARCHAR(50) NULL;

-- Add index for better performance when querying by discount code
CREATE INDEX idx_orders_discount_code ON orders(discount_code);

-- Add comment to document the new column
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Promotion code that was applied to this order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'orders', 
    @level2type = N'COLUMN', @level2name = N'discount_code';
