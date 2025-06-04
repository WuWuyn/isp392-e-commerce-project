package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.BookReview;
import com.example.isp392.model.Category;
import com.example.isp392.model.Publisher;
import com.example.isp392.repository.BookReviewRepository;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class ProductController {

    private final BookService bookService;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final BookReviewRepository bookReviewRepository;

    public ProductController(BookService bookService, CategoryRepository categoryRepository, 
                            PublisherRepository publisherRepository, BookReviewRepository bookReviewRepository) {
        this.bookService = bookService;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.bookReviewRepository = bookReviewRepository;
    }

    @RequestMapping(value = "/all-category", method = RequestMethod.GET)
    public String listProduct(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "dateAdded") String sortField,
            @RequestParam(name = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(name = "search", required = false) String searchQuery,
            @RequestParam(name = "category", required = false) List<Integer> categoryIds,
            @RequestParam(name = "publisher", required = false) List<Integer> publisherIds,
            @RequestParam(name = "minPrice", required = false) BigInteger minPrice,
            @RequestParam(name = "maxPrice", required = false) BigInteger maxPrice,
            @RequestParam(name = "priceRange", required = false) List<String> priceRanges,
            @RequestParam(name = "rating", required = false) Float minRating,
            Model model) {
        
        // Process price ranges if selected
        if (priceRanges != null && !priceRanges.isEmpty()) {
            for (String range : priceRanges) {
                switch (range) {
                    case "range1" -> {
                        // <50.000đ
                        minPrice = minPrice == null ? BigInteger.ZERO : minPrice;
                        maxPrice = BigInteger.valueOf(50000);
                    }
                    case "range2" -> {
                        // 50.000đ - 200.000đ
                        minPrice = BigInteger.valueOf(50000);
                        maxPrice = BigInteger.valueOf(200000);
                    }
                    case "range3" -> {
                        // 200.000đ - 500.000đ
                        minPrice = BigInteger.valueOf(200000);
                        maxPrice = BigInteger.valueOf(500000);
                    }
                    case "range4" -> {
                        // >500.000đ
                        minPrice = BigInteger.valueOf(500000);
                        maxPrice = null; // No upper limit
                    }
                }
            }
        }
        
        // Fetch books with all the filters
        Page<Book> books = bookService.findBooks(
                searchQuery, 
                categoryIds, 
                publisherIds, 
                minPrice, 
                maxPrice, 
                minRating, 
                page, 
                size, 
                sortField, 
                sortDirection);
        
        // Fetch active categories and all publishers for filter options
        List<Category> allCategories = categoryRepository.findByIsActiveTrue();
        List<Publisher> allPublishers = publisherRepository.findAll();
        
        // Add data to the model
        model.addAttribute("books", books);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("allPublishers", allPublishers);
        
        // Add pagination info
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", books.getTotalPages());
        model.addAttribute("totalItems", books.getTotalElements());
        
        // Add filter parameters to maintain state
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("selectedCategoryIds", categoryIds != null ? categoryIds : new ArrayList<>());
        model.addAttribute("selectedPublisherIds", publisherIds != null ? publisherIds : new ArrayList<>());
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("selectedPriceRanges", priceRanges != null ? priceRanges : new ArrayList<>());
        model.addAttribute("minRating", minRating);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDirection", sortDirection);
        
        return "all-category";
    }

    @GetMapping("/product-detail")
    public String detailProduct(
            @RequestParam("book_id") int bookId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            @RequestParam(value = "sort", defaultValue = "createdDate") String sortField,
            @RequestParam(value = "direction", defaultValue = "DESC") String sortDirection,
            @RequestParam(value = "minRating", required = false) Integer minRating,
            Model model) {
        
        // Lấy thông tin chi tiết sách từ cơ sở dữ liệu
        Optional<Book> bookOptional = bookService.getBookById(bookId);
        
        if (bookOptional.isPresent()) {
            Book book = bookOptional.get();
            model.addAttribute("book", book);
            
            // Lấy các sách liên quan (cùng danh mục)
            if (book.getCategories() != null && !book.getCategories().isEmpty()) {
                Category firstCategory = book.getCategories().iterator().next();
                List<Book> relatedBooks = bookService.findBooksByCategory(firstCategory, 0, 5, "dateAdded", "DESC")
                        .getContent();
                model.addAttribute("relatedBooks", relatedBooks);
            }
            
            // Lấy đánh giá sách từ cơ sở dữ liệu
            Page<BookReview> bookReviews;
            Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lọc theo rating nếu có
            if (minRating != null && minRating > 0) {
                bookReviews = bookReviewRepository.findByBookIdAndRatingGreaterThanEqual(bookId, minRating, pageable);
            } else {
                bookReviews = bookReviewRepository.findByBookId(bookId, pageable);
            }
            
            // Đếm tổng số đánh giá
            long totalReviews = bookReviewRepository.countByBookId(bookId);
            
            model.addAttribute("bookReviews", bookReviews);
            model.addAttribute("totalReviews", totalReviews);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookReviews.getTotalPages());
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortDirection", sortDirection);
            model.addAttribute("minRating", minRating);
            
            return "product-detail";
        } else {
            // Nếu không tìm thấy sách, chuyển hướng về trang chủ
            return "redirect:/";
        }
    }
}
