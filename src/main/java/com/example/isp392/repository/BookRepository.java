package com.example.isp392.repository;

import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer>, JpaSpecificationExecutor<Book> {
    List<Book> findByIsActiveTrue();
    /**
     * Find book by ID with pessimistic lock to prevent race conditions
     * This is used for inventory management to ensure atomic updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.bookId = :bookId")
    Optional<Book> findByIdForUpdate(@Param("bookId") Integer bookId);
    Page<Book> findByShop_ShopIdAndIsActiveTrue(Integer shopId, Pageable pageable);
    // Tìm sách theo tiêu đề (phân trang)
    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    
    // Tìm sách theo danh mục (phân trang)
    @Query("SELECT b FROM Book b JOIN b.categories c WHERE c = ?1 AND b.isActive = true")
    Page<Book> findByCategory(Category category, Pageable pageable);
    
    // Lấy những sách có đánh giá cao nhất
    // Thêm "WHERE b.active = true"
    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.averageRating DESC")
    List<Book> findTopRatedBooks(Pageable pageable);
    
    // Lấy những sách mới thêm vào
    @Query("SELECT b FROM Book b WHERE b.isActive = true ORDER BY b.dateAdded DESC")
    List<Book> findNewAdditions(Pageable pageable);
    
    // Tìm những sách có giảm giá (selling_price < original_price)
    // Thêm "AND b.active = true"
    @Query("SELECT b FROM Book b WHERE b.sellingPrice < b.originalPrice AND b.isActive = true")
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
     * Find books with low stock by shop ID
     * 
     * @param shopId ID of the shop
     * @param threshold Stock threshold
     * @return List of books with stock below threshold
     */
    List<Book> findByShopShopIdAndStockQuantityLessThanAndIsActiveTrue(Integer shopId, Integer threshold);
    
    /**
     * Find highest rated books by shop ID
     * 
     * @param shopId ID of the shop
     * @param pageable Pagination and sorting
     * @return List of highest rated books
     */
    List<Book> findByShopShopIdAndIsActiveTrueOrderByAverageRatingDesc(Integer shopId, Pageable pageable);
    
    /**
     * Count active books by shop ID
     * 
     * @param shopId ID of the shop
     * @return Count of active books
     */
    long countByShopShopIdAndIsActiveTrue(Integer shopId);
    
    /**
     * Get today's revenue for a shop
     * 
     * @param shopId ID of the shop
     * @param today Today's date
     * @return Total revenue for today
     */
    @Query(value = "SELECT COALESCE(SUM(oi.price * oi.quantity), 0) " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND CONVERT(date, o.order_date) = :today " +
           "AND o.status NOT IN ('CANCELLED', 'REFUNDED')", nativeQuery = true)
    BigDecimal getTodayRevenue(@Param("shopId") Integer shopId, @Param("today") LocalDate today);
    
    /**
     * Get new orders count for a shop within the last N days
     * 
     * @param shopId ID of the shop
     * @param daysAgo Number of days to look back
     * @return Count of new orders
     */
    @Query(value = "SELECT COUNT(DISTINCT o.order_id) " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date >= DATEADD(day, -:daysAgo, GETDATE())", nativeQuery = true)
    int getNewOrdersCount(@Param("shopId") Integer shopId, @Param("daysAgo") int daysAgo);
    
    /**
     * Get weekly revenue data for a shop
     * 
     * @param shopId ID of the shop
     * @return List of daily revenue for the last 7 days
     */
    @Query(value = "SELECT CONVERT(date, o.order_date) AS order_date,\n" +
            "           COALESCE(o.sub_total, 0) AS daily_revenue \n" +
            "           FROM orders o \n" +
            "           JOIN order_items oi ON o.order_id = oi.order_id\n" +
            "           JOIN books b ON oi.book_id = b.book_id \n" +
            "           WHERE b.shop_id = :shopId\n" +
            "           AND o.order_date >= DATEADD(day, -6, CONVERT(date, GETDATE())) \n" +
            "           AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') \n" +
            "           GROUP BY CONVERT(date, o.order_date),o.sub_total\n" +
            "           ORDER BY CONVERT(date, o.order_date)", nativeQuery = true)
    List<Map<String, Object>> getWeeklyRevenue(@Param("shopId") Integer shopId);
    
    /**
     * Get bestselling books by quantity sold
     * 
     * @param shopId ID of the shop
     * @param limit Maximum number of books to return
     * @return List of bestselling books with sales data
     */
    @Query(value = "SELECT TOP 5  b.book_id, b.title, SUM(oi.quantity) as total_quantity, " +
           "o.sub_total as total_revenue " +
           "FROM books b " +
           "JOIN order_items oi ON b.book_id = oi.book_id " +
           "JOIN orders o ON oi.order_id = o.order_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY b.book_id, b.title,o.sub_total " +
           "ORDER BY total_quantity DESC ", nativeQuery = true)
    List<Map<String, Object>> getBestsellingBooksByQuantity(@Param("shopId") Integer shopId, @Param("limit") int limit);
    
    /**
     * Get revenue data for a period (daily, weekly, monthly)
     * 
     * @param shopId ID of the shop
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param groupBy Group by clause (day, week, month)
     * @return List of revenue data grouped by the specified period
     */
    @Query(value = "SELECT " +
           "CASE " +
           "  WHEN :groupBy = 'day' THEN CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
           "  WHEN :groupBy = 'week' THEN CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) " +
           "  WHEN :groupBy = 'month' THEN CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) " +
           "END AS time_period, " +
           "COALESCE(o.sub_total, 0) AS revenue, " +
           "COUNT(DISTINCT o.order_id) AS order_count " +
           "FROM orders o " +
           "JOIN order_items oi ON o.order_id = oi.order_id " +
           "JOIN books b ON oi.book_id = b.book_id " +
           "WHERE b.shop_id = :shopId " +
           "AND o.order_date BETWEEN :startDate AND :endDate " +
           "AND o.order_status NOT IN ('CANCELLED', 'REFUNDED') " +
           "GROUP BY " +
           "CASE " +
           "  WHEN :groupBy = 'day' THEN CONVERT(varchar, CONVERT(date, o.order_date), 120) " +
           "  WHEN :groupBy = 'week' THEN CONCAT(YEAR(o.order_date), '-W', DATEPART(week, o.order_date)) " +
           "  WHEN :groupBy = 'month' THEN CONCAT(YEAR(o.order_date), '-', FORMAT(o.order_date, 'MM')) " +
           "END,  " +
            "o.sub_total" +
           "ORDER BY time_period", nativeQuery = true)
    List<Map<String, Object>> getRevenueByPeriod(
            @Param("shopId") Integer shopId, 
            @Param("startDate") LocalDate startDate, 
            @Param("endDate") LocalDate endDate,
            @Param("groupBy") String groupBy);

    /**
     * Check if a book with the given ISBN exists in a specific shop
     *
     * @param isbn The ISBN to check.
     * @param shopId The ID of the shop to check within.
     * @return true if a book with this ISBN exists in the shop, false otherwise.
     */
    boolean existsByIsbnAndShopShopId(String isbn, Integer shopId);
    
    /**
     * Find a book by ISBN
     *
     * @param isbn ISBN to search for
     * @return Optional containing the book if found, empty otherwise
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Find a book by ISBN within a specific shop
     *
     * @param isbn ISBN to search for
     * @param shopId Shop ID to search within
     * @return Optional containing the book if found, empty otherwise
     */
    Optional<Book> findByIsbnAndShopShopId(String isbn, Integer shopId);

    /**
     * Get total views for all books in a shop
     * @param shopId the shop ID
     * @return total views (sum of viewsCount)
     */
    @Query(value = "SELECT COALESCE(SUM(b.views_count), 0) " +
            "FROM books b join shops s ON b.shop_id = s.shop_id " +
            "WHERE s.shop_id = :shopId", nativeQuery = true)
    Integer getTotalViewsByShopId(@Param("shopId") Integer shopId);

    /**
     * Get views for each product in a shop
     * @param shopId the shop ID
     * @return list of Object[]: [bookId, title, viewsCount]
     */
    @Query(value = "SELECT b.book_id, b.title, b.views_count FROM books b WHERE b.shop_id = :shopId", nativeQuery = true)
    List<Object[]> getViewsByProductInShop(@Param("shopId") Integer shopId);

    /**
     * Increment the viewsCount of a book by 1
     * @param bookId the book ID
     */
    @Modifying
    @Query("UPDATE Book b SET b.viewsCount = b.viewsCount + 1 WHERE b.bookId = :bookId")
    void incrementViewsCount(@Param("bookId") int bookId);

    /**
     * Deactivates all books belonging to a specific shop.
     * Sets their isActive status to false.
     * @param shopId ID of the shop whose books are to be deactivated
     */
    @Modifying
    @Query("UPDATE Book b SET b.isActive = false WHERE b.shop.shopId = :shopId")
    void updateIsActiveByShopId(@Param("shopId") Integer shopId);

    /**
     * Count books added after a specific date
     * 
     * @param date The cutoff date
     * @return Count of books added after the specified date
     */
    @Query("SELECT COUNT(b) FROM Book b WHERE b.dateAdded >= :date")
    long countByDateAddedAfter(@Param("date") LocalDate date);

    /**
     * Get total views for all books across the platform
     * @return total views (sum of viewsCount)
     */
    @Query(value = "SELECT COALESCE(SUM(b.views_count), 0) FROM books b", nativeQuery = true)
    Integer getTotalViewsAllBooks();

    /**
     * Get views for each product across the platform (top N)
     * @param limit Maximum number of products to return
     * @return list of Object[]: [bookId, title, viewsCount]
     */
    @Query(value = "SELECT TOP(:limit) b.book_id, b.title, b.views_count FROM books b ORDER BY b.views_count DESC", nativeQuery = true)
    List<Object[]> getTopViewedBooks(@Param("limit") Integer limit);

    @Modifying
    @Query("UPDATE Book b SET b.isActive = false WHERE b.shop.shopId = :shopId")
    void deactivateBooksByShopId(@Param("shopId") Integer shopId);

    Page<Book> findByShop_ShopIdAndIsActiveTrueAndTitleContainingIgnoreCase(Integer shopId, String title, Pageable pageable);
}
