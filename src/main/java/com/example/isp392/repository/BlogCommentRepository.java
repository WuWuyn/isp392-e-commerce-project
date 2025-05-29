package com.example.isp392.repository;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Integer> {
    
    // Find comments by blog
    List<BlogComment> findByBlogPostOrderByCreatedDateDesc(Blog blogPost);
    
    // Find comments by blog with pagination
    Page<BlogComment> findByBlogPost(Blog blogPost, Pageable pageable);
    
    // Count comments by blog
    long countByBlogPost(Blog blogPost);
}
