-- Remove total_amount column as it's now replaced by final_total_amount
-- This migration should be run after ensuring all code uses final_total_amount

-- First, ensure final_total_amount is populated for all records
UPDATE customer_orders 
SET final_total_amount = total_amount 
WHERE final_total_amount IS NULL AND total_amount IS NOT NULL;

-- Add NOT NULL constraint to final_total_amount
ALTER TABLE customer_orders 
ALTER COLUMN final_total_amount DECIMAL(18,0) NOT NULL;

-- Add default value for final_total_amount
ALTER TABLE customer_orders 
ADD CONSTRAINT DF_customer_orders_final_total_amount DEFAULT 0 FOR final_total_amount;

-- Drop the total_amount column (commented out for safety - uncomment when ready)
-- ALTER TABLE customer_orders DROP COLUMN total_amount;

-- Add comment for final_total_amount
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Final total amount after all discounts are applied. This is the amount customer actually pays.', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders', 
    @level2type = N'COLUMN', @level2name = N'final_total_amount';

-- Create index on final_total_amount for better query performance
CREATE INDEX IX_customer_orders_final_total_amount_created_at 
ON customer_orders(final_total_amount, created_at);

-- Note: The total_amount column drop is commented out for safety.
-- Uncomment the DROP COLUMN statement after verifying all systems work correctly with final_total_amount.
