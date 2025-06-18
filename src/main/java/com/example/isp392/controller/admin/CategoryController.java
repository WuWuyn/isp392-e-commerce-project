package com.example.isp392.controller.admin;

import com.example.isp392.model.Category;
import com.example.isp392.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Display the category management page with filtering, sorting and pagination
     */
    @GetMapping
    public String showCategories(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "categoryName") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(required = false) String status) {

        // Create sort object
        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        String actualSortField = sort;
        // Explicitly set 'name' to 'categoryName' if it comes through
        if ("name".equalsIgnoreCase(sort)) {
            actualSortField = "categoryName";
        }
        
        // Validate and fallback to a guaranteed existing field if the provided one is not explicitly mapped or invalid
        List<String> validSortFields = Arrays.asList("categoryId", "categoryName", "categoryDescription", "isActive");
        if (!validSortFields.contains(actualSortField)) {
            actualSortField = "categoryName"; // Fallback to a guaranteed existing field
        }

        Sort sorting = Sort.by(sortDirection, actualSortField);
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sorting);
        
        // Get categories based on search and status
        Page<Category> categories;
        if (search != null && !search.isEmpty()) {
            if (status != null && !status.equals("all")) {
                boolean isActive = status.equals("active");
                categories = categoryService.findByNameContainingAndActive(search, isActive, pageable);
            } else {
                categories = categoryService.findByNameContaining(search, pageable);
            }
        } else if (status != null && !status.equals("all")) {
            boolean isActive = status.equals("active");
            categories = categoryService.findByActive(isActive, pageable);
        } else {
            categories = categoryService.findAll(pageable);
        }

        // Add attributes to model
        model.addAttribute("categories", categories);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categories.getTotalPages());
        
        return "admin/category";
    }

    /**
     * Show category details page
     */
    @GetMapping("/{id}")
    public String showCategoryDetails(@PathVariable Integer id, Model model) {
        Optional<Category> category = categoryService.findById(id);
        if (category.isEmpty()) {
            return "redirect:/admin/categories";
        }
        
        model.addAttribute("category", category.get());
        return "admin/category-details";
    }

    /**
     * Create a new category
     */
    @PostMapping
    public String createCategory(
            @ModelAttribute Category category,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please check your input");
            return "redirect:/admin/categories";
        }
        
        try {
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating category: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }

    /**
     * Update an existing category
     */
    @PostMapping("/{id}")
    public String updateCategory(
            @PathVariable Integer id,
            @ModelAttribute Category category,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Category> existingCategory = categoryService.findById(id);
            if (existingCategory.isPresent()) {
                category.setCategoryId(id);
                categoryService.save(category);
                redirectAttributes.addFlashAttribute("successMessage", "Category updated successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Category not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating category: " + e.getMessage());
        }
        
        return "redirect:/admin/categories/" + id;
    }

    /**
     * Toggle category status
     */
    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        
        try {
            Optional<Category> category = categoryService.findById(id);
            if (category.isPresent()) {
                Category existingCategory = category.get();
                existingCategory.setActive(!existingCategory.isActive());
                categoryService.save(existingCategory);
                
                String status = existingCategory.isActive() ? "activated" : "deactivated";
                redirectAttributes.addFlashAttribute("successMessage", "Category " + status + " successfully");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Category not found");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating category status: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }

    /**
     * Bulk action on categories (activate/deactivate)
     */
    @PostMapping("/bulk-action")
    public String bulkAction(
            @RequestParam List<Integer> selectedIds,
            @RequestParam String action,
            RedirectAttributes redirectAttributes) {
        
        if (selectedIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No categories selected");
            return "redirect:/admin/categories";
        }
        
        try {
            boolean activate = action.equals("activate");
            int count = categoryService.toggleActiveMultiple(selectedIds, activate);
            
            String status = activate ? "activated" : "deactivated";
            redirectAttributes.addFlashAttribute("successMessage", 
                count + " categories " + status + " successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Error performing bulk action: " + e.getMessage());
        }
        
        return "redirect:/admin/categories";
    }
}
