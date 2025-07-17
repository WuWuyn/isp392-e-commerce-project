-- Enhancement of promotion system with comprehensive features
-- This migration adds new columns and tables for the enhanced promotion management system

-- Add new columns to existing promotions table
ALTER TABLE promotions 
ADD COLUMN name NVARCHAR(255) NULL,
ADD COLUMN promotion_type VARCHAR(50) NULL,
ADD COLUMN scope_type VARCHAR(50) NULL,
ADD COLUMN status VARCHAR(50) NULL DEFAULT 'DRAFT',
ADD COLUMN created_by INT NULL,
ADD COLUMN created_at DATETIME2 NULL,
ADD COLUMN updated_by INT NULL,
ADD COLUMN updated_at DATETIME2 NULL;

-- Update existing data with default values
UPDATE promotions 
SET name = COALESCE(description, 'Promotion ' + CAST(promotion_id AS VARCHAR(10))),
    promotion_type = CASE 
        WHEN discount_type = 'PERCENTAGE' THEN 'PERCENTAGE_DISCOUNT'
        WHEN discount_type = 'FIXED_AMOUNT' THEN 'FIXED_AMOUNT_DISCOUNT'
        ELSE 'PERCENTAGE_DISCOUNT'
    END,
    scope_type = 'SITE_WIDE',
    status = CASE 
        WHEN is_active = 1 THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END,
    created_at = GETDATE()
WHERE name IS NULL;

-- Make required columns NOT NULL after setting default values
ALTER TABLE promotions 
ALTER COLUMN name NVARCHAR(255) NOT NULL;

ALTER TABLE promotions 
ALTER COLUMN promotion_type VARCHAR(50) NOT NULL;

ALTER TABLE promotions 
ALTER COLUMN scope_type VARCHAR(50) NOT NULL;

ALTER TABLE promotions 
ALTER COLUMN status VARCHAR(50) NOT NULL;

-- Add foreign key constraints
ALTER TABLE promotions 
ADD CONSTRAINT FK_promotions_created_by 
FOREIGN KEY (created_by) REFERENCES users(user_id);

ALTER TABLE promotions 
ADD CONSTRAINT FK_promotions_updated_by 
FOREIGN KEY (updated_by) REFERENCES users(user_id);

-- Create indexes for better performance
CREATE INDEX idx_promotion_code ON promotions(code);
CREATE INDEX idx_promotion_dates ON promotions(start_date, end_date);
CREATE INDEX idx_promotion_active ON promotions(is_active);
CREATE INDEX idx_promotion_scope ON promotions(scope_type);
CREATE INDEX idx_promotion_status ON promotions(status);
CREATE INDEX idx_promotion_type ON promotions(promotion_type);

-- Create junction tables for scope-specific relationships

-- Promotion-Category relationship
CREATE TABLE promotion_categories (
    promotion_id INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (promotion_id, category_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE
);

-- Promotion-Book relationship
CREATE TABLE promotion_books (
    promotion_id INT NOT NULL,
    book_id INT NOT NULL,
    PRIMARY KEY (promotion_id, book_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

-- Promotion-Shop relationship
CREATE TABLE promotion_shops (
    promotion_id INT NOT NULL,
    shop_id INT NOT NULL,
    PRIMARY KEY (promotion_id, shop_id),
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id) ON DELETE CASCADE,
    FOREIGN KEY (shop_id) REFERENCES shops(shop_id) ON DELETE CASCADE
);

-- Create promotion usage tracking table (for future use)
CREATE TABLE promotion_usage (
    usage_id INT IDENTITY(1,1) PRIMARY KEY,
    promotion_id INT NOT NULL,
    user_id INT NOT NULL,
    order_id INT NULL,
    used_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    discount_amount DECIMAL(18,2) NOT NULL,
    FOREIGN KEY (promotion_id) REFERENCES promotions(promotion_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Create indexes for promotion usage table
CREATE INDEX idx_promotion_usage_promotion ON promotion_usage(promotion_id);
CREATE INDEX idx_promotion_usage_user ON promotion_usage(user_id);
CREATE INDEX idx_promotion_usage_date ON promotion_usage(used_at);

-- Add unique constraint to promotion code if not exists
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'UQ_promotions_code')
BEGIN
    ALTER TABLE promotions 
    ADD CONSTRAINT UQ_promotions_code UNIQUE (code);
END

-- Update current_usage_count to 0 for existing promotions if NULL
UPDATE promotions 
SET current_usage_count = 0 
WHERE current_usage_count IS NULL;

-- Add check constraints for data validation
ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_discount_value_positive 
CHECK (discount_value > 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_max_discount_positive 
CHECK (max_discount_amount IS NULL OR max_discount_amount > 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_min_order_positive 
CHECK (min_order_value IS NULL OR min_order_value > 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_usage_limits_positive 
CHECK (usage_limit_per_user IS NULL OR usage_limit_per_user > 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_total_usage_positive 
CHECK (total_usage_limit IS NULL OR total_usage_limit > 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_current_usage_non_negative 
CHECK (current_usage_count >= 0);

ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_end_after_start 
CHECK (end_date > start_date);

-- Add check constraint for promotion types
ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_type_valid 
CHECK (promotion_type IN ('PERCENTAGE_DISCOUNT', 'FIXED_AMOUNT_DISCOUNT', 'BUY_ONE_GET_ONE', 'FREE_SHIPPING', 'BUNDLE_DISCOUNT'));

-- Add check constraint for scope types
ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_scope_valid 
CHECK (scope_type IN ('SITE_WIDE', 'CATEGORY', 'PRODUCT', 'SHOP'));

-- Add check constraint for status
ALTER TABLE promotions 
ADD CONSTRAINT CK_promotions_status_valid 
CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'EXPIRED', 'USED_UP'));

-- Create view for promotion analytics
CREATE VIEW promotion_analytics AS
SELECT 
    p.promotion_id,
    p.name,
    p.code,
    p.promotion_type,
    p.scope_type,
    p.status,
    p.is_active,
    p.discount_value,
    p.current_usage_count,
    p.total_usage_limit,
    p.start_date,
    p.end_date,
    p.created_at,
    CASE 
        WHEN p.total_usage_limit IS NULL OR p.total_usage_limit = 0 THEN 0
        ELSE (CAST(p.current_usage_count AS FLOAT) / p.total_usage_limit) * 100
    END AS usage_percentage,
    CASE 
        WHEN GETDATE() < p.start_date THEN 'NOT_STARTED'
        WHEN GETDATE() > p.end_date THEN 'EXPIRED'
        WHEN p.total_usage_limit IS NOT NULL AND p.current_usage_count >= p.total_usage_limit THEN 'USED_UP'
        WHEN p.is_active = 1 THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END AS computed_status,
    cb.username AS created_by_username,
    ub.username AS updated_by_username
FROM promotions p
LEFT JOIN users cb ON p.created_by = cb.user_id
LEFT JOIN users ub ON p.updated_by = ub.user_id;

-- Insert sample data for testing (optional - remove in production)
-- This creates a few sample promotions to test the system
INSERT INTO promotions (
    name, code, description, promotion_type, discount_type, discount_value, 
    scope_type, start_date, end_date, is_active, status, 
    usage_limit_per_user, total_usage_limit, current_usage_count, created_at
) VALUES 
(
    'Welcome New Users', 'WELCOME10', 'Welcome discount for new users',
    'PERCENTAGE_DISCOUNT', 'PERCENTAGE', 10.00,
    'SITE_WIDE', GETDATE(), DATEADD(MONTH, 3, GETDATE()), 1, 'ACTIVE',
    1, 1000, 0, GETDATE()
),
(
    'Summer Sale 2024', 'SUMMER20', 'Summer sale discount',
    'PERCENTAGE_DISCOUNT', 'PERCENTAGE', 20.00,
    'SITE_WIDE', GETDATE(), DATEADD(MONTH, 2, GETDATE()), 1, 'ACTIVE',
    3, 500, 0, GETDATE()
),
(
    'Book Lovers Special', 'BOOKLOV15', 'Special discount for book categories',
    'PERCENTAGE_DISCOUNT', 'PERCENTAGE', 15.00,
    'CATEGORY', GETDATE(), DATEADD(MONTH, 1, GETDATE()), 1, 'ACTIVE',
    2, 200, 0, GETDATE()
);

-- Add comments to tables for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Enhanced promotions table with comprehensive promotion management features', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'promotions';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Junction table for promotion-category relationships', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'promotion_categories';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Junction table for promotion-book relationships', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'promotion_books';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Junction table for promotion-shop relationships', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'promotion_shops';

EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Tracks individual promotion usage by users', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'promotion_usage';
