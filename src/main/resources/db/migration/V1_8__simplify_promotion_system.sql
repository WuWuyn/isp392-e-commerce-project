-- Migration to simplify the promotion system
-- Remove complex promotion types and scopes, eliminate DRAFT status

-- Update existing promotions with unsupported types to PERCENTAGE_DISCOUNT
UPDATE promotions 
SET promotion_type = 'PERCENTAGE_DISCOUNT'
WHERE promotion_type NOT IN ('PERCENTAGE_DISCOUNT', 'FIXED_AMOUNT_DISCOUNT');

-- Update existing promotions with unsupported scopes to SITE_WIDE
UPDATE promotions 
SET scope_type = 'SITE_WIDE'
WHERE scope_type NOT IN ('SITE_WIDE', 'CATEGORY');

-- Update existing promotions with DRAFT status to ACTIVE
UPDATE promotions 
SET status = 'ACTIVE'
WHERE status = 'DRAFT';

-- Clear relationships for promotions that are no longer category-specific
-- This will be handled by the application logic, but we can clean up orphaned relationships

-- Remove promotion_books relationships (no longer supported)
DROP TABLE IF EXISTS promotion_books;

-- Remove promotion_shops relationships (no longer supported)  
DROP TABLE IF EXISTS promotion_shops;

-- Add indexes for better performance on simplified fields
CREATE INDEX IF NOT EXISTS idx_promotion_type_scope ON promotions(promotion_type, scope_type);
CREATE INDEX IF NOT EXISTS idx_promotion_active_status ON promotions(is_active, status);

-- Add comment to document the simplification
COMMENT ON TABLE promotions IS 'Simplified promotion system supporting only PERCENTAGE_DISCOUNT/FIXED_AMOUNT_DISCOUNT types and SITE_WIDE/CATEGORY scopes';
