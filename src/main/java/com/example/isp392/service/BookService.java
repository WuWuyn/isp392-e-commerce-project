package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import com.example.isp392.model.Publisher;
import com.example.isp392.repository.BookRepository;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository, PublisherRepository publisherRepository) {
        this.bookRepository = bookRepository;
        // We don't need to store these repositories as fields since they're only used in Criteria API queries
        // But we still need them in the constructor for dependency injection
    }

    // Lấy tất cả sách
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // Lấy sách theo ID
    public Optional<Book> getBookById(int id) {
        return bookRepository.findById(id);
    }

    // Lấy danh sách sách có đánh giá cao
    public List<Book> getTopRatedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findTopRatedBooks(pageable);
    }

    // Lấy những sách mới thêm vào
    public List<Book> getNewAdditions(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findNewAdditions(pageable);
    }

    // Lấy những sách đang giảm giá
    public List<Book> getDiscountedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findDiscountedBooks(pageable);
    }

    // Tìm kiếm sách theo tiêu đề (phân trang)
    public Page<Book> searchBooksByTitle(String title, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    // Tìm kiếm sách theo danh mục (phân trang)
    public Page<Book> findBooksByCategory(Category category, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByCategory(category, pageable);
    }

    /**
     * Advanced search method to find books with multiple filters
     */
    public Page<Book> findBooks(
            String searchQuery,
            List<Integer> categoryIds,
            List<Integer> publisherIds,
            BigInteger minPrice,
            BigInteger maxPrice,
            Float minRating,
            int page,
            int size,
            String sortField,
            String sortDirection) {

        // Create a criteria builder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);

        // Create a list to hold all predicates
        List<Predicate> predicates = new ArrayList<>();

        // Add title search predicate if search query is provided
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(book.get("title")), "%" + searchQuery.toLowerCase() + "%"));
        }

        // Add category filters if category IDs are provided
        if (categoryIds != null && !categoryIds.isEmpty()) {
            // Create a join with the categories
            Join<Book, Category> categoryJoin = book.join("categories");
            predicates.add(categoryJoin.get("categoryId").in(categoryIds));
        }

        // Add publisher filters if publisher IDs are provided
        if (publisherIds != null && !publisherIds.isEmpty()) {
            Join<Book, Publisher> publisherJoin = book.join("publisher");
            predicates.add(publisherJoin.get("publisherId").in(publisherIds));
        }

        // Add price range filters if provided
        if (minPrice != null) {
            predicates.add(cb.ge(book.get("sellingPrice"), minPrice));
        }

        if (maxPrice != null) {
            predicates.add(cb.le(book.get("sellingPrice"), maxPrice));
        }

        // Add rating filter if provided
        if (minRating != null) {
            predicates.add(cb.ge(book.get("averageRating"), new BigDecimal(minRating)));
        }

        // Add predicates to the query
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        // Add sorting
        if ("ASC".equalsIgnoreCase(sortDirection)) {
            query.orderBy(cb.asc(book.get(sortField)));
        } else {
            query.orderBy(cb.desc(book.get(sortField)));
        }

        // Create count query for pagination
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Book> countRoot = countQuery.from(Book.class);

        // Apply the same predicates to the count query
        if (!predicates.isEmpty()) {
            // We need to recreate the joins and predicates for the count query
            List<Predicate> countPredicates = new ArrayList<>();

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                countPredicates.add(cb.like(cb.lower(countRoot.get("title")), "%" + searchQuery.toLowerCase() + "%"));
            }

            if (categoryIds != null && !categoryIds.isEmpty()) {
                Join<Book, Category> categoryJoin = countRoot.join("categories");
                countPredicates.add(categoryJoin.get("categoryId").in(categoryIds));
            }

            if (publisherIds != null && !publisherIds.isEmpty()) {
                Join<Book, Publisher> publisherJoin = countRoot.join("publisher");
                countPredicates.add(publisherJoin.get("publisherId").in(publisherIds));
            }

            if (minPrice != null) {
                countPredicates.add(cb.ge(countRoot.get("sellingPrice"), minPrice));
            }

            if (maxPrice != null) {
                countPredicates.add(cb.le(countRoot.get("sellingPrice"), maxPrice));
            }

            if (minRating != null) {
                countPredicates.add(cb.ge(countRoot.get("averageRating"), new BigDecimal(minRating)));
            }

            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        countQuery.select(cb.count(countRoot));

        // Execute queries
        TypedQuery<Book> typedQuery = entityManager.createQuery(query);
        TypedQuery<Long> typedCountQuery = entityManager.createQuery(countQuery);

        // Apply pagination
        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        // Get results
        List<Book> books = typedQuery.getResultList();
        Long total = typedCountQuery.getSingleResult();

        // Create a pageable object for the response
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortField));

        // Return a Page implementation
        return new PageImpl<>(books, pageable, total);
    }


}