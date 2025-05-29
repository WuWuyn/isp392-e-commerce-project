package com.example.isp392.service;

import com.example.isp392.model.Blog;
import com.example.isp392.repository.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BlogService {

    private final BlogRepository blogRepository;

    @Autowired
    public BlogService(BlogRepository blogRepository) {
        this.blogRepository = blogRepository;
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
}
