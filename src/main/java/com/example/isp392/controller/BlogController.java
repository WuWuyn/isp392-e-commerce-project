package com.example.isp392.controller;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogComment;
import com.example.isp392.model.User;
import com.example.isp392.service.BlogCommentService;
import com.example.isp392.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/blog")
public class BlogController {

    private final BlogService blogService;
    private final BlogCommentService blogCommentService;

    @Autowired
    public BlogController(BlogService blogService, BlogCommentService blogCommentService) {
        this.blogService = blogService;
        this.blogCommentService = blogCommentService;
    }

    // Main blog page with filtering options
    @GetMapping
    public String listBlogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        // Default page size
        int size = 8;
        
        // Get blogs based on search and sort criteria
        Page<Blog> blogsPage;
        if (search != null && !search.isEmpty()) {
            blogsPage = blogService.searchBlogs(search, page, size);
            model.addAttribute("search", search);
        } else {
            blogsPage = blogService.getBlogsSorted(sort, page, size);
        }
        
        model.addAttribute("blogs", blogsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", blogsPage.getTotalPages());
        model.addAttribute("sort", sort);
        
        // If there are blogs, get the latest blog for the highlight section
        if (!blogsPage.isEmpty()) {
            model.addAttribute("latestBlog", blogsPage.getContent().get(0));
        }
        
        return "blog";
    }

    // Blog single page
    @GetMapping("/{blogId}")
    public String viewBlog(@PathVariable int blogId, Model model) {
        Optional<Blog> blogOpt = blogService.getBlogById(blogId);
        
        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();
            
            // Increment the view count
            blogService.incrementViewCount(blogId);
            
            // Add blog to model
            model.addAttribute("blog", blog);
            
            // Get previous and next blogs for navigation
            Blog previousBlog = blogService.getPreviousBlog(blogId);
            Blog nextBlog = blogService.getNextBlog(blogId);
            
            if (previousBlog != null) {
                model.addAttribute("previousBlog", previousBlog);
            }
            
            if (nextBlog != null) {
                model.addAttribute("nextBlog", nextBlog);
            }
            
            return "blog-single";
        } else {
            return "redirect:/blog";
        }
    }
    
    /**
     * Handle adding a comment to a blog post
     */
    @PostMapping("/{blogId}/comment")
    public String addComment(
            @PathVariable int blogId,
            @RequestParam String comment,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        
        // Check if blog exists
        Optional<Blog> blogOpt = blogService.getBlogById(blogId);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Blog post not found");
            return "redirect:/blog";
        }
        
        // Add the comment
        if (currentUser != null) {
            BlogComment savedComment = blogCommentService.addComment(blogId, comment, currentUser);
            if (savedComment != null) {
                redirectAttributes.addFlashAttribute("success", "Comment added successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to add comment");
            }
        } else {
            // Handle guest comments or redirect to login
            redirectAttributes.addFlashAttribute("error", "You must be logged in to comment");
        }
        
        return "redirect:/blog/" + blogId + "#comments";
    }
    
    /**
     * Handle deleting a comment
     */
    @PostMapping("/{blogId}/comment/{commentId}/delete")
    public String deleteComment(
            @PathVariable int blogId,
            @PathVariable int commentId,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        
        if (currentUser != null) {
            boolean deleted = blogCommentService.deleteComment(commentId, currentUser.getUserId());
            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Comment deleted successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "You don't have permission to delete this comment");
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to delete comments");
        }
        
        return "redirect:/blog/" + blogId + "#comments";
    }
}
