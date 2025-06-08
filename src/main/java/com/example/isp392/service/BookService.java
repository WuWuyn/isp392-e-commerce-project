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
    }

    // ==========================================================
    // === PHƯƠNG THỨC SAVE ĐÃ ĐƯỢC TRIỂN KHAI TẠI ĐÂY ===
    // ==========================================================
    /**
     * Lưu một đối tượng Book vào cơ sở dữ liệu.
     * Dùng cho cả việc thêm mới và cập nhật.
     * @param book Đối tượng sách cần lưu.
     * @return Đối tượng sách đã được lưu (với ID được cập nhật nếu là sách mới).
     */
    public Book save(Book book) {
        return bookRepository.save(book);
    }


    public void deleteById(int id) {
        bookRepository.deleteById(id);
    }


    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public Optional<Book> getBookById(int id) {
        return bookRepository.findById(id);
    }

    public List<Book> getTopRatedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findTopRatedBooks(pageable);
    }

    public List<Book> getNewAdditions(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findNewAdditions(pageable);
    }

    public List<Book> getDiscountedBooks(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findDiscountedBooks(pageable);
    }

    public Page<Book> searchBooksByTitle(String title, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Book> findBooksByCategory(Category category, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByCategory(category, pageable);
    }

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

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);
        List<Predicate> predicates = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(book.get("title")), "%" + searchQuery.toLowerCase() + "%"));
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            Join<Book, Category> categoryJoin = book.join("categories");
            predicates.add(categoryJoin.get("categoryId").in(categoryIds));
        }
        if (publisherIds != null && !publisherIds.isEmpty()) {
            Join<Book, Publisher> publisherJoin = book.join("publisher");
            predicates.add(publisherJoin.get("publisherId").in(publisherIds));
        }
        if (minPrice != null) {
            predicates.add(cb.ge(book.get("sellingPrice"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.le(book.get("sellingPrice"), maxPrice));
        }
        if (minRating != null) {
            predicates.add(cb.ge(book.get("averageRating"), new BigDecimal(minRating.toString())));
        }

        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
        }

        if ("ASC".equalsIgnoreCase(sortDirection)) {
            query.orderBy(cb.asc(book.get(sortField)));
        } else {
            query.orderBy(cb.desc(book.get(sortField)));
        }

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Book> countRoot = countQuery.from(Book.class);

        if (!predicates.isEmpty()) {
            // Recreate joins and predicates for the count query
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
                countPredicates.add(cb.ge(countRoot.get("averageRating"), new BigDecimal(minRating.toString())));
            }
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }

        countQuery.select(cb.countDistinct(countRoot)); // Use countDistinct because of joins

        TypedQuery<Book> typedQuery = entityManager.createQuery(query);
        TypedQuery<Long> typedCountQuery = entityManager.createQuery(countQuery);

        typedQuery.setFirstResult(page * size);
        typedQuery.setMaxResults(size);

        List<Book> books = typedQuery.getResultList();
        Long total = typedCountQuery.getSingleResult();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortField));

        return new PageImpl<>(books, pageable, total);
    }
}