package com.example.isp392.service;

import com.example.isp392.model.*;
import com.example.isp392.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataImportExportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportExportService.class);

    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ShopService shopService;

    public DataImportExportService(BookRepository bookRepository,
                                   OrderRepository orderRepository,
                                   CategoryRepository categoryRepository,
                                   PublisherRepository publisherRepository,
                                   ShopService shopService) {
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.shopService = shopService;
    }

    /**
     * Imports books from a CSV file for a specific seller's shop.
     */
    @Transactional
    public void importBooksFromCsvForSeller(MultipartFile file, Integer shopId) throws IOException {
        Shop currentShop = shopService.getShopById(shopId);
        if (currentShop == null) {
            throw new RuntimeException("Shop not found with ID: " + shopId);
        }

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.builder().setHeader().setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord csvRecord : csvParser) {
                try {
                    // Check for existing book by ISBN within the same shop
                    String isbn = csvRecord.get("isbn");
                    if (bookRepository.findByIsbnAndShopShopId(isbn, shopId).isPresent()) {
                        log.warn("Skipping book with duplicate ISBN '{}' for Shop ID {}", isbn, shopId);
                        continue;
                    }

                    Book book = new Book();
                    book.setTitle(csvRecord.get("title"));
                    book.setAuthors(csvRecord.get("authors"));
                    book.setDescription(csvRecord.get("description"));
                    book.setIsbn(isbn);
                    book.setStockQuantity(Integer.parseInt(csvRecord.get("stockQuantity")));
                    book.setSellingPrice(new BigDecimal(csvRecord.get("sellingPrice")));
                    book.setOriginalPrice(new BigDecimal(csvRecord.get("originalPrice")));
                    book.setCoverImgUrl(csvRecord.get("coverImgUrl"));
                    book.setNumberOfPages(Integer.parseInt(csvRecord.get("numberOfPages")));
                    book.setDimensions(csvRecord.get("dimensions"));
                    book.setDateAdded(LocalDate.now());

                    // SỬA LỖI TẠI ĐÂY: Sử dụng đúng tên phương thức setActive()
                    book.setActive(true);

                    // Assign to the current seller's shop
                    book.setShop(currentShop);

                    // Handle Publisher
                    String publisherName = csvRecord.get("publisherName");
                    if (publisherName != null && !publisherName.isEmpty()) {
                        Publisher publisher = publisherRepository.findByPublisherName(publisherName)
                                .orElseGet(() -> {
                                    Publisher newPublisher = new Publisher();
                                    newPublisher.setPublisherName(publisherName);
                                    return publisherRepository.save(newPublisher);
                                });
                        book.setPublisher(publisher);
                    }

                    // Handle Categories
                    String categoriesString = csvRecord.get("categories");
                    if (categoriesString != null && !categoriesString.isEmpty()) {
                        Set<Category> categories = Arrays.stream(categoriesString.split(","))
                                .map(String::trim)
                                .map(categoryName -> categoryRepository.findByCategoryName(categoryName)
                                        .orElseGet(() -> {
                                            Category newCategory = new Category();
                                            newCategory.setCategoryName(categoryName);
                                            return categoryRepository.save(newCategory);
                                        }))
                                .collect(Collectors.toSet());
                        book.setCategories(categories);
                    }

                    bookRepository.save(book);
                    log.info("Imported book '{}' for Shop ID {}", book.getTitle(), shopId);
                } catch (Exception e) {
                    log.error("Error importing book from CSV row {}: {}", csvRecord.getRecordNumber(), e.getMessage());
                }
            }
        }
    }

    /**
     * Exports all books for a specific seller's shop to a CSV file.
     */
    public void exportBooksToCsvForSeller(PrintWriter writer, Integer shopId) throws IOException {
        // Use the new repository method
        List<Book> books = bookRepository.findByShopShopId(shopId);

        String[] headers = {
                "bookId", "title", "authors", "description", "isbn", "stockQuantity",
                "sellingPrice", "originalPrice", "coverImgUrl", "dateAdded",
                "publisherName", "categories", "numberOfPages", "dimensions", "isActive"
        };

        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build())) {
            for (Book book : books) {
                String publisherName = book.getPublisher() != null ? book.getPublisher().getPublisherName() : "";
                String categories = book.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(Collectors.joining(","));

                // SỬA LỖI TẠI ĐÂY: Sử dụng đúng tên phương thức getActive()
                csvPrinter.printRecord(
                        book.getBookId(), book.getTitle(), book.getAuthors(), book.getDescription(), book.getIsbn(),
                        book.getStockQuantity(), book.getSellingPrice(), book.getOriginalPrice(), book.getCoverImgUrl(),
                        book.getDateAdded(), publisherName, categories, book.getNumberOfPages(), book.getDimensions(), book.getActive()
                );
            }
        }
    }

    /**
     * Exports all orders related to a specific seller's shop to a CSV file.
     */
    public void exportOrdersToCsvForSeller(PrintWriter writer, Integer shopId) throws IOException {
        List<Order> orders = orderRepository.findOrdersByShopId(shopId);
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                .setHeader("orderId", "userId", "customerName", "orderDate", "totalAmount", "status", "deliveryAddress")
                .build())) {
            for (Order order : orders) {
                String deliveryAddress = "N/A";
                String customerName = "N/A";
                Integer userId = null;

                if (order.getCustomerOrder() != null) {
                    CustomerOrder co = order.getCustomerOrder();
                    deliveryAddress = String.join(", ",
                            co.getShippingAddressDetail() != null ? co.getShippingAddressDetail() : "",
                            co.getShippingWard() != null ? co.getShippingWard() : "",
                            co.getShippingDistrict() != null ? co.getShippingDistrict() : "",
                            co.getShippingProvince() != null ? co.getShippingProvince() : "");

                    if (co.getUser() != null) {
                        userId = co.getUser().getUserId();
                        customerName = co.getUser().getFullName();
                    }
                }

                csvPrinter.printRecord(order.getOrderId(), userId, customerName, order.getOrderDate(), order.getTotalAmount(), order.getOrderStatus(), deliveryAddress);
            }
        }
    }
}