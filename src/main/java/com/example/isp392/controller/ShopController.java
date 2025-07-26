package com.example.isp392.controller;

import com.example.isp392.model.Book;
import com.example.isp392.model.Shop;
import com.example.isp392.service.BookService;
import com.example.isp392.service.ShopService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ShopController {

    private final ShopService shopService;
    private final BookService bookService;

    public ShopController(ShopService shopService, BookService bookService) {
        this.shopService = shopService;
        this.bookService = bookService;
    }

    // Controller này không có @RequestMapping ở trên, nên URL sẽ là /shops/{shopId}
    @GetMapping("/shops/{shopId}")
    public String viewShopPage(@PathVariable("shopId") Integer shopId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "8") int size,
                               @RequestParam(required = false) String searchQuery, // Thêm tham số search
                               @RequestParam(defaultValue = "dateAdded") String sortField, // Thêm tham số sort
                               @RequestParam(defaultValue = "desc") String sortDir,   // Thêm tham số sort
                               Model model) {
        try {
            Shop shop = shopService.getShopById(shopId);

            // Tạo đối tượng Sort và Pageable
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortField);
            Pageable pageable = PageRequest.of(page, size, sort);

            // Gọi phương thức service mới
            Page<Book> bookPage = bookService.searchActiveBooksByShop(shopId, searchQuery, pageable);

            model.addAttribute("shop", shop);
            model.addAttribute("bookPage", bookPage);

            // Truyền tất cả các tham số ra view để giữ trạng thái
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", bookPage.getTotalPages());
            model.addAttribute("searchQuery", searchQuery);
            model.addAttribute("sortField", sortField);
            model.addAttribute("sortDir", sortDir);

            return "shop-view";

        } catch (RuntimeException e) {
            return "redirect:/";
        }
    }

}