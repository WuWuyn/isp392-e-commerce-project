package com.example.isp392.repository;

import com.example.isp392.model.Blog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {
    
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
