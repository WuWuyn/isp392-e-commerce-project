package com.example.isp392.repository;

import com.example.isp392.model.Blog;
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
public interface BlogRepository extends JpaRepository<Blog, Integer>, JpaSpecificationExecutor<Blog> {
    
    // Find blogs containing the search term in title or content
    @Query("SELECT b FROM Blog b WHERE b.title LIKE %:searchTerm% OR b.content LIKE %:searchTerm%")
    Page<Blog> findByTitleOrContentContaining(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Find blogs by locked status
    Page<Blog> findByIsLocked(boolean isLocked, Pageable pageable);
    
    // Find latest blogs
    Page<Blog> findAllByOrderByCreatedDateDesc(Pageable pageable);
    
    // Find oldest blogs
    Page<Blog> findAllByOrderByCreatedDateAsc(Pageable pageable);
    
    // Find most popular blogs (by views)
    Page<Blog> findAllByOrderByViewsCountDesc(Pageable pageable);

    /**
     * Count blogs created within a date range
     * @param startDate Start date
     * @param endDate End date
     * @return Count of blogs created in the date range
     */
    @Query("SELECT COUNT(b) FROM Blog b WHERE b.createdDate BETWEEN :startDate AND :endDate")
    long countByCreatedDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get blog creation trend data by period
     * @param startDate Start date
     * @param endDate End date
     * @return List of blog creation data grouped by date
     */
    @Query(value = "SELECT CAST(created_date AS DATE) as creation_date, COUNT(*) as count " +
            "FROM blogs " +
            "WHERE created_date BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(created_date AS DATE) " +
            "ORDER BY CAST(created_date AS DATE)",
            nativeQuery = true)
    List<Map<String, Object>> getBlogCreationTrend(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get total blog views
     * @return Total views across all blogs
     */
    @Query(value = "SELECT COALESCE(SUM(views_count), 0) FROM blogs", nativeQuery = true)
    long getTotalBlogViews();

    /**
     * Get forum activity statistics
     * @param startDate Start date
     * @param endDate End date
     * @return Forum activity statistics
     */
    @Query(value = "SELECT " +
            "COUNT(DISTINCT b.blog_id) as total_posts, " +
            "COUNT(DISTINCT b.user_id) as active_users, " +
            "COALESCE(SUM(b.views_count), 0) as total_views, " +
            "COUNT(DISTINCT bc.comment_id) as total_comments " +
            "FROM blogs b " +
            "LEFT JOIN blog_comments bc ON b.blog_id = bc.blog_id " +
            "WHERE b.created_date BETWEEN :startDate AND :endDate",
            nativeQuery = true)
    Map<String, Object> getForumActivityStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find blogs by author (user)
    Page<Blog> findByUserEmail(String email, Pageable pageable);
    
    // Get blog with next ID for navigation
    @Query("SELECT b FROM Blog b WHERE b.blogId > :blogId ORDER BY b.blogId ASC")
    List<Blog> findNextBlog(@Param("blogId") int blogId, Pageable pageable);
    
    // Get blog with previous ID for navigation
    @Query("SELECT b FROM Blog b WHERE b.blogId < :blogId ORDER BY b.blogId DESC")
    List<Blog> findPreviousBlog(@Param("blogId") int blogId, Pageable pageable);
    Page<Blog> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
