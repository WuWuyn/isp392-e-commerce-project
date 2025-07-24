package com.example.isp392.controller;

import com.example.isp392.model.Blog;
import com.example.isp392.model.BlogComment;
import com.example.isp392.model.User;
import com.example.isp392.service.BlogCommentService;
import com.example.isp392.service.BlogService;
import com.example.isp392.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.AccessDeniedException;
import com.example.isp392.service.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Optional;

@Controller
@RequestMapping("/blog")
public class BlogController {

    private final BlogService blogService;
    private final BlogCommentService blogCommentService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    @Autowired
    public BlogController(BlogService blogService,
                          BlogCommentService blogCommentService,
                          UserService userService, FileStorageService fileStorageService) {
        this.blogService = blogService;
        this.blogCommentService = blogCommentService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
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

    /**
     * Show the form for creating a new blog post.
     */
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("blog", new Blog());
        return "blog-create";
    }

    /**
     * Xử lý việc tạo blog mới.
     * @AuthenticationPrincipal sẽ luôn có giá trị ở đây.
     */
    @PostMapping("/create")
    public String processCreateBlog(@ModelAttribute("blog") Blog blog,
                                    @RequestParam("imageFile") MultipartFile imageFile,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to create a post.");
            return "redirect:/buyer/login";
        }

        try {
            String email = userDetails.getUsername();
            User fullCurrentUser = userService.findByEmailDirectly(email);

            if (fullCurrentUser == null) {
                throw new Exception("Authenticated user not found in the database.");
            }

            // 👇 Lưu file ảnh nếu có
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(imageFile, "blogs");
                blog.setImageUrl(imageUrl);
            }

            // 👇 Set các thông tin cần thiết
            blog.setUser(fullCurrentUser);
            blog.setCreatedDate(java.time.LocalDateTime.now());
            blog.setViewsCount(0);
            blog.setLocked(false);

            // 👇 Lưu vào DB
            Blog savedBlog = blogService.save(blog); // Nếu bạn có blogRepository.save(blog)

            redirectAttributes.addFlashAttribute("success", "Blog post created successfully!");
            return "redirect:/blog/" + savedBlog.getBlogId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating blog post: " + e.getMessage());
            return "redirect:/blog/create";
        }
    }


    // Blog single page
    // Trong file controller/BlogController.java

    @GetMapping("/{blogId}")
    public String viewBlog(@PathVariable int blogId,
                           Model model,
                           @AuthenticationPrincipal UserDetails userDetails) {

        Optional<Blog> blogOpt = blogService.getBlogById(blogId);

        if (blogOpt.isPresent()) {
            Blog blog = blogOpt.get();
            blogService.incrementViewCount(blogId);
            model.addAttribute("blog", blog);

            // ================= THÊM LOGIC MỚI TẠI ĐÂY =================
            // Kiểm tra xem người dùng đã đăng nhập chưa và gửi thông tin sang view
            if (userDetails != null) {
                User currentUser = userService.findByEmailDirectly(userDetails.getUsername());
                model.addAttribute("currentUser", currentUser);
            }
            // ==========================================================

            // ... phần code lấy previousBlog và nextBlog giữ nguyên ...
            Blog previousBlog = blogService.getPreviousBlog(blogId);
            Blog nextBlog = blogService.getNextBlog(blogId);
            if (previousBlog != null) { model.addAttribute("previousBlog", previousBlog); }
            if (nextBlog != null) { model.addAttribute("nextBlog", nextBlog); }

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
            @AuthenticationPrincipal UserDetails userDetails, // Sửa ở đây: Dùng UserDetails
            RedirectAttributes redirectAttributes) {

        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to comment.");
            return "redirect:/buyer/login";
        }

        // Kiểm tra xem bài viết có tồn tại không
        Optional<Blog> blogOpt = blogService.getBlogById(blogId);
        if (blogOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Blog post not found.");
            return "redirect:/blog";
        }

        try {
            // Lấy thông tin User đầy đủ từ email
            String email = userDetails.getUsername();
            User currentUser = userService.findByEmailDirectly(email);

            if (currentUser == null) {
                throw new Exception("Authenticated user could not be found in the database.");
            }

            // Thêm bình luận với đối tượng User đầy đủ
            BlogComment savedComment = blogCommentService.addComment(blogId, comment, currentUser);

            if (savedComment != null) {
                redirectAttributes.addFlashAttribute("success", "Comment added successfully!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Failed to add comment.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while adding your comment: " + e.getMessage());
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
            @AuthenticationPrincipal UserDetails userDetails, // <-- SỬA Ở ĐÂY: Dùng UserDetails
            RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra xem userDetails có tồn tại không (người dùng đã đăng nhập)
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to delete comments.");
            return "redirect:/buyer/login";
        }

        try {
            // 2. Lấy thông tin User đầy đủ từ database
            String email = userDetails.getUsername();
            User currentUser = userService.findByEmailDirectly(email);

            if (currentUser == null) {
                throw new IllegalStateException("Authenticated user could not be found in the database.");
            }

            // 3. Gọi service để thực hiện xóa với ID của người dùng
            boolean deleted = blogCommentService.deleteComment(commentId, currentUser.getUserId());

            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Comment deleted successfully!");
            } else {
                // Lỗi này xảy ra khi service trả về false (không có quyền xóa)
                redirectAttributes.addFlashAttribute("error", "You do not have permission to delete this comment.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred: " + e.getMessage());
        }

        return "redirect:/blog/" + blogId + "#comments";
    }

    @GetMapping("/edit/{blogId}")
    public String showEditForm(@PathVariable int blogId,
                               Model model,
                               @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return "redirect:/buyer/login";
        }

        // Dùng email để lấy thông tin User đầy đủ
        String email = userDetails.getUsername();
        User currentUser = userService.findByEmailDirectly(email);

        Optional<Blog> blogOpt = blogService.getBlogById(blogId);
        if (blogOpt.isEmpty()) {
            return "redirect:/blog";
        }
        Blog blog = blogOpt.get();

        try {
            boolean isAdmin = currentUser.getUserRoles().stream()
                    .anyMatch(userRole -> userRole.getRole().getRoleName().equals("ADMIN"));

            if (!blog.getUser().getUserId().equals(currentUser.getUserId()) && !isAdmin) {
                return "error/403";
            }
        } catch (Exception e) {
            return "error/403";
        }

        model.addAttribute("blog", blog);
        return "blog-edit";
    }

    /**
     * Xử lý việc cập nhật một bài blog.
     */
    @PostMapping("/edit/{blogId}")
    public String processEditBlog(@PathVariable int blogId,
                                  @ModelAttribute("blog") Blog blog,
                                  @RequestParam("imageFile") MultipartFile imageFile,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  RedirectAttributes redirectAttributes) {

        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "Your session may have expired. Please log in again.");
            return "redirect:/buyer/login";
        }

        try {
            String email = userDetails.getUsername();
            User currentUser = userService.findByEmailDirectly(email);

            if (currentUser == null) {
                throw new Exception("Could not find the authenticated user in the database.");
            }

            // Lấy blog cũ từ DB để giữ lại các thông tin như createdDate, viewsCount, imageUrl...
            Blog existingBlog = blogService.findById(blogId);
            if (existingBlog == null) {
                throw new Exception("Blog post not found.");
            }

            // Cập nhật các trường có thể thay đổi
            existingBlog.setTitle(blog.getTitle());
            existingBlog.setContent(blog.getContent());
            existingBlog.setUser(currentUser); // vẫn gán lại user

            // Nếu người dùng upload ảnh mới, lưu ảnh và cập nhật imageUrl
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageUrl = fileStorageService.storeFile(imageFile, "blogs");
                existingBlog.setImageUrl(imageUrl);
            }

            blogService.save(existingBlog); // hoặc blogRepository.save(existingBlog)

            redirectAttributes.addFlashAttribute("success", "Blog post updated successfully!");
            return "redirect:/blog/" + blogId;

        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to perform this action.");
            return "redirect:/blog/" + blogId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating blog post: " + e.getMessage());
            return "redirect:/blog/edit/" + blogId;
        }
    }


    /**
     * Xử lý việc xóa một bài blog.
     */
    @PostMapping("/delete/{blogId}")
    public String processDeleteBlog(@PathVariable int blogId,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            redirectAttributes.addFlashAttribute("error", "You must be logged in to perform this action.");
            return "redirect:/buyer/login";
        }

        try {
            // Lấy email từ UserDetails
            String email = userDetails.getUsername();
            User currentUser = userService.findByEmailDirectly(email);

            // Kiểm tra lại để chắc chắn
            if (currentUser == null) {
                throw new Exception("Authenticated user not found in the database.");
            }

            // Gọi service với đối tượng User đầy đủ thông tin
            blogService.deleteBlog(blogId, currentUser);

            redirectAttributes.addFlashAttribute("success", "Blog post deleted successfully!");
            return "redirect:/blog";
        } catch (AccessDeniedException e) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to perform this action.");
            return "redirect:/blog";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting blog post: " + e.getMessage());
            return "redirect:/blog";
        }
    }
}