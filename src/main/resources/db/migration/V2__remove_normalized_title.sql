-- Xóa chỉ mục idx_book_normalized_title nếu tồn tại
IF EXISTS (
    SELECT * FROM sys.indexes 
    WHERE name = 'idx_book_normalized_title' AND object_id = OBJECT_ID('books')
)
BEGIN
    DROP INDEX idx_book_normalized_title ON books;
    
    PRINT 'Đã xóa chỉ mục idx_book_normalized_title';
END
ELSE
BEGIN
    PRINT 'Chỉ mục idx_book_normalized_title không tồn tại';
END

-- Xóa cột normalized_title khỏi bảng books nếu tồn tại
IF EXISTS (
    SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'books' AND COLUMN_NAME = 'normalized_title'
)
BEGIN
    ALTER TABLE books DROP COLUMN normalized_title;
    
    PRINT 'Đã xóa cột normalized_title khỏi bảng books';
END
ELSE
BEGIN
    PRINT 'Cột normalized_title không tồn tại trong bảng books';
END 