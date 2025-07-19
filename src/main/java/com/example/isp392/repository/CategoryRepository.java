package com.example.isp392.repository;

import com.example.isp392.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Search by name containing (case insensitive)
    Page<Category> findByCategoryNameContainingIgnoreCase(String categoryName, Pageable pageable);
    
    // Search by name containing and active status
    Page<Category> findByCategoryNameContainingIgnoreCaseAndIsActive(String categoryName, boolean isActive, Pageable pageable);
    
    // Search by active status
    Page<Category> findByIsActive(boolean isActive, Pageable pageable);
    
    // Get all active categories (no pagination)
    List<Category> findByIsActiveTrue();
    
    // Get all active categories (with pagination)
    Page<Category> findByIsActiveTrue(Pageable pageable);

    Optional<Category> findByCategoryName(String categoryName);
}

