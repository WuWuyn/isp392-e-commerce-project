package com.example.isp392.service;

import com.example.isp392.model.BlogComment;
import com.example.isp392.model.BookReview;
import com.example.isp392.repository.BlogCommentRepository;
import com.example.isp392.repository.BookReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class ModerationService {

    private final BookReviewRepository bookReviewRepository;
    private final BlogCommentRepository blogCommentRepository;

    public ModerationService(BookReviewRepository bookReviewRepository, BlogCommentRepository blogCommentRepository) {
        this.bookReviewRepository = bookReviewRepository;
        this.blogCommentRepository = blogCommentRepository;
    }

    // Lấy danh sách review có phân trang và lọc
    public Page<BookReview> findPaginatedReviews(String search, Integer rating, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<BookReview> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("content"), "%" + search + "%"));
        }
        if (rating != null && rating > 0) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rating"), rating));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdDate"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdDate"), endDate.atTime(23, 59, 59)));
        }
        return bookReviewRepository.findAll(spec, pageable);
    }

    // Lấy danh sách comment có phân trang và lọc
    public Page<BlogComment> findPaginatedComments(String search, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Specification<BlogComment> spec = (root, query, cb) -> cb.conjunction();
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(root.get("content"), "%" + search + "%"));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdDate"), startDate.atStartOfDay()));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdDate"), endDate.atTime(23, 59, 59)));
        }
        return blogCommentRepository.findAll(spec, pageable);
    }

    @Transactional
    public boolean approveReview(Integer reviewId) {
        return bookReviewRepository.findById(reviewId)
                .map(review -> {
                    review.setApproved(true);
                    bookReviewRepository.save(review);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void deleteReview(Integer reviewId) {
        bookReviewRepository.deleteById(reviewId);
    }

    @Transactional
    public boolean approveComment(Integer commentId) {
        return blogCommentRepository.findById(commentId)
                .map(comment -> {
                    comment.setApproved(true);
                    blogCommentRepository.save(comment);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public void deleteComment(Integer commentId) {
        blogCommentRepository.deleteById(commentId);
    }
}