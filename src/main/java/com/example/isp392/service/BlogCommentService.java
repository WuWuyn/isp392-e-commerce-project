package com.example.isp392.service;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogComment;
import com.example.isp392.model.User;
import com.example.isp392.repository.BlogCommentRepository;
import com.example.isp392.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BlogCommentService {

    private final BlogCommentRepository blogCommentRepository;
    private final BlogRepository blogRepository;

    @Autowired
    public BlogCommentService(BlogCommentRepository blogCommentRepository, BlogRepository blogRepository) {
        this.blogCommentRepository = blogCommentRepository;
        this.blogRepository = blogRepository;
    }

    /**
     * Get all comments for a blog post
     * @param blogId Blog ID
     * @return List of comments
     */
    public List<BlogComment> getCommentsByBlogId(int blogId) {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isPresent()) {
            return blogCommentRepository.findByBlogPostOrderByCreatedDateDesc(blogOpt.get());
        }
        return List.of();
    }

    /**
     * Get comments for a blog post with pagination
     * @param blogId Blog ID
     * @param page Page number
     * @param size Page size
     * @return Page of comments
     */
    public Page<BlogComment> getCommentsByBlogIdPaginated(int blogId, int page, int size) {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isPresent()) {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());
            return blogCommentRepository.findByBlogPost(blogOpt.get(), pageable);
        }
        return Page.empty();
    }

    /**
     * Add a comment to a blog post
     * @param blogId Blog ID
     * @param content Comment content
     * @param user User who made the comment
     * @return The created comment
     */
    @Transactional
    public BlogComment addComment(int blogId, String content, User user) {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();
            
            BlogComment comment = new BlogComment();
            comment.setBlogPost(blog);
            comment.setUser(user);
            comment.setContent(content);
            comment.setCreatedDate(LocalDateTime.now());
            
            return blogCommentRepository.save(comment);
        }
        return null;
    }

    /**
     * Delete a comment
     * @param commentId Comment ID
     * @param userId User ID (to ensure only the comment author or admin can delete)
     * @return True if comment was deleted
     */
    @Transactional
    public boolean deleteComment(int commentId, int userId) {
        Optional<BlogComment> commentOpt = blogCommentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            BlogComment comment = commentOpt.get();
            
            // Only the comment author or the blog author can delete a comment
            if (comment.getUser().getUserId() == userId || comment.getBlogPost().getUser().getUserId() == userId) {
                blogCommentRepository.delete(comment);
                return true;
            }
        }
        return false;
    }
}
