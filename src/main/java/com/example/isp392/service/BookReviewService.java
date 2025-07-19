package com.example.isp392.service;

import com.example.isp392.model.BookReview;
import com.example.isp392.model.OrderItem;
import com.example.isp392.model.User;
import com.example.isp392.repository.BookReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookReviewService {

    private final BookReviewRepository bookReviewRepository;

    public BookReviewService(BookReviewRepository bookReviewRepository) {
        this.bookReviewRepository = bookReviewRepository;
    }

    // Lấy đánh giá đã có hoặc tạo mới
    public BookReview getOrCreateReview(User user, Integer orderItemId) {
        return bookReviewRepository.findByUserAndOrderItem_OrderItemId(user, orderItemId)
                .orElse(new BookReview());
    }

    // Kiểm tra xem có phải là đánh giá đã tồn tại không
    public boolean isExistingReview(User user, Integer orderItemId) {
        return bookReviewRepository.findByUserAndOrderItem_OrderItemId(user, orderItemId).isPresent();
    }

    // Lưu hoặc cập nhật đánh giá
    public void saveOrUpdateReview(BookReview review, User user, OrderItem orderItem) {
        Optional<BookReview> existingReviewOpt = bookReviewRepository.findByUserAndOrderItem_OrderItemId(user, orderItem.getOrderItemId());

        if (existingReviewOpt.isPresent()) {
            // Cập nhật đánh giá đã có
            BookReview existingReview = existingReviewOpt.get();
            existingReview.setRating(review.getRating());
            existingReview.setTitle(review.getTitle());
            existingReview.setContent(review.getContent());
            bookReviewRepository.save(existingReview);
        } else {
            // Tạo đánh giá mới
            review.setUser(user);
            review.setOrderItem(orderItem);
            review.setCreatedDate(LocalDateTime.now());
            bookReviewRepository.save(review);
        }
    }

    // Lấy danh sách ID của các OrderItem đã được người dùng đánh giá
    public Set<Integer> getReviewedItemIdsForUser(User user) {
        return bookReviewRepository.findByUser(user).stream()
                .filter(review -> review.getOrderItem() != null)
                .map(review -> review.getOrderItem().getOrderItemId())
                .collect(Collectors.toSet());
    }

    public boolean deleteReview(Integer reviewId, User currentUser) {
        Optional<BookReview> reviewOpt = bookReviewRepository.findById(reviewId);

        if (reviewOpt.isPresent()) {
            BookReview review = reviewOpt.get();

            // THÊM 2 DÒNG SAU ĐỂ KIỂM TRA
            System.out.println("ID người dùng của review: " + review.getUser().getUserId());
            System.out.println("ID người dùng đang đăng nhập: " + currentUser.getUserId());

            // **Kiểm tra bảo mật**:
            if (review.getUser().getUserId().equals(currentUser.getUserId())) {
                bookReviewRepository.delete(review);
                return true;
            }
        }
        return false;
    }
}