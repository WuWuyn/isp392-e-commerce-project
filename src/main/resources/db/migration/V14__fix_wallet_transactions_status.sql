-- Fix wallet_transactions table status column
-- Add default value for status column and update existing records

-- Check if status column exists, if not add it
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'wallet_transactions' AND COLUMN_NAME = 'status'
)
BEGIN
    ALTER TABLE wallet_transactions ADD status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED';
    PRINT 'Added status column to wallet_transactions table';
END
ELSE
BEGIN
    -- If column exists but has NULL values, update them
    UPDATE wallet_transactions 
    SET status = 'COMPLETED' 
    WHERE status IS NULL;
    
    -- Make sure column is NOT NULL
    ALTER TABLE wallet_transactions 
    ALTER COLUMN status VARCHAR(50) NOT NULL;
    
    PRINT 'Updated existing wallet_transactions status column';
END

-- Add check constraint for valid status values
IF NOT EXISTS (
    SELECT * FROM sys.check_constraints 
    WHERE name = 'CK_wallet_transactions_status_valid'
)
BEGIN
    ALTER TABLE wallet_transactions 
    ADD CONSTRAINT CK_wallet_transactions_status_valid 
    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'));
    
    PRINT 'Added status check constraint to wallet_transactions table';
END

-- Add index for better performance on status queries
IF NOT EXISTS (
    SELECT * FROM sys.indexes 
    WHERE name = 'IX_wallet_transactions_status' AND object_id = OBJECT_ID('wallet_transactions')
)
BEGIN
    CREATE INDEX IX_wallet_transactions_status ON wallet_transactions(status);
    PRINT 'Added status index to wallet_transactions table';
END

-- Add comment for documentation
EXEC sp_addextendedproperty 
    @name = N'MS_Description', 
    @value = N'Transaction status: PENDING, COMPLETED, FAILED, CANCELLED', 
    @level0type = N'SCHEMA', @level0name = N'dbo', 
    @level1type = N'TABLE', @level1name = N'wallet_transactions', 
    @level2type = N'COLUMN', @level2name = N'status';
