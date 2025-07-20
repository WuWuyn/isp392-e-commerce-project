-- Add status column to group_orders table
ALTER TABLE group_orders ADD status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Update existing group orders based on their orders
UPDATE go
SET go.status = 
    CASE 
        WHEN EXISTS (SELECT 1 FROM orders o WHERE o.group_order_id = go.group_order_id AND o.order_status = 'CANCELLED' GROUP BY o.group_order_id HAVING COUNT(*) = COUNT(CASE WHEN o.order_status = 'CANCELLED' THEN 1 END)) 
            THEN 'CANCELLED'
        WHEN EXISTS (SELECT 1 FROM orders o WHERE o.group_order_id = go.group_order_id AND o.order_status = 'SHIPPED' GROUP BY o.group_order_id HAVING COUNT(*) = COUNT(CASE WHEN o.order_status = 'SHIPPED' THEN 1 END)) 
            THEN 'SHIPPED'
        WHEN EXISTS (SELECT 1 FROM orders o WHERE o.group_order_id = go.group_order_id AND o.order_status = 'PROCESSING' GROUP BY o.group_order_id HAVING COUNT(*) = COUNT(CASE WHEN o.order_status = 'PROCESSING' THEN 1 END)) 
            THEN 'PROCESSING'
        ELSE 'PENDING'
    END
FROM group_orders go; 