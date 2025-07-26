package com.example.isp392.repository;

import com.example.isp392.model.BookReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.example.isp392.model.User;
import java.util.Optional;
import java.util.List;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Integer>, JpaSpecificationExecutor<BookReview> {
    
    // Tìm đánh giá cho một quyển sách cụ thể
    @Query("SELECT br FROM BookReview br WHERE br.orderItem.book.bookId = :bookId")
    List<BookReview> findByBookId(int bookId);

    @Query("SELECT br FROM BookReview br JOIN br.orderItem oi JOIN oi.book b WHERE b.shop.shopId = :shopId ORDER BY br.createdDate DESC")
    Page<BookReview> findReviewsByShopId(@Param("shopId") Integer shopId, Pageable pageable);

    @Query("SELECT br FROM BookReview br JOIN br.orderItem oi JOIN oi.book b WHERE b.bookId = :bookId AND b.shop.shopId = :shopId")
    Page<BookReview> findReviewsByBookIdAndShopId(@Param("bookId") Integer bookId, @Param("shopId") Integer shopId, Pageable pageable);
    // Tìm đánh giá với phân trang
    @Query("SELECT br FROM BookReview br WHERE br.orderItem.book.bookId = :bookId")
    Page<BookReview> findByBookId(int bookId, Pageable pageable);

    // Đếm số lượng đánh giá cho một quyển sách
    @Query("SELECT COUNT(br) FROM BookReview br WHERE br.orderItem.book.bookId = :bookId")
    long countByBookId(int bookId);

    // Tìm đánh giá theo sao
    @Query("SELECT br FROM BookReview br WHERE br.orderItem.book.bookId = :bookId AND br.rating >= :minRating")
    Page<BookReview> findByBookIdAndRatingGreaterThanEqual(int bookId, int minRating, Pageable pageable);

    Page<BookReview> findAllByIsApproved(boolean isApproved, Pageable pageable);

    Page<BookReview> findAllByOrderByCreatedDateDesc(Pageable pageable);

    long countByIsApproved(boolean isApproved);

    Optional<BookReview> findByUserAndOrderItem_OrderItemId(User user, Integer orderItemId);

    List<BookReview> findByUser(User user);

    // Đếm số lượng đánh giá đã được duyệt cho một quyển sách
    @Query("SELECT COUNT(br) FROM BookReview br WHERE br.orderItem.book.bookId = :bookId AND br.isApproved = true")
    long countApprovedByBookId(@Param("bookId") Integer bookId);

    // Tính điểm đánh giá trung bình của các đánh giá đã được duyệt cho một quyển sách
    @Query("SELECT AVG(CAST(br.rating AS double)) FROM BookReview br WHERE br.orderItem.book.bookId = :bookId AND br.isApproved = true")
    Double calculateAverageRatingByBookId(@Param("bookId") Integer bookId);
}

