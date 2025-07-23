-- Migration to add promotion_code column to customer_orders table
-- This allows storing the promotion code that was applied to the entire customer order

-- Add promotion_code column to customer_orders table
ALTER TABLE customer_orders 
ADD promotion_code VARCHAR(50) NULL;

-- Add index for better performance when querying by promotion code
CREATE INDEX idx_customer_orders_promotion_code ON customer_orders(promotion_code);

-- Add comment to document the new column
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Promotion code that was applied to this customer order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders', 
    @level2type = N'COLUMN', @level2name = N'promotion_code';
