ALTER TABLE otp_tokens
ADD token_type VARCHAR(50) NOT NULL DEFAULT 'OTP';
 
-- Update existing rows if needed (though DEFAULT 'OTP' should handle this for new columns)
-- If you had existing data that wasn't OTP, you'd need more complex logic here. 