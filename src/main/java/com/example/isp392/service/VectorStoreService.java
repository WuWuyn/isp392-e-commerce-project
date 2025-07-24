package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.Promotion;
import com.example.isp392.model.Shop;
import com.example.isp392.model.PaymentMethod;
import com.example.isp392.model.BookReview;
import com.example.isp392.repository.BookRepository;
import com.example.isp392.repository.PromotionRepository;
import com.example.isp392.repository.BookReviewRepository;
import com.example.isp392.config.PaymentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing Vector Store operations with books
 */
@Service
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    private final BookRepository bookRepository;
    private final VectorStore vectorStore;
    private final PromotionRepository promotionRepository;
    private final BookReviewRepository bookReviewRepository;
    private final PaymentConfig paymentConfig;

    @Autowired
    public VectorStoreService(BookRepository bookRepository,
                             @Autowired(required = false) VectorStore vectorStore,
                             PromotionRepository promotionRepository,
                             BookReviewRepository bookReviewRepository,
                             PaymentConfig paymentConfig) {
        this.bookRepository = bookRepository;
        this.vectorStore = vectorStore;
        this.promotionRepository = promotionRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.paymentConfig = paymentConfig;
    }

    /**
     * Load all books into Vector Store
     */
    public void loadBooksToVectorStore() {
        if (vectorStore == null) {
            logger.warn("Vector Store not available");
            return;
        }

        try {
            logger.info("Loading books into Vector Store with batch processing...");

            List<Book> books = bookRepository.findAllActiveWithCompleteInfo();
            logger.info("Found {} books in database", books.size());

            if (books.isEmpty()) {
                logger.warn("No books found in database");
                return;
            }

            // Process in batches for better performance and error handling
            int batchSize = 100;
            int totalBooks = books.size();
            int processedBooks = 0;
            int failedBooks = 0;

            for (int i = 0; i < totalBooks; i += batchSize) {
                int endIndex = Math.min(i + batchSize, totalBooks);
                List<Book> batch = books.subList(i, endIndex);

                try {
                    List<Document> documents = new ArrayList<>();

                    for (Book book : batch) {
                        try {
                            Document document = createRichDocumentFromBook(book);
                            documents.add(document);
                            processedBooks++;
                        } catch (Exception e) {
                            logger.warn("Failed to create document for book {} ({}): {}",
                                      book.getBookId(), book.getTitle(), e.getMessage());
                            failedBooks++;
                        }
                    }

                    if (!documents.isEmpty()) {
                        vectorStore.add(documents);
                        logger.debug("Processed batch {}-{} ({} documents)", i + 1, endIndex, documents.size());
                    }

                } catch (Exception e) {
                    logger.error("Error processing batch {}-{}: {}", i + 1, endIndex, e.getMessage());
                    failedBooks += batch.size();
                }
            }

            logger.info("Vector Store loading completed: {} books processed, {} failed",
                       processedBooks, failedBooks);

        } catch (Exception e) {
            logger.error("Critical error loading books into Vector Store: {}", e.getMessage(), e);
        }
    }

    /**
     * Create rich semantic document from book with comprehensive information
     */
    private Document createRichDocumentFromBook(Book book) {
        StringBuilder content = new StringBuilder();

        // Core book information
        content.append("Title: ").append(book.getTitle()).append("\n");
        content.append("Authors: ").append(book.getAuthors()).append("\n");

        // Categories
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            content.append("Categories: ");
            book.getCategories().forEach(category -> {
                content.append(category.getCategoryName()).append(", ");
            });
            content.append("\n");
        }

        // Publisher information
        if (book.getPublisher() != null) {
            content.append("Publisher: ").append(book.getPublisher().getPublisherName()).append("\n");
        }

        // Publication details
        if (book.getPublicationDate() != null) {
            content.append("Publication Date: ").append(book.getPublicationDate()).append("\n");
        }

        if (book.getNumberOfPages() != null) {
            content.append("Pages: ").append(book.getNumberOfPages()).append("\n");
        }

        // Pricing information
        content.append("Original Price: ").append(book.getOriginalPrice()).append(" VND\n");
        if (book.getSellingPrice() != null) {
            content.append("Selling Price: ").append(book.getSellingPrice()).append(" VND\n");

            // Discount calculation
            int discountPercent = book.getDiscountPercentage();
            if (discountPercent > 0) {
                content.append("Discount: ").append(discountPercent).append("% off\n");
            }
        }

        // Rating and reviews
        if (book.getAverageRating() != null && book.getAverageRating().doubleValue() > 0) {
            content.append("Rating: ").append(book.getAverageRating()).append("/5");
            if (book.getTotalReviews() != null && book.getTotalReviews() > 0) {
                content.append(" (").append(book.getTotalReviews()).append(" reviews)");
            }
            content.append("\n");
        }

        // Availability
        content.append("Stock: ").append(book.getStockQuantity()).append(" available\n");

        // === MARKETPLACE INFORMATION ===
        addMarketplaceInformation(content, book);

        // Description
        if (book.getDescription() != null && !book.getDescription().trim().isEmpty()) {
            content.append("Description: ").append(book.getDescription()).append("\n");
        }

        // Additional metadata
        if (book.getViewsCount() != null && book.getViewsCount() > 0) {
            content.append("Views: ").append(book.getViewsCount()).append("\n");
        }

        // Create comprehensive metadata
        Map<String, Object> metadata = createBookMetadata(book);

        String documentId = "book_" + book.getBookId();
        return new Document(documentId, content.toString(), metadata);
    }

    /**
     * Add comprehensive marketplace information to document content
     */
    private void addMarketplaceInformation(StringBuilder content, Book book) {
        // Vendor/Shop Information
        addVendorInformation(content, book);

        // Active Promotions
        addActivePromotions(content, book);

        // Payment Methods
        addPaymentMethods(content);

        // Additional Marketplace Features
        addMarketplaceFeatures(content, book);
    }

    /**
     * Add vendor/shop information with ratings and location
     */
    private void addVendorInformation(StringBuilder content, Book book) {
        if (book.getShop() == null) {
            return;
        }

        Shop shop = book.getShop();
        content.append("\n=== VENDOR INFORMATION ===\n");
        content.append("Store Name: ").append(shop.getShopName()).append("\n");

        // Shop rating (calculated from actual reviews)
        ShopRatingInfo ratingInfo = calculateShopRating(shop);
        if (ratingInfo.hasRatings()) {
            content.append("Store Rating: ").append(String.format("%.1f", ratingInfo.getAverageRating()))
                   .append("/5 (").append(ratingInfo.getTotalReviews()).append(" reviews)\n");
        } else {
            content.append("Store Rating: New seller (no reviews yet)\n");
        }

        // Shop location (using available address fields)
        StringBuilder location = new StringBuilder();
        if (shop.getShopDetailAddress() != null) {
            location.append(shop.getShopDetailAddress());
        }
        if (shop.getShopWard() != null) {
            if (location.length() > 0) location.append(", ");
            location.append(shop.getShopWard());
        }
        if (shop.getShopDistrict() != null) {
            if (location.length() > 0) location.append(", ");
            location.append(shop.getShopDistrict());
        }
        if (shop.getShopProvince() != null) {
            if (location.length() > 0) location.append(", ");
            location.append(shop.getShopProvince());
        }

        if (location.length() > 0) {
            content.append("Store Location: ").append(location.toString()).append("\n");
        }

        // Shop description
        if (shop.getDescription() != null && !shop.getDescription().trim().isEmpty()) {
            String shortDesc = shop.getDescription().length() > 100
                ? shop.getDescription().substring(0, 100) + "..."
                : shop.getDescription();
            content.append("Store Description: ").append(shortDesc).append("\n");
        }

        // Shop approval status and activity
        content.append("Store Status: ");
        if (shop.getApprovalStatus() == Shop.ApprovalStatus.APPROVED && shop.isActive()) {
            content.append("Verified Active Seller\n");
        } else {
            content.append(shop.getApprovalStatus().toString()).append("\n");
        }

        // Shop views/popularity
        if (shop.getViewsCount() != null && shop.getViewsCount() > 0) {
            content.append("Store Popularity: ").append(shop.getViewsCount()).append(" views\n");
        }

        // Registration date for trust factor
        if (shop.getRegistrationDate() != null) {
            content.append("Store Since: ").append(shop.getRegistrationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        }
    }

    /**
     * Add active promotion codes and discounts
     */
    private void addActivePromotions(StringBuilder content, Book book) {
        try {
            // Get site-wide promotions
            List<Promotion> siteWidePromotions = promotionRepository.findSiteWidePromotions();

            // Get category-specific promotions for this book
            List<Promotion> categoryPromotions = promotionRepository.findCategoryPromotions(book.getBookId());

            // Filter active and non-expired promotions
            List<Promotion> activePromotions = new ArrayList<>();
            activePromotions.addAll(filterActivePromotions(siteWidePromotions));
            activePromotions.addAll(filterActivePromotions(categoryPromotions));

            if (!activePromotions.isEmpty()) {
                content.append("\n=== ACTIVE PROMOTIONS ===\n");

                for (Promotion promotion : activePromotions) {
                    content.append("Promotion Code: ").append(promotion.getCode()).append("\n");
                    content.append("Promotion Name: ").append(promotion.getName()).append("\n");

                    // Discount information
                    if (promotion.getPromotionType() == Promotion.PromotionType.PERCENTAGE_DISCOUNT) {
                        content.append("Discount: ").append(promotion.getDiscountValue()).append("% off\n");
                    } else if (promotion.getPromotionType() == Promotion.PromotionType.FIXED_AMOUNT_DISCOUNT) {
                        content.append("Discount: ").append(promotion.getDiscountValue()).append(" VND off\n");
                    }

                    // Minimum order value
                    if (promotion.getMinOrderValue() != null && promotion.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
                        content.append("Minimum Order: ").append(promotion.getMinOrderValue()).append(" VND\n");
                    }

                    // Maximum discount
                    if (promotion.getMaxDiscountAmount() != null && promotion.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                        content.append("Maximum Discount: ").append(promotion.getMaxDiscountAmount()).append(" VND\n");
                    }

                    // Expiry date
                    if (promotion.getEndDate() != null) {
                        content.append("Valid Until: ").append(promotion.getEndDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
                    }

                    // Usage limit (using available fields)
                    if (promotion.getTotalUsageLimit() != null && promotion.getCurrentUsageCount() != null) {
                        int remaining = promotion.getTotalUsageLimit() - promotion.getCurrentUsageCount();
                        content.append("Remaining Uses: ").append(remaining).append("\n");
                    }

                    // Scope information
                    content.append("Scope: ");
                    if (promotion.getScopeType().equals("SITE_WIDE")) {
                        content.append("Platform-wide promotion\n");
                    } else if (promotion.getScopeType().equals("CATEGORY")) {
                        content.append("Category-specific promotion\n");
                    }

                    content.append("---\n");
                }
            }

        } catch (Exception e) {
            logger.warn("Error loading promotions for book {}: {}", book.getBookId(), e.getMessage());
        }
    }

    /**
     * Add available payment methods and options (dynamic configuration)
     */
    private void addPaymentMethods(StringBuilder content) {
        content.append("\n=== PAYMENT OPTIONS ===\n");

        // Available payment methods from enum
        content.append("Payment Methods:\n");
        for (PaymentMethod method : PaymentMethod.values()) {
            switch (method) {
                case VNPAY:
                    content.append("- VNPay (Credit/Debit Cards, Bank Transfer, E-wallets)\n");
                    content.append("  • Visa, Mastercard, JCB cards\n");
                    content.append("  • Internet Banking (all major Vietnamese banks)\n");
                    content.append("  • E-wallets: MoMo, ZaloPay, ShopeePay\n");
                    content.append("  • QR Code payment\n");
                    if (paymentConfig.getVnpayTerminalId() != null) {
                        content.append("  • Secure payment processing enabled\n");
                    }
                    break;
                case COD:
                    content.append("- Cash on Delivery (COD)\n");
                    content.append("  • Pay when you receive the book\n");
                    content.append("  • Available for orders under 5,000,000 VND\n");
                    break;
            }
        }

        // Dynamic shipping fee information
        content.append("\nShipping & Fees:\n");
        content.append("- Standard shipping fee: ").append(paymentConfig.DEFAULT_SHIPPING_FEE).append(" VND\n");
        content.append("- Free shipping for orders above 300,000 VND\n");

        // Security and guarantees
        content.append("\nSecurity & Guarantees:\n");
        content.append("- SSL encrypted transactions\n");
        content.append("- Money-back guarantee\n");
        content.append("- Buyer protection program\n");
    }

    /**
     * Add dynamic marketplace features and services
     */
    private void addMarketplaceFeatures(StringBuilder content, Book book) {
        content.append("\n=== MARKETPLACE FEATURES ===\n");

        // Dynamic shipping information based on configuration
        content.append("Shipping Options:\n");
        content.append("- Standard Delivery: 3-5 business days (").append(paymentConfig.DEFAULT_SHIPPING_FEE).append(" VND)\n");
        content.append("- Express Delivery: 1-2 business days (additional fees apply)\n");
        content.append("- Same-day Delivery: Available in major cities\n");

        // Dynamic free shipping threshold
        BigDecimal freeShippingThreshold = new BigDecimal("300000");
        if (book.getSellingPrice() != null && book.getSellingPrice().compareTo(freeShippingThreshold) >= 0) {
            content.append("- ✅ FREE SHIPPING for this item!\n");
        } else {
            content.append("- Free shipping for orders above ").append(freeShippingThreshold).append(" VND\n");
        }

        // Return policy
        content.append("\nReturn Policy:\n");
        content.append("- 30-day return window\n");
        content.append("- Free returns for defective items\n");
        content.append("- Original packaging required\n");

        // Customer support
        content.append("\nCustomer Support:\n");
        content.append("- 24/7 chat support\n");
        content.append("- Email support: support@readhub.vn\n");

        // Quality assurance based on shop verification
        content.append("\nQuality Assurance:\n");
        if (book.getShop() != null && book.getShop().getApprovalStatus() == Shop.ApprovalStatus.APPROVED) {
            content.append("- ✅ Verified seller\n");
        }
        content.append("- Book condition guarantee\n");
        content.append("- Authenticity verification\n");

        // Premium services based on book price
        if (book.getSellingPrice() != null) {
            BigDecimal price = book.getSellingPrice();
            if (price.compareTo(new BigDecimal("500000")) >= 0) {
                content.append("- Premium packaging included\n");
                content.append("- Priority customer support\n");
            }
        }
    }

    /**
     * Filter promotions to only include active and non-expired ones
     */
    private List<Promotion> filterActivePromotions(List<Promotion> promotions) {
        LocalDateTime now = LocalDateTime.now();
        return promotions.stream()
                .filter(p -> p.getIsActive())
                .filter(p -> !p.isExpired())
                .filter(p -> !p.isNotStarted())
                .filter(p -> p.getTotalUsageLimit() == null ||
                           p.getCurrentUsageCount() == null ||
                           p.getCurrentUsageCount() < p.getTotalUsageLimit())
                .collect(Collectors.toList());
    }

    /**
     * Calculate shop rating from actual book reviews
     */
    private ShopRatingInfo calculateShopRating(Shop shop) {
        try {
            // Get all books from this shop using available repository method
            List<Book> shopBooks = bookRepository.findByShop_ShopIdAndIsActiveTrue(shop.getShopId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent();

            if (shopBooks.isEmpty()) {
                return new ShopRatingInfo(0.0, 0);
            }

            // Get all reviews for books from this shop
            List<Integer> bookIds = shopBooks.stream()
                    .map(Book::getBookId)
                    .collect(Collectors.toList());

            List<BookReview> allReviews = new ArrayList<>();
            for (Integer bookId : bookIds) {
                List<BookReview> bookReviews = bookReviewRepository.findByBookId(bookId);
                // Filter for approved reviews
                List<BookReview> approvedReviews = bookReviews.stream()
                        .filter(review -> review.isApproved())
                        .collect(Collectors.toList());
                allReviews.addAll(approvedReviews);
            }

            if (allReviews.isEmpty()) {
                return new ShopRatingInfo(0.0, 0);
            }

            // Calculate average rating
            double averageRating = allReviews.stream()
                    .mapToInt(BookReview::getRating)
                    .average()
                    .orElse(0.0);

            return new ShopRatingInfo(averageRating, allReviews.size());

        } catch (Exception e) {
            logger.warn("Error calculating shop rating for shop {}: {}", shop.getShopId(), e.getMessage());
            return new ShopRatingInfo(0.0, 0);
        }
    }

    /**
     * Helper class to hold shop rating information
     */
    private static class ShopRatingInfo {
        private final double averageRating;
        private final int totalReviews;

        public ShopRatingInfo(double averageRating, int totalReviews) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
        }

        public double getAverageRating() {
            return averageRating;
        }

        public int getTotalReviews() {
            return totalReviews;
        }

        public boolean hasRatings() {
            return totalReviews > 0;
        }
    }

    /**
     * Create comprehensive metadata for book document
     */
    private Map<String, Object> createBookMetadata(Book book) {
        Map<String, Object> metadata = new HashMap<>();

        // Basic identifiers
        metadata.put("type", "book");
        metadata.put("id", book.getBookId().toString());
        metadata.put("title", book.getTitle());
        metadata.put("authors", book.getAuthors());

        // Pricing
        metadata.put("originalPrice", book.getOriginalPrice().toString());
        if (book.getSellingPrice() != null) {
            metadata.put("sellingPrice", book.getSellingPrice().toString());
            metadata.put("hasDiscount", book.getDiscountPercentage() > 0);
            metadata.put("discountPercent", book.getDiscountPercentage());
        }

        // Availability
        metadata.put("stockQuantity", book.getStockQuantity().toString());
        metadata.put("inStock", book.getStockQuantity() > 0);

        // Quality indicators
        if (book.getAverageRating() != null) {
            metadata.put("rating", book.getAverageRating().toString());
            metadata.put("isHighlyRated", book.getAverageRating().doubleValue() >= 4.0);
        }

        if (book.getTotalReviews() != null) {
            metadata.put("totalReviews", book.getTotalReviews().toString());
        }

        // Categories
        if (book.getCategories() != null && !book.getCategories().isEmpty()) {
            String categories = book.getCategories().stream()
                    .map(category -> category.getCategoryName())
                    .collect(Collectors.joining(", "));
            metadata.put("categories", categories);
        }

        // === MARKETPLACE METADATA ===
        addMarketplaceMetadata(metadata, book);

        // Publisher
        if (book.getPublisher() != null) {
            metadata.put("publisherName", book.getPublisher().getPublisherName());
            metadata.put("publisherId", book.getPublisher().getPublisherId().toString());
        }

        // Publication details
        if (book.getPublicationDate() != null) {
            metadata.put("publicationDate", book.getPublicationDate().toString());
        }

        if (book.getNumberOfPages() != null) {
            metadata.put("numberOfPages", book.getNumberOfPages().toString());
        }

        return metadata;
    }

    // ===== REAL-TIME SYNCHRONIZATION METHODS =====

    /**
     * Update a single book document in vector store when book details change
     */
    public void updateBookDocument(Book book) {
        if (vectorStore == null) {
            logger.warn("Vector Store not available for book update");
            return;
        }

        try {
            // Remove old document
            removeBookDocument(book.getBookId());

            // Add updated document
            Document updatedDocument = createRichDocumentFromBook(book);
            vectorStore.add(List.of(updatedDocument));

            logger.info("Updated book document in vector store: {} (ID: {})", book.getTitle(), book.getBookId());

        } catch (Exception e) {
            logger.error("Error updating book document in vector store for book {}: {}", book.getBookId(), e.getMessage(), e);
        }
    }

    /**
     * Remove a book document from vector store
     */
    public void removeBookDocument(Integer bookId) {
        if (vectorStore == null) {
            logger.warn("Vector Store not available for book removal");
            return;
        }

        try {
            String documentId = "book_" + bookId;
            vectorStore.delete(List.of(documentId));
            logger.info("Removed book document from vector store: {}", documentId);

        } catch (Exception e) {
            logger.error("Error removing book document from vector store for book {}: {}", bookId, e.getMessage(), e);
        }
    }

    /**
     * Refresh promotion information across all relevant book documents
     */
    public void refreshPromotionInformation() {
        if (vectorStore == null) {
            logger.warn("Vector Store not available for promotion refresh");
            return;
        }

        try {
            logger.info("Starting promotion information refresh across all book documents");

            // Get all books and refresh their documents
            List<Book> allBooks = bookRepository.findAll();
            List<Document> updatedDocuments = new ArrayList<>();

            for (Book book : allBooks) {
                try {
                    Document updatedDocument = createRichDocumentFromBook(book);
                    updatedDocuments.add(updatedDocument);
                } catch (Exception e) {
                    logger.warn("Error creating document for book {} during promotion refresh: {}", book.getBookId(), e.getMessage());
                }
            }

            if (!updatedDocuments.isEmpty()) {
                // Clear and reload all documents
                clearVectorStore();
                vectorStore.add(updatedDocuments);
                logger.info("Refreshed promotion information for {} book documents", updatedDocuments.size());
            }

        } catch (Exception e) {
            logger.error("Error refreshing promotion information in vector store: {}", e.getMessage(), e);
        }
    }

    /**
     * Update shop information for all books belonging to a modified shop
     */
    public void updateShopInformation(Shop shop) {
        if (vectorStore == null) {
            logger.warn("Vector Store not available for shop update");
            return;
        }

        try {
            logger.info("Updating shop information for shop: {} (ID: {})", shop.getShopName(), shop.getShopId());

            // Get all books from this shop
            List<Book> shopBooks = bookRepository.findByShop_ShopIdAndIsActiveTrue(shop.getShopId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent();

            if (shopBooks.isEmpty()) {
                logger.info("No books found for shop {}", shop.getShopId());
                return;
            }

            // Update documents for all books from this shop
            for (Book book : shopBooks) {
                updateBookDocument(book);
            }

            logger.info("Updated {} book documents for shop {}", shopBooks.size(), shop.getShopName());

        } catch (Exception e) {
            logger.error("Error updating shop information in vector store for shop {}: {}", shop.getShopId(), e.getMessage(), e);
        }
    }

    /**
     * Clear all documents from vector store
     */
    public void clearVectorStore() {
        if (vectorStore == null) {
            logger.warn("Vector Store not available for clearing");
            return;
        }

        try {
            // Get all book IDs to generate document IDs for deletion
            List<Book> allBooks = bookRepository.findAll();
            List<String> documentIds = allBooks.stream()
                    .map(book -> "book_" + book.getBookId())
                    .collect(Collectors.toList());

            if (!documentIds.isEmpty()) {
                vectorStore.delete(documentIds);
                logger.info("Cleared {} documents from vector store", documentIds.size());
            }

        } catch (Exception e) {
            logger.error("Error clearing vector store: {}", e.getMessage(), e);
        }
    }

    /**
     * Perform full vector store refresh (useful for maintenance)
     */
    public void performFullRefresh() {
        logger.info("Starting full vector store refresh");
        loadBooksToVectorStore();
        logger.info("Full vector store refresh completed");
    }

    /**
     * Add marketplace-specific metadata
     */
    private void addMarketplaceMetadata(Map<String, Object> metadata, Book book) {
        // Vendor/Shop metadata
        if (book.getShop() != null) {
            Shop shop = book.getShop();
            metadata.put("shopName", shop.getShopName());
            metadata.put("shopId", shop.getShopId().toString());
            metadata.put("shopStatus", shop.getApprovalStatus().toString());
            metadata.put("isVerifiedSeller", shop.getApprovalStatus() == Shop.ApprovalStatus.APPROVED && shop.isActive());

            // Shop rating (calculated from actual reviews)
            ShopRatingInfo ratingInfo = calculateShopRating(shop);
            if (ratingInfo.hasRatings()) {
                metadata.put("shopRating", String.format("%.1f", ratingInfo.getAverageRating()));
                metadata.put("isHighRatedShop", ratingInfo.getAverageRating() >= 4.0);
                metadata.put("shopReviewCount", ratingInfo.getTotalReviews());
            } else {
                metadata.put("shopRating", "New seller");
                metadata.put("isHighRatedShop", false);
                metadata.put("shopReviewCount", 0);
            }

            // Shop location (combine address fields)
            StringBuilder location = new StringBuilder();
            if (shop.getShopDetailAddress() != null) location.append(shop.getShopDetailAddress());
            if (shop.getShopWard() != null) {
                if (location.length() > 0) location.append(", ");
                location.append(shop.getShopWard());
            }
            if (shop.getShopDistrict() != null) {
                if (location.length() > 0) location.append(", ");
                location.append(shop.getShopDistrict());
            }
            if (location.length() > 0) {
                metadata.put("shopLocation", location.toString());
            }

            if (shop.getViewsCount() != null) {
                metadata.put("shopPopularity", shop.getViewsCount().toString());
            }
        }

        // Promotion metadata
        try {
            List<Promotion> siteWidePromotions = promotionRepository.findSiteWidePromotions();
            List<Promotion> categoryPromotions = promotionRepository.findCategoryPromotions(book.getBookId());

            List<Promotion> activePromotions = new ArrayList<>();
            activePromotions.addAll(filterActivePromotions(siteWidePromotions));
            activePromotions.addAll(filterActivePromotions(categoryPromotions));

            metadata.put("hasActivePromotions", !activePromotions.isEmpty());
            metadata.put("promotionCount", activePromotions.size());

            if (!activePromotions.isEmpty()) {
                // Get the best promotion
                Promotion bestPromotion = activePromotions.stream()
                        .max((p1, p2) -> p1.getDiscountValue().compareTo(p2.getDiscountValue()))
                        .orElse(null);

                if (bestPromotion != null) {
                    metadata.put("bestPromotionCode", bestPromotion.getCode());
                    metadata.put("bestPromotionValue", bestPromotion.getDiscountValue().toString());
                    metadata.put("bestPromotionType", bestPromotion.getPromotionType().toString());
                }

                // List all promotion codes
                String promotionCodes = activePromotions.stream()
                        .map(Promotion::getCode)
                        .collect(Collectors.joining(", "));
                metadata.put("availablePromotions", promotionCodes);
            }

        } catch (Exception e) {
            logger.warn("Error loading promotion metadata for book {}: {}", book.getBookId(), e.getMessage());
            metadata.put("hasActivePromotions", false);
            metadata.put("promotionCount", 0);
        }

        // Payment and delivery metadata
        metadata.put("paymentMethods", "VNPay, COD");
        metadata.put("supportsInstallment", book.getSellingPrice() != null &&
                    book.getSellingPrice().compareTo(new BigDecimal("1000000")) >= 0);
        metadata.put("freeShipping", book.getSellingPrice() != null &&
                    book.getSellingPrice().compareTo(new BigDecimal("300000")) >= 0);

        // Quality assurance metadata
        metadata.put("hasReturnPolicy", true);
        metadata.put("hasWarranty", true);
        metadata.put("isAuthentic", true);
    }

    /**
     * Add a single book to Vector Store
     */
    public void addBookToVectorStore(Book book) {
        if (vectorStore == null) {
            logger.warn("Vector Store not available");
            return;
        }

        try {
            Document document = createRichDocumentFromBook(book);
            vectorStore.add(List.of(document));
            logger.info("Added book '{}' to Vector Store", book.getTitle());
        } catch (Exception e) {
            logger.error("Error adding book to Vector Store: {}", e.getMessage());
        }
    }

    /**
     * Remove book from Vector Store
     */
    public void removeBookFromVectorStore(Long bookId) {
        if (vectorStore == null) {
            logger.warn("Vector Store not available");
            return;
        }

        try {
            String documentId = "book_" + bookId;
            vectorStore.delete(List.of(documentId));
            logger.info("Removed book ID {} from Vector Store", bookId);
        } catch (Exception e) {
            logger.error("Error removing book from Vector Store: {}", e.getMessage());
        }
    }

    /**
     * Check if Vector Store is available
     */
    public boolean isVectorStoreAvailable() {
        return vectorStore != null;
    }

    /**
     * Get document count (simplified implementation)
     */
    public int getDocumentCount() {
        // For SimpleVectorStore, we can't easily get document count
        // Return 0 for now - this is just for admin display
        return 0;
    }
}
