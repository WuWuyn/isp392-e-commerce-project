-- Cleanup migration after GroupOrder to CustomerOrder refactoring
-- This migration removes the old group_orders table and cleans up redundant fields

-- Step 1: Remove the foreign key constraint from orders to group_orders
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_orders_group_order')
    ALTER TABLE orders DROP CONSTRAINT FK_orders_group_order;

-- Step 2: Remove group_order_id column from orders table
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'group_order_id')
    ALTER TABLE orders DROP COLUMN group_order_id;

-- Step 3: Remove user_id column from orders table since user is now accessed through customer_order
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'user_id')
    ALTER TABLE orders DROP COLUMN user_id;

-- Step 4: Remove shipping-related columns from orders table since they moved to customer_orders
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'recipient_name')
    ALTER TABLE orders DROP COLUMN recipient_name;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'recipient_phone')
    ALTER TABLE orders DROP COLUMN recipient_phone;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_province')
    ALTER TABLE orders DROP COLUMN shipping_province;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_district')
    ALTER TABLE orders DROP COLUMN shipping_district;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_ward')
    ALTER TABLE orders DROP COLUMN shipping_ward;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_address_detail')
    ALTER TABLE orders DROP COLUMN shipping_address_detail;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_company')
    ALTER TABLE orders DROP COLUMN shipping_company;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'shipping_address_type')
    ALTER TABLE orders DROP COLUMN shipping_address_type;

-- Step 5: Remove payment-related columns from orders table since they moved to customer_orders
IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'payment_method')
    ALTER TABLE orders DROP COLUMN payment_method;

IF EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('orders') AND name = 'payment_status')
    ALTER TABLE orders DROP COLUMN payment_status;

-- Step 6: Drop the group_orders table
IF EXISTS (SELECT * FROM sys.tables WHERE name = 'group_orders')
    DROP TABLE group_orders;

-- Step 7: Update any remaining native queries or stored procedures that might reference the old structure
-- (This would be done manually based on specific application needs)

-- Step 8: Add additional indexes for the new structure
CREATE INDEX IX_customer_orders_payment_status ON customer_orders(payment_status);
CREATE INDEX IX_customer_orders_payment_method ON customer_orders(payment_method);

-- Step 9: Add check constraints for data integrity
ALTER TABLE customer_orders ADD CONSTRAINT CK_customer_orders_shipping_address_type 
CHECK (shipping_address_type IN (0, 1));

ALTER TABLE customer_orders ADD CONSTRAINT CK_customer_orders_status 
CHECK (status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));

ALTER TABLE customer_orders ADD CONSTRAINT CK_customer_orders_payment_status 
CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED'));

ALTER TABLE customer_orders ADD CONSTRAINT CK_customer_orders_payment_method 
CHECK (payment_method IN ('COD', 'VNPAY', 'BANK_TRANSFER'));

-- Step 10: Add triggers for automatic timestamp updates
CREATE TRIGGER TR_customer_orders_update_timestamp
ON customer_orders
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE customer_orders 
    SET updated_at = GETDATE()
    FROM customer_orders co
    INNER JOIN inserted i ON co.customer_order_id = i.customer_order_id;
END;

-- Step 11: Add extended properties for better documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Unique identifier for the customer order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'customer_order_id';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reference to the user who placed the order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'user_id';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Overall status of the customer order (aggregated from individual shop orders)', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'status';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Total amount for the entire customer order including all shop orders', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders',
    @level2type = N'COLUMN', @level2name = N'total_amount';

-- Step 12: Create a view for backward compatibility if needed
CREATE VIEW vw_order_summary AS
SELECT 
    co.customer_order_id,
    co.user_id,
    co.recipient_name,
    co.recipient_phone,
    co.shipping_province,
    co.shipping_district,
    co.shipping_ward,
    co.shipping_address_detail,
    co.payment_method,
    co.payment_status,
    co.total_amount,
    co.status as customer_order_status,
    co.created_at as customer_order_date,
    o.order_id,
    o.shop_id,
    o.order_status,
    o.order_date,
    o.sub_total,
    o.shipping_fee as order_shipping_fee,
    o.discount_amount as order_discount,
    o.total_amount as order_total
FROM customer_orders co
LEFT JOIN orders o ON co.customer_order_id = o.customer_order_id;

-- Add description for the view
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Summary view combining customer order and individual shop order information', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'VIEW', @level1name = N'vw_order_summary';
