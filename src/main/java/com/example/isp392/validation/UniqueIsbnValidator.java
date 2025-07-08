package com.example.isp392.validation;

import com.example.isp392.service.BookService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UniqueIsbnValidator implements ConstraintValidator<UniqueIsbn, String> {

    @Autowired
    private BookService bookService;

    @Override
    public void initialize(UniqueIsbn constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String isbn, ConstraintValidatorContext context) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return true; // Let @NotBlank handle empty validation
        }
        
        // Check if ISBN exists in the database
        return !bookService.isbnExists(isbn.trim());
    }
} 