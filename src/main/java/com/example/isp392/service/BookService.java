package com.example.isp392.service;

import com.example.isp392.dto.BookFormDTO;
import com.example.isp392.dto.InventoryReportDTO;
import com.example.isp392.dto.InventoryUpdateDTO;
import com.example.isp392.model.Book;
import com.example.isp392.model.Category;
import com.example.isp392.model.OrderItem;
import com.example.isp392.model.Publisher;
import com.example.isp392.model.Shop;
import com.example.isp392.repository.BookRepository;
import com.example.isp392.repository.BookReviewRepository;
import com.example.isp392.repository.CategoryRepository;
import com.example.isp392.repository.PublisherRepository;
import com.example.isp392.repository.ShopRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import me.xdrop.fuzzywuzzy.FuzzySearch;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ShopRepository shopRepository;

    // Lớp nội bộ để giữ sách và điểm số
    private static class BookWithScore {
        private final Book book;
        private final int score;

        public BookWithScore(Book book, int score) {
            this.book = book;
            this.score = score;
        }

        public Book getBook() {
            return book;
        }

        public int getScore() {
            return score;
        }
    }

    @Transactional
    public void deactivateBooksByShopId(Integer shopId) {
        log.info("Deactivating all books for shop ID: {}", shopId);
        bookRepository.deactivateBooksByShopId(shopId);
    }

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Constructor for dependency injection
     *
     * @param bookRepository      Repository for book data access
     * @param bookReviewRepository Repository for book review data access
     * @param categoryRepository  Repository for category data access
     * @param publisherRepository Repository for publisher data access
     * @param shopRepository      Repository for shop data access
     */
    public BookService(
            BookRepository bookRepository,
            BookReviewRepository bookReviewRepository,
            CategoryRepository categoryRepository,
            PublisherRepository publisherRepository,
            ShopRepository shopRepository) {
        this.bookRepository = bookRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.shopRepository = shopRepository;
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

    // Tìm kiếm sách theo tiêu đề (phân trang) - sử dụng fuzzy search nâng cao
    public Page<Book> searchBooksByTitle(String title, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        // If title is empty, return all books
        if (title == null || title.trim().isEmpty()) {
            return bookRepository.findAll(pageable);
        }

        // Get all books
        List<Book> allBooks = bookRepository.findByIsActiveTrue();

        // Chuyển query về chữ thường để so sánh nhất quán
        final String lowerCaseQuery = title.toLowerCase();

        // Filter books using advanced fuzzy search
        List<Book> matchedBooks = allBooks.stream()
                .map(book -> {
                    String bookTitle = book.getTitle().toLowerCase();

                    // 1. Tính điểm cho lỗi chính tả (so khớp các từ)
                    int tokenScore = FuzzySearch.tokenSetRatio(lowerCaseQuery, bookTitle);

                    // 2. Tính điểm cho chuỗi con/tiền tố (để "Har" khớp với "Harry")
                    int partialScore = FuzzySearch.partialRatio(lowerCaseQuery, bookTitle);

                    // 3. Lấy điểm cao nhất từ hai thuật toán trên làm điểm cuối cùng
                    int finalScore = Math.max(tokenScore, partialScore);

                    return new BookWithScore(book, finalScore);
                })
                .filter(bookWithScore -> bookWithScore.getScore() >= 60) // Ngưỡng điểm hợp lý là 65
                .sorted((b1, b2) -> Integer.compare(b2.getScore(), b1.getScore()))
                .map(BookWithScore::getBook)
                .collect(Collectors.toList());

        // Convert list to page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matchedBooks.size());

        if (start >= matchedBooks.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, matchedBooks.size());
        }

        return new PageImpl<>(
                matchedBooks.subList(start, end),
                pageable,
                matchedBooks.size()
        );
    }

    // Tìm kiếm sách theo danh mục (phân trang)
    public Page<Book> findBooksByCategory(Category category, int page, int size, String sortField, String sortDirection) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        return bookRepository.findByCategory(category, pageable);
    }

    /**
     * Find books by shop ID with pagination and sorting
     *
     * @param shopId   ID of the shop
     * @param pageable Pagination and sorting information
     * @return Page of books belonging to the shop
     */
    public Page<Book> findByShopId(Integer shopId, Pageable pageable) {
        return bookRepository.findByShopShopId(shopId, pageable);
    }

    /**
     * Search for books by title within a specific shop using advanced fuzzy search
     *
     * @param shopId   ID of the shop
     * @param title    Search term for book title
     * @param pageable Pagination and sorting information
     * @return Page of matching books belonging to the shop
     */
    public Page<Book> searchBooksByShopAndTitle(Integer shopId, String title, Pageable pageable) {
        // If title is empty, return all books from the shop
        if (title == null || title.trim().isEmpty()) {
            return findByShopId(shopId, pageable);
        }

        // Get all books from the shop
        List<Book> shopBooks = bookRepository.findByShopShopId(shopId, Pageable.unpaged()).getContent();

        // Chuyển query về chữ thường để so sánh nhất quán
        final String lowerCaseQuery = title.toLowerCase();

        // Filter books using advanced fuzzy search
        List<Book> matchedBooks = shopBooks.stream()
                .map(book -> {
                    String bookTitle = book.getTitle().toLowerCase();

                    // 1. Tính điểm cho lỗi chính tả (so khớp các từ)
                    int tokenScore = FuzzySearch.tokenSetRatio(lowerCaseQuery, bookTitle);

                    // 2. Tính điểm cho chuỗi con/tiền tố (để "Har" khớp với "Harry")
                    int partialScore = FuzzySearch.partialRatio(lowerCaseQuery, bookTitle);

                    // 3. Lấy điểm cao nhất từ hai thuật toán trên làm điểm cuối cùng
                    int finalScore = Math.max(tokenScore, partialScore);

                    return new BookWithScore(book, finalScore);
                })
                .filter(bookWithScore -> bookWithScore.getScore() >= 65) // Ngưỡng điểm hợp lý là 65
                .sorted((b1, b2) -> Integer.compare(b2.getScore(), b1.getScore()))
                .map(BookWithScore::getBook)
                .collect(Collectors.toList());

        // Convert list to page
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), matchedBooks.size());

        if (start >= matchedBooks.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, matchedBooks.size());
        }

        return new PageImpl<>(
                matchedBooks.subList(start, end),
                pageable,
                matchedBooks.size()
        );
    }

    /**
     * Create a new book from BookFormDTO
     *
     * @param bookForm      DTO with book information
     * @param coverImageUrl URL of the uploaded cover image
     * @return Created book entity
     */
    @Transactional
    public Book createBook(BookFormDTO bookForm, String coverImageUrl) {
        log.debug("Creating new book: {}", bookForm.getTitle());

        // Validate prices
        if (bookForm.getSellingPrice().compareTo(bookForm.getOriginalPrice()) > 0) {
            throw new IllegalArgumentException("Giá bán không được lớn hơn giá gốc");
        }

        // Create new book entity
        Book book = new Book();

        // Set basic book information
        book.setTitle(bookForm.getTitle());
        book.setAuthors(bookForm.getAuthors());
        book.setDescription(bookForm.getDescription());
        book.setIsbn(bookForm.getIsbn());
        book.setNumberOfPages(bookForm.getNumberOfPages());
        book.setDimensions(bookForm.getDimensions());
        book.setSku(bookForm.getSku());
        book.setPublicationDate(bookForm.getPublicationDate());
        book.setOriginalPrice(bookForm.getOriginalPrice());
        book.setSellingPrice(bookForm.getSellingPrice());
        book.setStockQuantity(bookForm.getStockQuantity());
        book.setCoverImgUrl(coverImageUrl);

        // Set creation date
        book.setDateAdded(LocalDate.now());

        // Default values for new book
        book.setAverageRating(new BigDecimal("0.0"));
        book.setTotalReviews(0);
        book.setViewsCount(0);
        book.setActive(true); // New books are active by default

        // Get and set shop
        Shop shop = shopRepository.findById(bookForm.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found with ID: " + bookForm.getShopId()));
        book.setShop(shop);

        // Get and set publisher if provided
        if (bookForm.getPublisherId() != null) {
            Publisher publisher = publisherRepository.findById(bookForm.getPublisherId())
                    .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + bookForm.getPublisherId()));
            book.setPublisher(publisher);
        }

        // Get and set categories
        Set<Category> categories = new HashSet<>();
        for (Integer categoryId : bookForm.getCategoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            categories.add(category);
        }
        book.setCategories(categories);

        // Save and return book
        Book savedBook = bookRepository.save(book);
        log.info("Book created successfully with ID: {}", savedBook.getBookId());

        return savedBook;
    }

    /**
     * Update an existing book from BookFormDTO
     *
     * @param bookId        ID of the book to update
     * @param bookForm      DTO with updated book information
     * @param coverImageUrl URL of the updated cover image (or null to keep existing)
     * @return Updated book entity
     */
    @Transactional
    public Book updateBook(Integer bookId, BookFormDTO bookForm, String coverImageUrl) {
        log.debug("Updating book with ID: {}", bookId);

        // Validate prices
        if (bookForm.getSellingPrice().compareTo(bookForm.getOriginalPrice()) > 0) {
            throw new IllegalArgumentException("Giá bán không được lớn hơn giá gốc");
        }

        // Find existing book
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        // Update basic book information
        book.setTitle(bookForm.getTitle());
        book.setAuthors(bookForm.getAuthors());
        book.setDescription(bookForm.getDescription());
        book.setIsbn(bookForm.getIsbn());
        book.setNumberOfPages(bookForm.getNumberOfPages());
        book.setDimensions(bookForm.getDimensions());
        book.setSku(bookForm.getSku());
        book.setPublicationDate(bookForm.getPublicationDate());
        book.setOriginalPrice(bookForm.getOriginalPrice());
        book.setSellingPrice(bookForm.getSellingPrice());
        book.setStockQuantity(bookForm.getStockQuantity());

        // Update cover image if provided
        if (coverImageUrl != null) {
            book.setCoverImgUrl(coverImageUrl);
        }

        // Update publisher if provided
        if (bookForm.getPublisherId() != null) {
            Publisher publisher = publisherRepository.findById(bookForm.getPublisherId())
                    .orElseThrow(() -> new IllegalArgumentException("Publisher not found with ID: " + bookForm.getPublisherId()));
            book.setPublisher(publisher);
        }

        // Update categories
        Set<Category> categories = new HashSet<>();
        for (Integer categoryId : bookForm.getCategoryIds()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));
            categories.add(category);
        }
        book.setCategories(categories);

        // Save and return updated book
        Book updatedBook = bookRepository.save(book);
        log.info("Book updated successfully with ID: {}", updatedBook.getBookId());

        return updatedBook;
    }

    /**
     * Delete a book by ID
     *
     * @param bookId ID of the book to delete
     */
    @Transactional
    public void deleteBook(Integer bookId) {
        log.debug("Deactivating book with ID: {} via deleteBook method", bookId);
        this.deactivateBook(bookId);
    }

    /**
     * Cập nhật số lượng sách trong kho
     *
     * @param bookId   ID của sách
     * @param quantity Số lượng cần giảm (số âm để giảm, số dương để tăng)
     * @throws IllegalArgumentException nếu số lượng không hợp lệ hoặc không đủ trong kho
     */
    @Transactional
    public void updateStockQuantity(Integer bookId, int quantity) {
        // Use SELECT FOR UPDATE to prevent race conditions
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + bookId));

        int currentStock = book.getStockQuantity();
        int newQuantity = currentStock + quantity;

        log.info("Updating stock for book ID {}: current={}, change={}, new={}",
                bookId, currentStock, quantity, newQuantity);

        if (newQuantity < 0) {
            log.error("Insufficient stock for book ID {}: requested change={}, current stock={}",
                     bookId, quantity, currentStock);
            throw new IllegalArgumentException("Số lượng sách trong kho không đủ. Còn lại: " + currentStock +
                                             ", yêu cầu: " + Math.abs(quantity));
        }

        book.setStockQuantity(newQuantity);
        bookRepository.save(book);
        log.info("Successfully updated stock for book ID {} from {} to {}", bookId, currentStock, newQuantity);
    }

    /**
     * Atomic method to check and reserve inventory for multiple items
     * This prevents race conditions when multiple users order the same items
     */
    @org.springframework.transaction.annotation.Transactional(isolation = Isolation.SERIALIZABLE)
    public void reserveInventoryForOrder(List<OrderItem> orderItems) {
        log.info("Reserving inventory for {} items", orderItems.size());

        for (OrderItem item : orderItems) {
            Integer bookId = item.getBook().getBookId();
            int requestedQuantity = item.getQuantity();

            // Use SELECT FOR UPDATE to lock the row
            Book book = bookRepository.findByIdForUpdate(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + bookId));

            int currentStock = book.getStockQuantity();

            log.info("Checking inventory for book ID {}: requested={}, available={}",
                    bookId, requestedQuantity, currentStock);

            if (requestedQuantity > currentStock) {
                log.error("Insufficient inventory for book ID {}: requested={}, available={}",
                         bookId, requestedQuantity, currentStock);
                throw new IllegalArgumentException("Sản phẩm '" + book.getTitle() +
                                                 "' không đủ số lượng trong kho. Còn lại: " + currentStock +
                                                 ", yêu cầu: " + requestedQuantity);
            }

            // Reserve the inventory
            book.setStockQuantity(currentStock - requestedQuantity);
            bookRepository.save(book);

            log.info("Reserved {} units of book ID {}, remaining stock: {}",
                    requestedQuantity, bookId, book.getStockQuantity());
        }

        log.info("Successfully reserved inventory for all {} items", orderItems.size());
    }

    /**
     * Giảm số lượng sách trong kho sau khi đặt hàng
     *
     * @param bookId   ID của sách
     * @param quantity Số lượng cần giảm
     * @throws IllegalArgumentException nếu số lượng không hợp lệ hoặc không đủ trong kho
     */
    @Transactional
    public void decreaseStockQuantity(Integer bookId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng giảm phải lớn hơn 0");
        }
        updateStockQuantity(bookId, -quantity);
    }

    /**
     * Tăng số lượng sách trong kho (ví dụ: khi hủy đơn hàng)
     *
     * @param bookId   ID của sách
     * @param quantity Số lượng cần tăng
     * @throws IllegalArgumentException nếu số lượng không hợp lệ
     */
    @Transactional
    public void increaseStockQuantity(Integer bookId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng tăng phải lớn hơn 0");
        }
        updateStockQuantity(bookId, quantity);
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
            String sortDirection,
            boolean filterByActiveStatus) {

        // If we only have a search query with no other filters, use the fuzzy search
        if (searchQuery != null && !searchQuery.trim().isEmpty() &&
                (categoryIds == null || categoryIds.isEmpty()) &&
                (publisherIds == null || publisherIds.isEmpty()) &&
                minPrice == null && maxPrice == null && minRating == null) {

            // Use fuzzy search if only searching by title
            return searchBooksByTitle(searchQuery, page, size, sortField, sortDirection);
        }

        // Create a criteria builder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Book> query = cb.createQuery(Book.class);
        Root<Book> book = query.from(Book.class);

        // Create a list to hold all predicates
        List<Predicate> predicates = new ArrayList<>();
        if (filterByActiveStatus) {
            predicates.add(cb.isTrue(book.get("isActive")));
        }

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

    /**
     * Count active books for a shop
     *
     * @param shopId ID of the shop
     * @return Count of active books
     */
    public long countActiveBooksByShopId(Integer shopId) {
        return bookRepository.countByShopShopIdAndIsActiveTrue(shopId);
    }

    /**
     * Find books with low stock by shop ID
     *
     * @param shopId    ID of the shop
     * @param threshold Stock threshold
     * @return List of books with stock below threshold
     */
    public List<Book> findLowStockBooksByShopId(Integer shopId, int threshold) {
        return bookRepository.findByShopShopIdAndStockQuantityLessThanAndIsActiveTrue(shopId, threshold);
    }

    /**
     * Find bestselling books by shop ID
     *
     * @param shopId ID of the shop
     * @param limit  Maximum number of books to return
     * @return List of bestselling books
     */
    public List<Book> findBestsellingBooksByShopId(Integer shopId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return bookRepository.findByShopShopIdAndIsActiveTrueOrderByAverageRatingDesc(shopId, pageable);
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public long countAllBooks() {
        return bookRepository.count();
    }

    /**
     * Cập nhật thống kê đánh giá cho sách (total_reviews và average_rating)
     * Phương thức này sẽ được gọi tự động khi có đánh giá mới, cập nhật, hoặc xóa
     *
     * @param bookId ID của sách cần cập nhật thống kê
     */
    @Transactional
    public void updateBookReviewStatistics(Integer bookId) {
        try {
            // Lấy thông tin sách
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + bookId));

            // Đếm số lượng đánh giá đã được duyệt
            long totalApprovedReviews = bookReviewRepository.countApprovedByBookId(bookId);

            // Tính điểm đánh giá trung bình của các đánh giá đã được duyệt
            Double averageRating = bookReviewRepository.calculateAverageRatingByBookId(bookId);

            // Cập nhật thông tin sách
            book.setTotalReviews((int) totalApprovedReviews);
            book.setAverageRating(averageRating != null ?
                BigDecimal.valueOf(averageRating).setScale(1, RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

            // Lưu thay đổi
            bookRepository.save(book);

            log.info("Updated review statistics for book ID {}: {} reviews, {} average rating",
                    bookId, totalApprovedReviews, book.getAverageRating());

        } catch (Exception e) {
            log.error("Error updating review statistics for book ID {}: {}", bookId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Checks if an ISBN already exists within a specific shop.
     *
     * @param isbn   The ISBN to check.
     * @param shopId The ID of the shop to check within.
     * @return true if the ISBN exists in the specified shop, false otherwise.
     */
    public boolean isbnExists(String isbn, Integer shopId) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false; // An empty ISBN cannot exist
        }
        return bookRepository.existsByIsbnAndShopShopId(isbn.trim(), shopId);
    }

    /**
     * Finds a book by its ISBN.
     *
     * @param isbn The ISBN of the book to find.
     * @return An Optional containing the Book if found, or empty otherwise.
     */
    public Optional<Book> findByIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return Optional.empty();
        }
        return bookRepository.findByIsbn(isbn.trim());
    }

    // ==================== INVENTORY MANAGEMENT METHODS ====================

    /**
     * Get inventory report for all books in a shop
     *
     * @param shopId ID of the shop
     * @param pageable Pagination information
     * @return Page of inventory report DTOs
     */
    public Page<InventoryReportDTO> getInventoryReport(Integer shopId, Pageable pageable) {
        Page<Book> books = bookRepository.findByShopShopId(shopId, pageable);
        return books.map(book -> new InventoryReportDTO(
                book.getBookId(),
                book.getTitle(),
                book.getAuthors(),
                book.getIsbn(),
                book.getSku(),
                book.getStockQuantity(),
                book.getOriginalPrice(),
                book.getSellingPrice(),
                book.getCoverImgUrl(),
                book.getActive()
        ));
    }

    /**
     * Update stock quantity for a single book with validation
     *
     * @param bookId ID of the book
     * @param newQuantity New stock quantity
     * @param shopId ID of the shop (for security validation)
     * @param reason Reason for the update
     * @param notes Additional notes
     * @throws IllegalArgumentException if book not found or doesn't belong to shop
     */
    @Transactional
    public void updateBookStock(Integer bookId, Integer newQuantity, Integer shopId, String reason, String notes) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        // Validate that the book belongs to the shop
        if (!book.getShop().getShopId().equals(shopId)) {
            throw new IllegalArgumentException("Book does not belong to your shop");
        }

        int oldQuantity = book.getStockQuantity();
        book.setStockQuantity(newQuantity);
        bookRepository.save(book);

        log.info("Stock updated for book ID {}: {} -> {} (Shop: {}, Reason: {}, Notes: {})",
                bookId, oldQuantity, newQuantity, shopId, reason, notes);
    }

    /**
     * Bulk update stock quantities for multiple books
     *
     * @param inventoryUpdates List of inventory updates
     * @param shopId ID of the shop (for security validation)
     * @param globalReason Global reason for all updates
     * @param globalNotes Global notes for all updates
     * @return Number of successfully updated books
     */
    @Transactional
    public int bulkUpdateStock(List<InventoryUpdateDTO> inventoryUpdates, Integer shopId,
                              String globalReason, String globalNotes) {
        int successCount = 0;

        for (InventoryUpdateDTO update : inventoryUpdates) {
            try {
                String reason = update.getReason() != null ? update.getReason() : globalReason;
                String notes = update.getNotes() != null ? update.getNotes() : globalNotes;

                updateBookStock(update.getBookId(), update.getStockQuantity(), shopId, reason, notes);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to update stock for book ID {}: {}", update.getBookId(), e.getMessage());
                // Continue with other updates instead of failing the entire batch
            }
        }

        log.info("Bulk stock update completed: {}/{} books updated successfully",
                successCount, inventoryUpdates.size());
        return successCount;
    }

    /**
     * Get low stock books for a shop with custom threshold
     *
     * @param shopId ID of the shop
     * @param threshold Stock threshold (default 5 if null)
     * @return List of books with stock below threshold
     */
    public List<Book> getLowStockBooks(Integer shopId, Integer threshold) {
        int stockThreshold = threshold != null ? threshold : 5;
        return bookRepository.findByShopShopIdAndStockQuantityLessThanAndIsActiveTrue(shopId, stockThreshold);
    }

    /**
     * Get out of stock books for a shop
     *
     * @param shopId ID of the shop
     * @return List of books with zero stock
     */
    public List<Book> getOutOfStockBooks(Integer shopId) {
        return bookRepository.findByShopShopIdAndStockQuantityAndIsActiveTrue(shopId, 0);
    }

    /**
     * Get inventory statistics for a shop
     *
     * @param shopId ID of the shop
     * @return Map containing inventory statistics
     */
    public Map<String, Object> getInventoryStatistics(Integer shopId) {
        Map<String, Object> stats = new HashMap<>();

        // Total active products
        long totalProducts = bookRepository.countByShopShopIdAndIsActiveTrue(shopId);

        // Low stock products (stock <= 5)
        long lowStockCount = bookRepository.countByShopShopIdAndStockQuantityLessThanEqualAndIsActiveTrue(shopId, 5);

        // Out of stock products
        long outOfStockCount = bookRepository.countByShopShopIdAndStockQuantityAndIsActiveTrue(shopId, 0);

        // In stock products
        long inStockCount = totalProducts - outOfStockCount;

        stats.put("totalProducts", totalProducts);
        stats.put("inStockCount", inStockCount);
        stats.put("lowStockCount", lowStockCount);
        stats.put("outOfStockCount", outOfStockCount);

        return stats;
    }

    /**
     * Get total views for all books in a shop
     *
     * @param shopId the shop ID
     * @return total views (sum of viewsCount)
     */
    public int getTotalViewsByShopId(Integer shopId) {
        Integer total = bookRepository.getTotalViewsByShopId(shopId);
        return total != null ? total : 0;
    }

    /**
     * Get views for each product in a shop
     *
     * @param shopId the shop ID
     * @return list of maps: {bookId, title, viewsCount}
     */
    public List<Map<String, Object>> getViewsByProductInShop(Integer shopId) {
        List<Object[]> raw = bookRepository.getViewsByProductInShop(shopId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : raw) {
            Map<String, Object> map = new HashMap<>();
            map.put("bookId", row[0]);
            map.put("title", row[1]);
            map.put("viewsCount", row[2]);
            result.add(map);
        }
        return result;
    }

    @Transactional
    public void incrementViewsCount(int bookId) {
        bookRepository.incrementViewsCount(bookId);
    }

    /**
     * Deactivates all books belonging to a specific shop.
     *
     * @param shopId ID of the shop whose books are to be deactivated
     */
    @Transactional
    public Page<Book> searchActiveBooksByShop(Integer shopId, String searchQuery, Pageable pageable) {
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            // Nếu có từ khóa tìm kiếm, gọi phương thức repository mới
            return bookRepository.findByShop_ShopIdAndIsActiveTrueAndTitleContainingIgnoreCase(shopId, searchQuery, pageable);
        } else {
            // Nếu không, trả về tất cả sách của shop
            return bookRepository.findByShop_ShopIdAndIsActiveTrue(shopId, pageable);
        }
    }
    @Transactional
    public void deactivateBook(Integer bookId) {
        log.debug("Deactivating book with ID: {}", bookId);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));
        book.setActive(false); // Đặt trạng thái isActive thành false
        bookRepository.save(book);
        log.info("Book with ID {} has been successfully deactivated (hidden).", bookId);
    }
}
