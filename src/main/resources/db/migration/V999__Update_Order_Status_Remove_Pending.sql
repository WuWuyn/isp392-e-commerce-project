-- Migration to remove PENDING status and update to PROCESSING
-- This aligns with the new PaymentReservation flow where orders are created after payment

-- Update existing PENDING orders to PROCESSING
UPDATE orders 
SET order_status = 'PROCESSING' 
WHERE order_status = 'PENDING';

-- Update existing PENDING customer orders to PROCESSING  
UPDATE customer_orders 
SET status = 'PROCESSING' 
WHERE status = 'PENDING';

-- Add comments for clarity
EXEC sp_addextendedproperty 
    @name = N'MS_Description',
    @value = N'Order status after payment completion and inventory reservation. Ready for seller preparation.',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'orders',
    @level2type = N'COLUMN', @level2name = N'order_status';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Overall customer order status aggregated from individual shop orders.',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'status';
