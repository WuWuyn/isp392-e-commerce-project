-- Create chat tables for ReadHub chatbot functionality
-- These tables store chat sessions and messages for basic logging and history

-- Chat Sessions Table
CREATE TABLE chat_sessions (
    session_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_token NVARCHAR(255) NOT NULL UNIQUE,
    user_id INT NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    is_active BIT NOT NULL DEFAULT 1,
    
    -- Foreign key to users table (optional - allows anonymous sessions)
    CONSTRAINT FK_chat_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Chat Messages Table
CREATE TABLE chat_messages (
    message_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    session_id BIGINT NOT NULL,
    content NVARCHAR(MAX) NOT NULL,
    is_user_message BIT NOT NULL,
    message_type NVARCHAR(50) NOT NULL DEFAULT 'text',
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    metadata NVARCHAR(MAX) NULL,
    
    -- Foreign key to chat_sessions table
    CONSTRAINT FK_chat_messages_session_id FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);

-- Indexes for better performance
CREATE INDEX IX_chat_sessions_token ON chat_sessions(session_token);
CREATE INDEX IX_chat_sessions_user_active ON chat_sessions(user_id, is_active);
CREATE INDEX IX_chat_sessions_updated_at ON chat_sessions(updated_at);
CREATE INDEX IX_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX IX_chat_messages_created_at ON chat_messages(created_at);
