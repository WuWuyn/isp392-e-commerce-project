package com.example.isp392.service;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogCategory;
import com.example.isp392.model.User;
import com.example.isp392.repository.BlogCategoryRepository;
import com.example.isp392.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BlogService {
    private static final Logger log = LoggerFactory.getLogger(BlogService.class);
    private final BlogRepository blogRepository;

    public BlogService(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
    }

    /**
     * Creates and saves a new blog post.
     * @param title The title of the blog.
     * @param content The content of the blog.
     * @param author The user creating the blog.
     * @return The saved Blog entity.
     */
    @Transactional
    public Blog createBlog(String title, String content, User author) {
        Blog newBlog = new Blog();
        newBlog.setTitle(title);
        newBlog.setContent(content);
        newBlog.setUser(author);

        // Set default values for new posts
        newBlog.setCreatedDate(LocalDateTime.now());
        newBlog.setViewsCount(0);
        newBlog.setLocked(false);

        return blogRepository.save(newBlog);
    }



    // Get all blogs with pagination
    public Page<Blog> getAllBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogRepository.findAll(pageable);
    }

    // Get blogs based on sort option
    @Transactional(readOnly = true)
    public Page<Blog> getBlogsSorted(String sortOption, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Blog> blogs;
        switch (sortOption) {
            case "latest":
                blogs = blogRepository.findAllByOrderByCreatedDateDesc(pageable);
                break;
            case "oldest":
                blogs = blogRepository.findAllByOrderByCreatedDateAsc(pageable);
                break;
            case "popular":
                blogs = blogRepository.findAllByOrderByViewsCountDesc(pageable);
                break;
            default:
                blogs = blogRepository.findAll(pageable);
                break;
        }

        // Chủ động tải thông tin user để tránh lỗi LazyInitializationException
        blogs.getContent().forEach(blog -> {
            if (blog.getUser() != null) {
                // Chủ động truy cập để đảm bảo Hibernate tải dữ liệu
                blog.getUser().getFullName();
            }
        });

        return blogs;
    }

    // Get blogs by search term
    public Page<Blog> searchBlogs(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return blogRepository.findByTitleOrContentContaining(searchTerm, pageable);
    }

    // Get blog by ID
    public Optional<Blog> getBlogById(int id) {
        return blogRepository.findById(id);
    }

    // Increment blog view count
    @Transactional
    public void incrementViewCount(int blogId) {
        Optional<Blog> blogOpt = blogRepository.findById(blogId);
        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();
            blog.setViewsCount(blog.getViewsCount() + 1);
            blogRepository.save(blog);
        }
    }

    // Get next blog for navigation
    public Blog getNextBlog(int currentBlogId) {
        List<Blog> nextBlogs = blogRepository.findNextBlog(currentBlogId, PageRequest.of(0, 1));
        return nextBlogs.isEmpty() ? null : nextBlogs.get(0);
    }

    // Get previous blog for navigation
    public Blog getPreviousBlog(int currentBlogId) {
        List<Blog> previousBlogs = blogRepository.findPreviousBlog(currentBlogId, PageRequest.of(0, 1));
        return previousBlogs.isEmpty() ? null : previousBlogs.get(0);
    }

    @Transactional
    public Blog updateBlog(Integer blogId, String title, String content, User currentUser) {
        Blog blogToUpdate = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + blogId));

        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getRoleName().equals("ADMIN"));

        if (!blogToUpdate.getUser().getUserId().equals(currentUser.getUserId()) && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to edit this post.");
        }

        blogToUpdate.setTitle(title);
        blogToUpdate.setContent(content);

        return blogRepository.save(blogToUpdate);
    }

    /**
     * Deletes a blog post.
     * @param blogId The ID of the blog to delete.
     * @param currentUser The user attempting the deletion.
     * @throws AccessDeniedException if the user is not the author or an admin.
     */
    @Transactional
    public void deleteBlog(Integer blogId, User currentUser) {
        Blog blogToDelete = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + blogId));
        boolean isAdmin = currentUser.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getRoleName().equals("ADMIN"));

        if (!blogToDelete.getUser().getUserId().equals(currentUser.getUserId()) && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to delete this post.");
        }

        blogRepository.delete(blogToDelete);
    }

    @Autowired
    private BlogCategoryRepository blogCategoryRepository; // Inject the new repository

    // Method to get all blogs for admin view with pagination and search
    @Transactional(readOnly = true)
    public Page<Blog> getAllBlogsForAdmin(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return blogRepository.findByTitleContainingIgnoreCase(keyword, pageable);
        }
        return blogRepository.findAll(pageable);
    }

    // Method to Pin/Unpin a blog post
    @Transactional
    public Blog togglePinStatus(int blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + blogId));
        blog.setPinned(!blog.isPinned());
        return blogRepository.save(blog);
    }

    // Method to Lock/Unlock a blog post
    @Transactional
    public Blog toggleLockStatus(int blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + blogId));
        blog.setLocked(!blog.isLocked());
        return blogRepository.save(blog);
    }


    // Method to move a blog to a different category
    @Transactional
    public Blog moveBlogCategory(int blogId, Integer newCategoryId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog not found with id: " + blogId));
        BlogCategory newCategory = blogCategoryRepository.findById(newCategoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + newCategoryId));
        blog.setCategory(newCategory);
        return blogRepository.save(blog);
    }

// Admin can delete any post (re-using existing delete logic but ensuring it's called from an admin context)
// The existing deleteBlog(Integer blogId, User currentUser) already supports admins, which is great.

    // Admin can edit any post
    @Transactional
    public Blog updateBlogByAdmin(Integer blogId, String title, String content) {
        Blog blogToUpdate = blogRepository.findById(blogId)
                .orElseThrow(() -> new RuntimeException("Blog post not found with id: " + blogId));

        blogToUpdate.setTitle(title);
        blogToUpdate.setContent(content);

        return blogRepository.save(blogToUpdate);
    }
}