-- Migration to refactor GroupOrder to CustomerOrder
-- This migration renames tables and updates relationships to align with the new business logic

-- Step 1: Create the new customer_orders table with all necessary fields
CREATE TABLE customer_orders (
    customer_order_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    
    -- Shipping information (moved from orders table)
    recipient_name NVARCHAR(255) NOT NULL,
    recipient_phone VARCHAR(20) NOT NULL,
    shipping_province NVARCHAR(255) NOT NULL,
    shipping_district NVARCHAR(255) NOT NULL,
    shipping_ward NVARCHAR(255) NOT NULL,
    shipping_address_detail NVARCHAR(500) NOT NULL,
    shipping_company NVARCHAR(255),
    shipping_address_type INT, -- 0: Home, 1: Company
    
    -- Payment information (moved from orders table)
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    payment_url NVARCHAR(MAX),
    
    -- Order totals
    total_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    shipping_fee DECIMAL(18,0) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    
    -- Overall status
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- Notes for the entire customer order
    notes NVARCHAR(MAX),
    
    -- Timestamps
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2,
    
    -- Foreign key constraint
    CONSTRAINT FK_customer_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Step 2: Migrate data from group_orders to customer_orders
-- First, get the shipping and payment info from the first order in each group
INSERT INTO customer_orders (
    user_id,
    recipient_name,
    recipient_phone,
    shipping_province,
    shipping_district,
    shipping_ward,
    shipping_address_detail,
    shipping_company,
    shipping_address_type,
    payment_method,
    payment_status,
    payment_url,
    total_amount,
    shipping_fee,
    discount_amount,
    status,
    created_at,
    updated_at
)
SELECT 
    go.user_id,
    COALESCE(first_order.recipient_name, 'Unknown'),
    COALESCE(first_order.recipient_phone, '0000000000'),
    COALESCE(first_order.shipping_province, 'Unknown'),
    COALESCE(first_order.shipping_district, 'Unknown'),
    COALESCE(first_order.shipping_ward, 'Unknown'),
    COALESCE(first_order.shipping_address_detail, 'Unknown'),
    first_order.shipping_company,
    first_order.shipping_address_type,
    first_order.payment_method,
    first_order.payment_status,
    go.payment_url,
    go.total_amount,
    COALESCE(SUM(o.shipping_fee), 0) as total_shipping_fee,
    COALESCE(SUM(o.discount_amount), 0) as total_discount,
    go.status,
    go.created_at,
    go.updated_at
FROM group_orders go
LEFT JOIN orders o ON go.group_order_id = o.group_order_id
LEFT JOIN (
    -- Get the first order for each group to extract shipping/payment info
    SELECT 
        group_order_id,
        recipient_name,
        recipient_phone,
        shipping_province,
        shipping_district,
        shipping_ward,
        shipping_address_detail,
        shipping_company,
        shipping_address_type,
        payment_method,
        payment_status,
        ROW_NUMBER() OVER (PARTITION BY group_order_id ORDER BY order_id) as rn
    FROM orders
    WHERE group_order_id IS NOT NULL
) first_order ON go.group_order_id = first_order.group_order_id AND first_order.rn = 1
GROUP BY 
    go.group_order_id,
    go.user_id,
    go.total_amount,
    go.payment_url,
    go.status,
    go.created_at,
    go.updated_at,
    first_order.recipient_name,
    first_order.recipient_phone,
    first_order.shipping_province,
    first_order.shipping_district,
    first_order.shipping_ward,
    first_order.shipping_address_detail,
    first_order.shipping_company,
    first_order.shipping_address_type,
    first_order.payment_method,
    first_order.payment_status;

-- Step 3: Add customer_order_id column to orders table
ALTER TABLE orders ADD customer_order_id INT;

-- Step 4: Update orders to reference customer_orders instead of group_orders
-- Create a mapping between old group_order_id and new customer_order_id
WITH order_mapping AS (
    SELECT 
        go.group_order_id,
        co.customer_order_id,
        ROW_NUMBER() OVER (PARTITION BY go.user_id ORDER BY go.created_at) as rn_go,
        ROW_NUMBER() OVER (PARTITION BY co.user_id ORDER BY co.created_at) as rn_co
    FROM group_orders go
    INNER JOIN customer_orders co ON go.user_id = co.user_id
)
UPDATE o
SET customer_order_id = om.customer_order_id
FROM orders o
INNER JOIN order_mapping om ON o.group_order_id = om.group_order_id
WHERE om.rn_go = om.rn_co;

-- Step 5: Remove shipping and payment fields from orders table since they moved to customer_orders
-- Note: We'll keep these for now to ensure data integrity, but they should be removed in a future migration
-- after confirming the application works correctly with the new structure

-- Step 6: Add foreign key constraint for customer_order_id
ALTER TABLE orders ADD CONSTRAINT FK_orders_customer_order 
FOREIGN KEY (customer_order_id) REFERENCES customer_orders(customer_order_id);

-- Step 7: Create indexes for better performance
CREATE INDEX IX_customer_orders_user_id ON customer_orders(user_id);
CREATE INDEX IX_customer_orders_status ON customer_orders(status);
CREATE INDEX IX_customer_orders_created_at ON customer_orders(created_at);
CREATE INDEX IX_orders_customer_order_id ON orders(customer_order_id);

-- Step 8: Update any remaining NULL customer_order_id values
-- For orders that don't have a group_order_id, create individual customer_orders
INSERT INTO customer_orders (
    user_id,
    recipient_name,
    recipient_phone,
    shipping_province,
    shipping_district,
    shipping_ward,
    shipping_address_detail,
    shipping_company,
    shipping_address_type,
    payment_method,
    payment_status,
    total_amount,
    shipping_fee,
    discount_amount,
    status,
    created_at,
    updated_at
)
SELECT 
    u.user_id,
    COALESCE(o.recipient_name, u.full_name),
    COALESCE(o.recipient_phone, '0000000000'),
    COALESCE(o.shipping_province, 'Unknown'),
    COALESCE(o.shipping_district, 'Unknown'),
    COALESCE(o.shipping_ward, 'Unknown'),
    COALESCE(o.shipping_address_detail, 'Unknown'),
    o.shipping_company,
    o.shipping_address_type,
    o.payment_method,
    o.payment_status,
    o.total_amount,
    o.shipping_fee,
    o.discount_amount,
    o.order_status,
    o.order_date,
    o.updated_at
FROM orders o
LEFT JOIN users u ON o.user_id = u.user_id
WHERE o.customer_order_id IS NULL AND o.user_id IS NOT NULL;

-- Update orders to reference the newly created customer_orders
UPDATE o
SET customer_order_id = co.customer_order_id
FROM orders o
INNER JOIN customer_orders co ON o.user_id = co.user_id 
    AND o.order_date = co.created_at
WHERE o.customer_order_id IS NULL;

-- Step 9: Make customer_order_id NOT NULL after all orders have been updated
-- First, delete any orders that still don't have a customer_order_id (orphaned orders)
DELETE FROM orders WHERE customer_order_id IS NULL;

-- Now make the column NOT NULL
ALTER TABLE orders ALTER COLUMN customer_order_id INT NOT NULL;

-- Step 10: Add comments for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Customer orders table - represents the complete customer purchase transaction containing multiple shop orders', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'customer_orders';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Reference to the customer order that contains this shop-specific order', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'orders',
    @level2type = N'COLUMN', @level2name = N'customer_order_id';
