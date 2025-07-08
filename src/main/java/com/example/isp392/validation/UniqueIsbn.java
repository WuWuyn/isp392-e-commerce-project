package com.example.isp392.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueIsbnValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueIsbn {
    String message() default "A book with this ISBN already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 