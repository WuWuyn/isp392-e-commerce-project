package com.example.isp392.service;

import com.example.isp392.model.Category;
import com.example.isp392.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Constructor injection (as per memory guideline)
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Find all categories
     * @return List of all categories
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    /**
     * Find all categories with pagination
     * @param pageable Pagination information
     * @return Page of categories
     */
    public Page<Category> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable);
    }

    /**
     * Find category by ID
     * @param id Category ID
     * @return Optional containing category if found
     */
    public Optional<Category> findById(Integer id) {
        return categoryRepository.findById(id);
    }

    /**
     * Save a category (create or update)
     * @param category Category to save
     * @return Saved category
     */
    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    /**
     * Delete a category by ID
     * @param id Category ID to delete
     */
    public void deleteById(Integer id) {
        categoryRepository.deleteById(id);
    }

    /**
     * Delete multiple categories by their IDs
     * @param ids List of category IDs to delete
     */
    public void deleteAllById(Iterable<Integer> ids) {
        categoryRepository.deleteAllById(ids);
    }

    /**
     * Find categories containing the name (case insensitive) with pagination
     * @param categoryName Category name to search for
     * @param pageable Pagination information
     * @return Page of matching categories
     */
    public Page<Category> findByCategoryNameContainingIgnoreCase(String categoryName, Pageable pageable) {
        return categoryRepository.findByCategoryNameContainingIgnoreCase(categoryName, pageable);
    }
}
