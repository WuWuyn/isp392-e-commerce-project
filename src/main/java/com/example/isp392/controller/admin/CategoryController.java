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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
@RequestMapping("/admin/category")
public class CategoryController {

    private final CategoryService categoryService;

    // Constructor injection
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Display the category management page
     * 
     * @param model Model to add attributes
     * @param page Page number (0-based)
     * @param size Page size
     * @param search Search term for category name
     * @param sortField Field to sort by
     * @param sortDir Sort direction (asc or desc)
     * @return category management view
     */
    @GetMapping
    public String showCategoryPage(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "categoryName") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir) {

        // Create sort object
        Sort sort = Sort.by(sortDir.equals("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortField);
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Get categories based on search or get all
        Page<Category> categoryPage;
        if (search != null && !search.isEmpty()) {
            categoryPage = categoryService.findByCategoryNameContainingIgnoreCase(search, pageable);
        } else {
            categoryPage = categoryService.findAll(pageable);
        }

        // Add attributes to model
        model.addAttribute("categoryPage", categoryPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("totalItems", categoryPage.getTotalElements());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");
        model.addAttribute("search", search != null ? search : "");
        
        // For pagination
        if (categoryPage.getTotalPages() > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, categoryPage.getTotalPages())
                    .boxed()
                    .collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        
        // Add an empty category for the form
        model.addAttribute("category", new Category());
        
        return "admin/product/category";
    }

    /**
     * Handle creation of a new category
     * 
     * @param category Category to create
     * @param bindingResult Validation result
     * @param redirectAttributes For flash messages
     * @return redirect to category list
     */
    @PostMapping
    public String createCategory(
            @ModelAttribute Category category,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid form data");
            return "redirect:/admin/category";
        }
        
        try {
            categoryService.save(category);
            redirectAttributes.addFlashAttribute("successMessage", "Category created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error creating category: " + e.getMessage());
        }
        
        return "redirect:/admin/category";
    }

    /**
     * Get a specific category by ID
     * 
     * @param id Category ID
     * @return Category object or null
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Category getCategoryById(@PathVariable Integer id) {
        return categoryService.findById(id).orElse(null);
    }

    /**
     * Update an existing category
     * 
     * @param id Category ID to update
     * @param category Updated category data
     * @param redirectAttributes For flash messages
     * @return redirect to category list
     */
    @PostMapping("/update/{id}")
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
        
        return "redirect:/admin/category";
    }

    /**
     * Delete a category
     * 
     * @param id Category ID to delete
     * @param redirectAttributes For flash messages
     * @return redirect to category list
     */
    @PostMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable Integer id,
            RedirectAttributes redirectAttributes) {
        
        try {
            categoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Category deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting category: " + e.getMessage());
        }
        
        return "redirect:/admin/category";
    }

    /**
     * Delete multiple categories
     * 
     * @param ids Array of category IDs to delete
     * @param redirectAttributes For flash messages
     * @return redirect to category list
     */
    @PostMapping("/delete-multiple")
    public String deleteMultipleCategories(
            @RequestParam("ids") Integer[] ids,
            RedirectAttributes redirectAttributes) {
        
        try {
            categoryService.deleteAllById(List.of(ids));
            redirectAttributes.addFlashAttribute("successMessage", "Categories deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting categories: " + e.getMessage());
        }
        
        return "redirect:/admin/category";
    }
}
