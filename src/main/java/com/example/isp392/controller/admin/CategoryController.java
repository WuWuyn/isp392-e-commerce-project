package com.example.isp392.controller.admin;

import com.example.isp392.dto.CategoryDTO;
import com.example.isp392.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String listCategories(
            Model model,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly) {

        List<CategoryDTO> categories = categoryService.getAllCategories(search, activeOnly);
        model.addAttribute("categories", categories);
        model.addAttribute("activeMenu", "categories");
        model.addAttribute("search", search);
        model.addAttribute("activeOnly", activeOnly);
        return "admin/categories";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<CategoryDTO> getCategory(@PathVariable Integer id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @PostMapping
    public String saveCategory(@Valid @ModelAttribute CategoryDTO categoryDTO,
                               RedirectAttributes redirectAttributes) {
        try {
            if (categoryDTO.getCategoryId() == null) {
                categoryService.createCategory(categoryDTO);
                redirectAttributes.addFlashAttribute("successMessage", "Category created successfully!");
            } else {
                categoryService.updateCategory(categoryDTO.getCategoryId(), categoryDTO);
                redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/disable")
    public String disableCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.disableCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category disabled successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/activate")
    public String activateCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.activateCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category activated successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
