package com.example.isp392.repository;

import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    // Tìm sách theo tiêu đề (phân trang)
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    // Tìm sách theo tiêu đề đã được chuẩn hóa (không dấu, chữ thường)
    Page<Book> findByNormalizedTitleContaining(String normalizedTitle, Pageable pageable);
    
    // Tìm sách theo danh mục (phân trang)
    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c = ?1")
    Page<Book> findByCategory(Category category, Pageable pageable);
    
    // Lấy những sách có đánh giá cao nhất
    @Query("SELECT b FROM Book b ORDER BY b.averageRating DESC")
    List<Book> findTopRatedBooks(Pageable pageable);
    
    // Lấy những sách mới thêm vào
    @Query("SELECT b FROM Book b ORDER BY b.dateAdded DESC")
    List<Book> findNewAdditions(Pageable pageable);
    
    // Tìm những sách có giảm giá (selling_price < original_price)
    @Query("SELECT b FROM Book b WHERE b.sellingPrice < b.originalPrice")
    List<Book> findDiscountedBooks(Pageable pageable);
    
    /**
     * Find all books belonging to a specific shop with pagination
     * 
     * @param shopId ID of the shop
     * @param pageable pagination information
     * @return page of books belonging to the shop
     */
    Page<Book> findByShopShopId(Integer shopId, Pageable pageable);
    
    /**
     * Search for books by normalized title within a specific shop
     * 
     * @param shopId ID of the shop
     * @param normalizedTitle Normalized title to search for (case insensitive, accent insensitive)
     * @param pageable pagination information
     * @return page of matching books belonging to the shop
     */
    Page<Book> findByShopShopIdAndNormalizedTitleContaining(Integer shopId, String normalizedTitle, Pageable pageable);
}
