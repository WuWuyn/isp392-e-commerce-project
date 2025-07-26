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
            // MODIFICATION: Add validation check for existing category name
            if (categoryService.existsByName(category.getCategoryName())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Error: A category with the name '" + category.getCategoryName() + "' already exists.");
                return "redirect:/admin/categories";
            }

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
            // MODIFICATION: Add @RequestParam to explicitly capture the 'active' checkbox value
            @RequestParam(name = "active", required = false) String activeValue,
            RedirectAttributes redirectAttributes) {

        try {
            Optional<Category> existingCategoryOpt = categoryService.findById(id);
            if (existingCategoryOpt.isPresent()) {
                Category existingCategory = existingCategoryOpt.get();

                // Manually update fields from the form
                existingCategory.setCategoryName(category.getCategoryName());
                existingCategory.setCategoryDescription(category.getCategoryDescription());

                // MODIFICATION: Check if the checkbox was ticked.
                // If activeValue is "on" (the default for a checked box) or "true", set active to true.
                // If it's null (unchecked), set active to false.
                existingCategory.setActive(activeValue != null && (activeValue.equals("on") || activeValue.equals("true")));

                categoryService.save(existingCategory);
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
   @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Category> categoryOpt = categoryService.findById(id);
            if (categoryOpt.isPresent()) {
                String categoryName = categoryOpt.get().getCategoryName();
                // Phương thức này giờ đã chứa logic kiểm tra
                categoryService.deleteById(id);
                redirectAttributes.addFlashAttribute("successMessage", "Category '" + categoryName + "' has been permanently deleted.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Category not found.");
            }
        } catch (IllegalStateException e) {
            // <<< BẮT LỖI CỤ THỂ TỪ SERVICE
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Giữ lại để bắt các lỗi không mong muốn khác
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please check system logs.");
        }
        return "redirect:/admin/categories";
    }
}
