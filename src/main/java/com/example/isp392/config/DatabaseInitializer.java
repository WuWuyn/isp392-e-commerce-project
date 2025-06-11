package com.example.isp392.config;

import com.example.isp392.model.Book;
import com.example.isp392.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    /**
     * Cập nhật trường normalizedTitle cho tất cả sách hiện có
     * Chạy một lần khi ứng dụng khởi động
     */
    @Bean
    public CommandLineRunner initializeNormalizedTitles(BookRepository bookRepository) {
        return args -> {
            logger.info("Bắt đầu cập nhật trường normalizedTitle cho sách...");
            
            List<Book> books = bookRepository.findAll();
            int updatedCount = 0;
            
            for (Book book : books) {
                if (book.getNormalizedTitle() == null || book.getNormalizedTitle().isEmpty()) {
                    // Gọi phương thức để cập nhật normalizedTitle
                    book.updateNormalizedTitle();
                    bookRepository.save(book);
                    updatedCount++;
                }
            }
            
            logger.info("Đã cập nhật normalizedTitle cho {} cuốn sách.", updatedCount);
        };
    }
} 