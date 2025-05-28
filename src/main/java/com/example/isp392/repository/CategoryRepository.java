package com.example.isp392.repository;

import com.example.isp392.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Page<Category> findByCategoryNameContainingIgnoreCase(String categoryName, Pageable pageable);
}
