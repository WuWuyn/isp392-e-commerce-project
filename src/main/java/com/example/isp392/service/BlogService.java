package com.example.isp392.service;

import com.example.isp392.model.Blog;
import com.example.isp392.model.User;
import com.example.isp392.repository.BlogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

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
        // 1. Tạo Specification để lọc các bài viết chưa bị khóa
        Specification<Blog> spec = (root, query, cb) -> cb.isFalse(root.get("isLocked"));

        // 2. Xác định tiêu chí sắp xếp
        Sort sort;
        switch (sortOption) {
            case "latest":
                sort = Sort.by(Sort.Direction.DESC, "createdDate");
                break;
            case "oldest":
                sort = Sort.by(Sort.Direction.ASC, "createdDate");
                break;
            case "popular":
                sort = Sort.by(Sort.Direction.DESC, "viewsCount");
                break;
            default:
                sort = Sort.by(Sort.Direction.DESC, "createdDate"); // Mặc định mới nhất
                break;
        }

        // 3. Tạo Pageable với sắp xếp
        Pageable pageable = PageRequest.of(page, size, sort);

        // 4. Thực hiện truy vấn với Specification và Pageable
        Page<Blog> blogs = blogRepository.findAll(spec, pageable);

        // Chủ động tải thông tin user để tránh lỗi LazyInitializationException (giữ nguyên)
        blogs.getContent().forEach(blog -> {
            if (blog.getUser() != null) {
                blog.getUser().getFullName();
            }
        });

        return blogs;
    }

    // Get blogs by search term

    public Page<Blog> searchBlogs(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));

        // Specification để lọc bài chưa bị khóa
        Specification<Blog> notLockedSpec = (root, query, cb) -> cb.isFalse(root.get("isLocked"));

        // Specification để tìm kiếm theo title hoặc content
        Specification<Blog> searchSpec = (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("title")), "%" + searchTerm.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("content")), "%" + searchTerm.toLowerCase() + "%")
                );

        // Kết hợp hai điều kiện: (chưa bị khóa) AND (thỏa mãn tìm kiếm)
        Specification<Blog> finalSpec = notLockedSpec.and(searchSpec);

        return blogRepository.findAll(finalSpec, pageable);
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

    @Transactional(readOnly = true)
    public Page<Blog> findBlogsForAdmin(String keyword, String status, Pageable pageable) {
        Specification<Blog> spec = (root, query, cb) -> cb.conjunction(); // Base spec (always true)

        if (StringUtils.hasText(keyword)) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
        }

        if (StringUtils.hasText(status)) {
            if ("pinned".equalsIgnoreCase(status)) {
                spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isPinned")));
            } else if ("locked".equalsIgnoreCase(status)) {
                spec = spec.and((root, query, cb) -> cb.isTrue(root.get("isLocked")));
            }
        }
        return blogRepository.findAll(spec, pageable);
    }


    /**
     * Cập nhật bài viết từ trang Admin.
     */
    @Transactional
    public Blog updateBlog(Integer blogId, Blog blogFromForm) {
        Blog existingBlog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id: " + blogId));

        existingBlog.setTitle(blogFromForm.getTitle());
        existingBlog.setContent(blogFromForm.getContent());
        // Có thể cập nhật các trường khác ở đây nếu form có
        return blogRepository.save(existingBlog);
    }

    /**
     * Xóa bài viết bởi admin.
     */
    @Transactional
    public void deleteBlogById(Integer blogId) {
        if (!blogRepository.existsById(blogId)) {
            throw new EntityNotFoundException("Blog not found with id: " + blogId);
        }
        blogRepository.deleteById(blogId);
    }

    /**
     * Thay đổi trạng thái ghim (pin) của bài viết.
     */
    @Transactional
    public boolean togglePinStatus(Integer blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id: " + blogId));
        blog.setPinned(!blog.isPinned());
        blogRepository.save(blog);
        return blog.isPinned();
    }

    /**
     * Thay đổi trạng thái khóa (lock) của bài viết.
     */
    @Transactional
    public boolean toggleLockStatus(Integer blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id: " + blogId));
        blog.setLocked(!blog.isLocked());
        blogRepository.save(blog);
        return blog.isLocked();
    }
    @Transactional
    public Blog save(Blog blog) {
        return blogRepository.save(blog);
    }
    @Transactional(readOnly = true)
    public Blog findById(int blogId) {
        return blogRepository.findById(blogId)
                .orElseThrow(() -> new EntityNotFoundException("Blog not found with id: " + blogId));
    }
}