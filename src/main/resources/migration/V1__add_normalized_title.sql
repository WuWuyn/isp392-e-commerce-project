-- Thêm cột normalized_title vào bảng books nếu chưa tồn tại
IF NOT EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'books' AND COLUMN_NAME = 'normalized_title'
)
BEGIN
    ALTER TABLE books ADD normalized_title NVARCHAR(500) NULL;
    
    PRINT 'Đã thêm cột normalized_title vào bảng books';
END
ELSE
BEGIN
    PRINT 'Cột normalized_title đã tồn tại trong bảng books';
END

-- Tạo chỉ mục cho cột normalized_title nếu chưa tồn tại
IF NOT EXISTS (
    SELECT * FROM sys.indexes 
    WHERE name = 'idx_book_normalized_title' AND object_id = OBJECT_ID('books')
)
BEGIN
    CREATE INDEX idx_book_normalized_title ON books (normalized_title);
    
    PRINT 'Đã tạo chỉ mục idx_book_normalized_title trên cột normalized_title';
END
ELSE
BEGIN
    PRINT 'Chỉ mục idx_book_normalized_title đã tồn tại';
END 