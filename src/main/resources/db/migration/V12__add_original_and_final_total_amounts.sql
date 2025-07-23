-- Add original_total_amount and final_total_amount fields to customer_orders table
-- These fields provide better tracking of order totals before and after discounts

-- Add original_total_amount field (total before discount: subtotal + shipping)
ALTER TABLE customer_orders 
ADD original_total_amount DECIMAL(18,0);

-- Add final_total_amount field (total after discount: original - discount)
ALTER TABLE customer_orders 
ADD final_total_amount DECIMAL(18,0);

-- Update existing records to populate the new fields based on current data
-- original_total_amount = total_amount + discount_amount (reverse the discount)
-- final_total_amount = total_amount (current total is already after discount)
UPDATE customer_orders 
SET 
    original_total_amount = total_amount + ISNULL(discount_amount, 0),
    final_total_amount = total_amount
WHERE original_total_amount IS NULL OR final_total_amount IS NULL;

-- Add indexes for better query performance
CREATE INDEX IX_customer_orders_original_total_amount ON customer_orders(original_total_amount);
CREATE INDEX IX_customer_orders_final_total_amount ON customer_orders(final_total_amount);

-- Add comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Total amount before any discounts are applied (subtotal + shipping fees)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders', 
    @level2type = N'COLUMN', @level2name = N'original_total_amount';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Final total amount after all discounts are applied (original_total_amount - discount_amount)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders', 
    @level2type = N'COLUMN', @level2name = N'final_total_amount';
