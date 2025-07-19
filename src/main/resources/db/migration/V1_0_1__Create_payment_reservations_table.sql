-- Create payment_reservations table
CREATE TABLE payment_reservations (
    reservation_id INT IDENTITY(1,1) PRIMARY KEY,
    user_id INT NOT NULL,
    vnpay_txn_ref NVARCHAR(255) UNIQUE,
    reservation_data NVARCHAR(MAX),
    total_amount DECIMAL(10,2) NOT NULL,
    shipping_fee DECIMAL(10,2),
    discount_amount DECIMAL(10,2),
    payment_method NVARCHAR(50) NOT NULL,
    status NVARCHAR(50) NOT NULL,
    shipping_address_detail NVARCHAR(500),
    shipping_ward NVARCHAR(255),
    shipping_district NVARCHAR(255),
    shipping_province NVARCHAR(255),
    recipient_name NVARCHAR(255),
    recipient_phone NVARCHAR(20),
    notes NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL,
    expires_at DATETIME2 NOT NULL,
    confirmed_at DATETIME2,
    cancelled_at DATETIME2,
    
    CONSTRAINT FK_payment_reservations_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Create indexes for better performance
CREATE INDEX IX_payment_reservations_vnpay_txn_ref ON payment_reservations(vnpay_txn_ref);
CREATE INDEX IX_payment_reservations_user_id ON payment_reservations(user_id);
CREATE INDEX IX_payment_reservations_status ON payment_reservations(status);
CREATE INDEX IX_payment_reservations_expires_at ON payment_reservations(expires_at);
