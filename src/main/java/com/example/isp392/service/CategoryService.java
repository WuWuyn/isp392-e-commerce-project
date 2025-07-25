package com.example.isp392.service;

import com.example.isp392.model.Category;
import com.example.isp392.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * Get all categories (alias for findAll for consistency)
     * @return List of all categories
     */
    public List<Category> getAllCategories() {
        return findAll();
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
     * Find categories by name containing (case insensitive)
     * @param name Category name to search for
     * @param pageable Pagination information
     * @return Page of matching categories
     */
    public Page<Category> findByNameContaining(String name, Pageable pageable) {
        return categoryRepository.findByCategoryNameContainingIgnoreCase(name, pageable);
    }

    /**
     * Find categories by name containing and active status
     * @param name Category name to search for
     * @param isActive Active status to filter by
     * @param pageable Pagination information
     * @return Page of matching categories
     */
    public Page<Category> findByNameContainingAndActive(String name, boolean isActive, Pageable pageable) {
        return categoryRepository.findByCategoryNameContainingIgnoreCaseAndIsActive(name, isActive, pageable);
    }

    /**
     * Find categories by active status
     * @param isActive Active status to filter by
     * @param pageable Pagination information
     * @return Page of matching categories
     */
    public Page<Category> findByActive(boolean isActive, Pageable pageable) {
        return categoryRepository.findByIsActive(isActive, pageable);
    }
    
    /**
     * Find all active categories
     * @return List of all active categories
     */
    public List<Category> findAllActive() {
        return categoryRepository.findByIsActiveTrue();
    }
    
    /**
     * Find all active categories with pagination
     * @param pageable Pagination information
     * @return Page of active categories
     */
    public Page<Category> findAllActive(Pageable pageable) {
        return categoryRepository.findByIsActiveTrue(pageable);
    }
    
    /**
     * Toggle multiple categories' active status
     * @param ids List of category IDs
     * @param active New active status
     * @return Number of categories updated
     */
    @Transactional
    public int toggleActiveMultiple(List<Integer> ids, boolean active) {
        int count = 0;
        for (Integer id : ids) {
            Optional<Category> category = categoryRepository.findById(id);
            if (category.isPresent()) {
                Category existingCategory = category.get();
                existingCategory.setActive(active);
                categoryRepository.save(existingCategory);
                count++;
            }
        }
        return count;
    }
    public boolean existsByName(String name) {
        return categoryRepository.findByCategoryNameIgnoreCase(name).isPresent();
    }
}
