-- Add VNPAY transaction ID field to customer_orders table
-- This field will store the VNPAY transaction reference number for traceability and reconciliation

ALTER TABLE customer_orders 
ADD vnpay_transaction_id VARCHAR(100);

-- Add index for faster lookups by transaction ID
CREATE INDEX IX_customer_orders_vnpay_transaction_id ON customer_orders(vnpay_transaction_id);

-- Add comment for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'VNPAY transaction reference ID for payment traceability and reconciliation', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders', 
    @level2type = N'COLUMN', @level2name = N'vnpay_transaction_id';
