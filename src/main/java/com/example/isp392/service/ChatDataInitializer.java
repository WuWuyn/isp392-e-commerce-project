package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import com.example.isp392.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Khởi tạo dữ liệu cho Vector Store (ChromaDB)
 * Lập chỉ mục sách và thông tin cửa hàng
 */
@Component
@Order(100)
public class ChatDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChatDataInitializer.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing data for ReadHub Assistant...");

        if (vectorStore == null) {
            logger.error("Vector Store is null - this should not happen!");
            return;
        }

        logger.info("Vector Store available: {}", vectorStore.getClass().getSimpleName());

        try {
            // Load books with comprehensive information
            vectorStoreService.loadBooksToVectorStore();

            // Add basic store information
            indexStoreInfo();

            logger.info("Data initialization successful! ReadHub Assistant is ready.");

        } catch (Exception e) {
            logger.error("Error initializing data: {}", e.getMessage(), e);
            logger.warn("Chatbot will operate in basic mode.");
        }
    }

    private void indexBooks() {
        try {
            List<Book> activeBooks = bookRepository.findByIsActiveTrue();
            logger.info("📚 Đang lập chỉ mục {} cuốn sách...", activeBooks.size());

            List<Document> bookDocuments = activeBooks.stream()
                    .map(this::createBookDocument)
                    .collect(Collectors.toList());

            if (!bookDocuments.isEmpty()) {
                vectorStore.add(bookDocuments);
                logger.info("✅ Đã lập chỉ mục {} cuốn sách vào Vector Store", bookDocuments.size());
            }

        } catch (Exception e) {
            logger.error("❌ Lỗi khi lập chỉ mục sách: {}", e.getMessage());
        }
    }

    private void indexStoreInfo() {
        try {
            logger.info("🏪 Đang lập chỉ mục thông tin sàn...");

            // Thông tin cửa hàng cơ bản
            String storeInfo = """
                    ReadHub là một nền tảng thương mại điện tử đa người bán, chuyên cung cấp các sản phẩm sách.
                    
                    Thông tin liên hệ:
                    
                    - Website: readhub.vn
                    - Email: support@readhub.vn
                    - Hotline: 1900-1234
                    - Phương thức thanh toán:
                    
                    Thanh toán khi nhận hàng (COD)
                    Ví điện tử: VNPAY
                """;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", "store_info");
            metadata.put("category", "general");

            Document storeDocument = new Document(storeInfo, metadata);
            vectorStore.add(List.of(storeDocument));

            logger.info("✅ Đã lập chỉ mục thông tin sàn");

        } catch (Exception e) {
            logger.error("❌ Lỗi khi lập chỉ mục thông tin sàn: {}", e.getMessage());
        }
    }

    private Document createBookDocument(Book book) {
        StringBuilder content = new StringBuilder();

        // Tạo nội dung tìm kiếm cho sách
        content.append("Sách: ").append(book.getTitle()).append("\n");

        if (book.getAuthors() != null && !book.getAuthors().trim().isEmpty()) {
            content.append("Tác giả: ").append(book.getAuthors()).append("\n");
        }

        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            String categories = book.getCategories().stream()
                    .map(Category::getCategoryName)
                    .collect(Collectors.joining(", "));
            content.append("Thể loại: ").append(categories).append("\n");
        }

        if (book.getDescription() != null && !book.getDescription().trim().isEmpty()) {
            content.append("Mô tả: ").append(book.getDescription()).append("\n");
        }

        content.append("Giá: ").append(book.getSellingPrice()).append(" VND\n");

        if (book.getAverageRating() != null && book.getAverageRating().doubleValue() > 0) {
            content.append("Đánh giá: ").append(book.getAverageRating()).append("/5 sao\n");
        }

        // Metadata để lọc và xử lý
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "book");
        metadata.put("book_id", book.getBookId());
        metadata.put("title", book.getTitle());
        metadata.put("price", book.getSellingPrice());
        metadata.put("rating", book.getAverageRating());

        return new Document(content.toString(), metadata);
    }






}
