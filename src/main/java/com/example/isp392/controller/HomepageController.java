package com.example.isp392.controller;

import com.example.isp392.model.Blog;
import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.service.BlogService;
import com.example.isp392.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@Controller
public class HomepageController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final BlogService blogService;

    public HomepageController(BookService bookService, CategoryRepository categoryRepository, BlogService blogService) {
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.blogService = blogService;
    }

    @GetMapping("/")
    public String index(Model model){
        // Lấy danh sách các sách có đánh giá cao nhất (People's Choice)
        List<Book> topRatedBooks = bookService.getTopRatedBooks(5);
        model.addAttribute("topRatedBooks", topRatedBooks);

        // Lấy danh sách sách mới thêm (New Additions)
        List<Book> newAdditions = bookService.getNewAdditions(5);
        model.addAttribute("newAdditions", newAdditions);

        // Lấy danh sách các sách đang giảm giá
        List<Book> discountedBooks = bookService.getDiscountedBooks(5);
        model.addAttribute("discountedBooks", discountedBooks);

        // Lấy danh sách các danh mục
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        
        // Lấy danh sách bài blog mới nhất cho phần "From the Blog"
        Page<Blog> recentBlogs = blogService.getBlogsSorted("latest", 0, 4);
        model.addAttribute("recentBlogs", recentBlogs.getContent());

        return "home";
    }

    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String homepage(Model model){
        // Chuyển hướng đến trang index để tránh trùng lặp code
        return "redirect:/";
    }

    @RequestMapping(value = "/about-contact", method = RequestMethod.GET)
    public String aboutContact(){
        return "about-contact";
    }

    @RequestMapping(value = "/terms-policy", method = RequestMethod.GET)
    public String termsPolicy(){
        return "terms-policy";
    }
}
