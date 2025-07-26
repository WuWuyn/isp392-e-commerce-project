package com.example.isp392.service;

import com.example.isp392.model.BookReview;
import com.example.isp392.model.OrderItem;
import com.example.isp392.model.User;
import com.example.isp392.repository.BookReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.example.isp392.model.Book;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final BookService bookService;

    public BookReviewService(BookReviewRepository bookReviewRepository, BookService bookService) {
        this.bookReviewRepository = bookReviewRepository;
        this.bookService = bookService;
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

        // Cập nhật thống kê đánh giá cho sách
        if (orderItem != null && orderItem.getBook() != null) {
            bookService.updateBookReviewStatistics(orderItem.getBook().getBookId());
        }
    }

    // Save review
    public BookReview saveReview(BookReview review) {
        BookReview savedReview = bookReviewRepository.save(review);

        // Cập nhật thống kê đánh giá cho sách
        if (review.getOrderItem() != null && review.getOrderItem().getBook() != null) {
            bookService.updateBookReviewStatistics(review.getOrderItem().getBook().getBookId());
        }

        return savedReview;
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

            // **Kiểm tra bảo mật quan trọng**: Chỉ cho phép xóa nếu review thuộc về người dùng hiện tại.
            if (review.getUser().getUserId().equals(currentUser.getUserId())) {
                // Lưu thông tin sách trước khi xóa để cập nhật thống kê
                Integer bookId = null;
                if (review.getOrderItem() != null && review.getOrderItem().getBook() != null) {
                    bookId = review.getOrderItem().getBook().getBookId();
                }

                bookReviewRepository.delete(review);

                // Cập nhật thống kê đánh giá cho sách sau khi xóa
                if (bookId != null) {
                    bookService.updateBookReviewStatistics(bookId);
                }

                return true;
            }
        }
        return false;
    }
    public Optional<BookReview> findByUserAndOrderItem_OrderItemId(User user, Integer orderItemId) {
        return bookReviewRepository.findByUserAndOrderItem_OrderItemId(user, orderItemId);
    }

    public Page<BookReview> getReviewsForSeller(Integer shopId, String searchTitle, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<BookReview> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join các bảng để có thể lọc và sắp xếp
            Join<BookReview, OrderItem> orderItemJoin = root.join("orderItem", JoinType.INNER);
            Join<OrderItem, Book> bookJoin = orderItemJoin.join("book", JoinType.INNER);

            // 1. Luôn lọc theo shopId của người bán
            predicates.add(cb.equal(bookJoin.get("shop").get("shopId"), shopId));

            // 2. Lọc theo tiêu đề sách (nếu có)
            if (searchTitle != null && !searchTitle.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(bookJoin.get("title")), "%" + searchTitle.trim().toLowerCase() + "%"));
            }

            // 3. Lọc theo ngày bắt đầu (nếu có)
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), startDate.atStartOfDay()));
            }

            // 4. Lọc theo ngày kết thúc (nếu có)
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), endDate.atTime(23, 59, 59)));
            }

            // Phân biệt để không bị trùng lặp kết quả khi join
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return bookReviewRepository.findAll(spec, pageable);
    }
    public Page<BookReview> getReviewsForBookBySeller(Integer bookId, Integer shopId, Pageable pageable) {
        return bookReviewRepository.findReviewsByBookIdAndShopId(bookId, shopId, pageable);
    }
}