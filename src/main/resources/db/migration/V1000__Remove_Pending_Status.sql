-- Migration to remove PENDING status from OrderStatus enum
-- This aligns with the new business flow where orders are created after payment

-- Step 1: Update any existing PENDING orders to PROCESSING
UPDATE orders 
SET order_status = 'PROCESSING' 
WHERE order_status = 'PENDING';

-- Step 2: Update any existing PENDING customer orders to PROCESSING  
UPDATE customer_orders 
SET status = 'PROCESSING' 
WHERE status = 'PENDING';

-- Step 3: Drop existing check constraint for customer_orders status
IF EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_customer_orders_status')
BEGIN
    ALTER TABLE customer_orders DROP CONSTRAINT CK_customer_orders_status;
    PRINT 'Dropped existing CK_customer_orders_status constraint';
END

-- Step 4: Add new check constraint without PENDING
ALTER TABLE customer_orders ADD CONSTRAINT CK_customer_orders_status 
CHECK (status IN ('PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- Step 5: Drop existing check constraint for orders status if it exists
IF EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_orders_status')
BEGIN
    ALTER TABLE orders DROP CONSTRAINT CK_orders_status;
    PRINT 'Dropped existing CK_orders_status constraint';
END

-- Step 6: Add new check constraint for orders without PENDING
ALTER TABLE orders ADD CONSTRAINT CK_orders_status 
CHECK (order_status IN ('PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

-- Step 7: Add comments for clarity
EXEC sp_addextendedproperty 
    @name = N'MS_Description',
    @value = N'Order status after payment completion. PENDING status removed - orders start at PROCESSING.',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'orders',
    @level2type = N'COLUMN', @level2name = N'order_status';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Customer order status aggregated from shop orders. PENDING status removed.',
    @level0type = N'SCHEMA', @level0name = N'dbo',
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'status';

PRINT 'Successfully removed PENDING status from OrderStatus enum and updated database constraints';
