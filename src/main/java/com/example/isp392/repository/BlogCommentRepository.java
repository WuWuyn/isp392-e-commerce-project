package com.example.isp392.repository;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer>, JpaSpecificationExecutor<BlogComment> {
    
    // Find comments by blog
    List<BlogComment> findByBlogPostOrderByCreatedDateDesc(Blog blogPost);
    
    // Find comments by blog with pagination
    Page<BlogComment> findByBlogPost(Blog blogPost, Pageable pageable);
    
    // Count comments by blog
    long countByBlogPost(Blog blogPost);

    Page<BlogComment> findAllByIsApproved(boolean isApproved, Pageable pageable);

    Page<BlogComment> findAllByOrderByCreatedDateDesc(Pageable pageable);

    long countByIsApproved(boolean isApproved);

    /**
     * Count comments created within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return Count of comments created in the date range
     */
    @Query("SELECT COUNT(c) FROM BlogComment c WHERE c.createdDate BETWEEN :startDate AND :endDate")
    long countByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get comment creation trend data by period
     * @param startDate Start date
     * @param endDate End date
     * @return List of comment creation data grouped by date
     */
    @Query(value = "SELECT CAST(created_date AS DATE) as creation_date, COUNT(*) as count " +
            "FROM blog_comments " +
            "WHERE created_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(created_date AS DATE) " +
            "ORDER BY CAST(created_date AS DATE)",
            nativeQuery = true)
    List<Map<String, Object>> getCommentCreationTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
