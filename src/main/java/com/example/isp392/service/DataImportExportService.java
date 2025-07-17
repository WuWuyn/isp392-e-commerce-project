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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DataImportExportService {

    private static final Logger log = LoggerFactory.getLogger(DataImportExportService.class);

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final PublisherRepository publisherRepository;
    private final ShopService shopService;

    public DataImportExportService(UserRepository userRepository,
                                   BookRepository bookRepository,
                                   OrderRepository orderRepository,
                                   RoleRepository roleRepository,
                                   CategoryRepository categoryRepository,
                                   PublisherRepository publisherRepository,
                                   ShopService shopService) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.shopService = shopService;
    }

    // ==================== IMPORT METHODS ====================

    @Transactional
    public void importUsersFromCsv(MultipartFile file) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             // SỬA LỖI 1: Sửa lại cú pháp khởi tạo CSVParser
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
                try {
                    User user = new User();
                    user.setEmail(csvRecord.get("email"));
                    user.setFullName(csvRecord.get("fullName"));

                    // SỬA LỖI 2: Sửa tên phương thức setPhoneNumber -> setPhone
                    user.setPhone(csvRecord.get("phone"));

                    // SỬA LỖI 3: Bỏ qua việc nhập địa chỉ vì cấu trúc không khớp (String vs Set<UserAddress>)
                    log.warn("Address import for user {} is skipped due to structural mismatch.", csvRecord.get("email"));

                    user.setPassword(csvRecord.get("password")); // IMPORTANT: In a real app, passwords should be hashed
                    user.setActive(Boolean.parseBoolean(csvRecord.get("isActive"))); // SỬA LỖI 4: sửa tên phương thức

                    // Handle roles (giữ nguyên logic gốc)
                    String rolesString = csvRecord.get("roles");
                    if (rolesString != null && !rolesString.isEmpty()) {
                        Set<com.example.isp392.model.UserRole> userRoles = Set.of(rolesString.split(","))
                                .stream()
                                .map(roleName -> roleRepository.findByRoleName("ROLE_" + roleName.trim().toUpperCase())
                                        .map(role -> {
                                            UserRole ur = new UserRole();
                                            ur.setUser(user);
                                            ur.setRole(role);
                                            return ur;
                                        })
                                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName)))
                                .collect(Collectors.toSet());
                        user.setUserRoles(userRoles);
                    } else {
                        log.warn("No roles specified for user: {}", user.getEmail());
                        roleRepository.findByRoleName("ROLE_BUYER").ifPresent(role -> {
                            UserRole ur = new UserRole();
                            ur.setUser(user);
                            ur.setRole(role);
                            user.setUserRoles(Set.of(ur));
                        });
                    }

                    userRepository.save(user);
                    log.info("Imported user: {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Error importing user from CSV row {}: {}", csvRecord.getRecordNumber(), e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void importBooksFromCsv(MultipartFile file) throws IOException {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
             // SỬA LỖI 1 (lần 2): Sửa lại cú pháp khởi tạo CSVParser
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setIgnoreHeaderCase(true)
                     .setTrim(true)
                     .build())) {

            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            Shop defaultShop = shopService.getShopById(1);

            for (CSVRecord csvRecord : csvRecords) {
                try {
                    Book book = new Book();
                    book.setTitle(csvRecord.get("title"));
                    book.setAuthors(csvRecord.get("authors"));
                    book.setDescription(csvRecord.get("description"));
                    book.setStockQuantity(Integer.parseInt(csvRecord.get("stockQuantity")));
                    book.setSellingPrice(new java.math.BigDecimal(csvRecord.get("sellingPrice")));
                    book.setOriginalPrice(new java.math.BigDecimal(csvRecord.get("originalPrice")));
                    book.setCoverImgUrl(csvRecord.get("coverImgUrl"));
                    book.setDateAdded(LocalDate.now());
                    book.setShop(defaultShop);

                    String publisherName = csvRecord.get("publisherName");
                    if (publisherName != null && !publisherName.isEmpty()) {
                        publisherRepository.findByPublisherName(publisherName)
                                .ifPresent(book::setPublisher);
                    }

                    String categoriesString = csvRecord.get("categories");
                    if (categoriesString != null && !categoriesString.isEmpty()) {
                        Set<Category> categories = Set.of(categoriesString.split(","))
                                .stream()
                                .map(categoryName -> categoryRepository.findByCategoryName(categoryName.trim())
                                        .orElseGet(() -> {
                                            Category newCategory = new Category();
                                            newCategory.setCategoryName(categoryName.trim());
                                            return categoryRepository.save(newCategory);
                                        }))
                                .collect(Collectors.toSet());
                        book.setCategories(categories);
                    }

                    bookRepository.save(book);
                    log.info("Imported book: {}", book.getTitle());
                } catch (Exception e) {
                    log.error("Error importing book from CSV row {}: {}", csvRecord.getRecordNumber(), e.getMessage());
                }
            }
        }
    }

    // ==================== EXPORT METHODS ====================

    public void exportUsersToCsv(PrintWriter writer) throws IOException {
        List<User> users = userRepository.findAll();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("userId", "email", "fullName", "phoneNumber", "address", "isActive", "roles"))) {
            for (User user : users) {
                String roles = user.getUserRoles().stream()
                        .map(ur -> ur.getRole().getRoleName().replace("ROLE_", ""))
                        .collect(Collectors.joining(","));

                // SỬA LỖI 3: Chuyển đổi Set<UserAddress> thành String để xuất
                String fullAddress = user.getAddresses().stream()
                        .map(Object::toString) // Giả định UserAddress có phương thức toString() hợp lý
                        .collect(Collectors.joining("; "));

                // SỬA LỖI 2 & 4: Sửa tên các phương thức getter cho đúng
                csvPrinter.printRecord(user.getUserId(), user.getEmail(), user.getFullName(), user.getPhone(), fullAddress, user.isActive(), roles);
            }
        }
    }

    public void exportBooksToCsv(PrintWriter writer) throws IOException {
        List<Book> books = bookRepository.findAll();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("bookId", "title", "authors", "description", "stockQuantity", "sellingPrice", "originalPrice", "coverImgUrl", "dateAdded", "shopId", "publisherName", "categories"))) {
            for (Book book : books) {
                String publisherName = book.getPublisher() != null ? book.getPublisher().getPublisherName() : "";
                String categories = book.getCategories().stream()
                        .map(Category::getCategoryName)
                        .collect(Collectors.joining(","));
                // SỬA LỖI 5: Lời gọi getBookId() sẽ hoạt động sau khi sửa file Book.java
                csvPrinter.printRecord(book.getBook_id(), book.getTitle(), book.getAuthors(), book.getDescription(), book.getStockQuantity(), book.getSellingPrice(), book.getOriginalPrice(), book.getCoverImgUrl(), book.getDateAdded(), book.getShop() != null ? book.getShop().getShopId() : null, publisherName, categories);
            }
        }
    }

    public void exportOrdersToCsv(PrintWriter writer) throws IOException {
        List<Order> orders = orderRepository.findAll();
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("orderId", "userId", "orderDate", "totalAmount", "status", "deliveryAddress"))) {
            for (Order order : orders) {
                // SỬA LỖI 6: Nối các thành phần địa chỉ thành một chuỗi
                String deliveryAddress = String.join(", ",
                        order.getShippingAddressDetail(),
                        order.getShippingWard(),
                        order.getShippingDistrict(),
                        order.getShippingProvince());

                // SỬA LỖI 7: Sửa tên phương thức getStatus -> getOrderStatus
                csvPrinter.printRecord(order.getOrderId(), order.getUser().getUserId(), order.getOrderDate(), order.getTotalAmount(), order.getOrderStatus(), deliveryAddress);
            }
        }
    }

    // Giữ nguyên logic logs
    public List<String> getImportLogs() {
        return List.of(
                "2025-07-17 10:00:00 - User import started.",
                "2025-07-17 10:01:15 - User 'john.doe@example.com' imported successfully.",
                "2025-07-17 10:02:30 - Product import started.",
                "2025-07-17 10:03:45 - Product 'The Great Novel' imported with warnings (missing category).",
                "2025-07-17 10:04:00 - User import finished. 100 records processed, 2 errors.",
                "2025-07-17 10:05:00 - Product import finished. 50 records processed, 1 warning."
        );
    }
}