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
                // Cập nhật lại tất cả các sách để đảm bảo xử lý khoảng trắng đúng
                book.updateNormalizedTitle();
                bookRepository.save(book);
                updatedCount++;
            }
            
            logger.info("Đã cập nhật normalizedTitle cho {} cuốn sách.", updatedCount);
        };
    }
} 