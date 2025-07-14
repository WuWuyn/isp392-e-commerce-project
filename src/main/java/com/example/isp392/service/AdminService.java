package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.User;
import com.example.isp392.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for admin-specific operations
 * This service provides functionality specifically for admin users
 */
@Service
public class AdminService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminService.class);

    @Value("${platform.commission.rate:0.10}")
    private BigDecimal platformCommissionRate; // Default to 10% if not specified in application.properties

    private final UserRepository userRepository;
    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    /**
     * Constructor with explicit dependency injection
     * Using constructor injection instead of @Autowired for better clarity and testability
     * 
     * @param userRepository repository for user data access
     */
    public AdminService(UserRepository userRepository,
                        UserService userService,
                        UserRoleRepository userRoleRepository,
                        BookRepository bookRepository,
                        OrderRepository orderRepository,
                        ShopRepository shopRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userRoleRepository = userRoleRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
    }
    
    /**
     * Get the currently authenticated admin user
     * 
     * @return Optional containing the admin user if found and authenticated
     */
    @Transactional(readOnly = true)
    public Optional<User> getCurrentAdminUser() {
        // Get the current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Optional.empty();
        }
        
        // Find the user by email
        return userRepository.findByEmail(auth.getName());
    }
    
    /**
     * Extract the first name from a full name
     * 
     * @param fullName the full name to extract from
     * @return the first name (first word) or the full name if no spaces
     */
    public String extractFirstName(String fullName) {
        if (fullName == null || fullName.isEmpty()) {
            return "";
        }
        
        // Split by space and return the first part
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : fullName;
    }

    public void addAdminInfoToModel(Model model) {
        getCurrentAdminUser().ifPresent(adminUser -> {
            model.addAttribute("user", adminUser);
            model.addAttribute("roles", userService.getUserRoles(adminUser));
            model.addAttribute("firstName", extractFirstName(adminUser.getFullName()));
        });
    }

    /**
     * Count users registered within the specified number of days
     *
     * @param days Number of days to look back
     * @return Count of new users
     */
    @Transactional(readOnly = true)
    public long countNewUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return userRepository.countByRegistrationDateAfter(cutoffDate);
    }

    /**
     * Count users by role
     *
     * @param roleName Role name to count
     * @return Count of users with the specified role
     */
    @Transactional(readOnly = true)
    public Long countUsersByRole(String roleName) {
        return userRoleRepository.countByRoleNameAndActive(roleName, true);
    }

    /**
     * Count products added within the specified number of days
     *
     * @param days Number of days to look back
     * @return Count of new products
     */
    @Transactional(readOnly = true)
    public long countNewProducts(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        return bookRepository.countByDateAddedAfter(cutoffDate);
    }

    /**
     * Calculate total platform revenue (commission from all orders)
     *
     * @return Total platform revenue
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalPlatformRevenue() {
        BigDecimal totalSales = orderRepository.calculateTotalOrderValue();
        return totalSales.multiply(platformCommissionRate);
    }

    /**
     * Get monthly platform revenue for the last several months
     *
     * @param months Number of months to include
     * @return List of monthly revenue data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyPlatformRevenue(int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months - 1).withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();

        List<Map<String, Object>> monthlyRevenue = orderRepository.getMonthlyRevenue(startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> data : monthlyRevenue) {
            Map<String, Object> item = new HashMap<>();
            String month = (String) data.get("month");
            // Convert "yyyy-MM" to "MMM" (e.g., "Jan") for better chart display
            LocalDate date = LocalDate.parse(month + "-01");
            item.put("month", date.format(java.time.format.DateTimeFormatter.ofPattern("MMM")));

            BigDecimal sales = (BigDecimal) data.get("revenue");
            BigDecimal commission = sales.multiply(platformCommissionRate);

            item.put("revenue", commission);
            result.add(item);
        }

        return result;
    }

    /**
     * Get top selling products
     *
     * @param limit Maximum number of products to return
     * @return List of top selling products
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopSellingProducts(int limit) {
        return orderRepository.getTopSellingProducts(limit);
    }

    /**
     * Get top sellers by revenue
     *
     * @param limit Maximum number of sellers to return
     * @return List of top sellers
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopSellers(int limit) {
        return orderRepository.getTopSellers(limit);
    }

    /**
     * Get recent orders
     *
     * @param limit Maximum number of orders to return
     * @return List of recent orders
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrders(int limit) {
        return orderRepository.getRecentPlatformOrders(limit);
    }

    /**
     * Get recent activities (user registrations, orders, shop approvals, etc.)
     *
     * @param limit Maximum number of activities to return
     * @return List of recent activities
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivities(int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();

        // Add recent user registrations
        List<User> recentUsers = userRepository.findRecentUsers(limit);
        for (User user : recentUsers) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "user_registration");
            activity.put("description", "New user registered: " + user.getFullName());
            // Ensure user.getRegistrationDate() is handled as LocalDateTime
            Object userRegistrationDateObject = user.getRegistrationDate();
            if (userRegistrationDateObject instanceof java.sql.Timestamp) {
                activity.put("timestamp", ((java.sql.Timestamp) userRegistrationDateObject).toLocalDateTime());
            } else if (userRegistrationDateObject instanceof java.time.LocalDateTime) {
                activity.put("timestamp", userRegistrationDateObject);
            } else {
                activity.put("timestamp", null); // Handle other types or null gracefully
            }
            activities.add(activity);
        }

        // Add recent orders
        List<Map<String, Object>> recentOrders = getRecentOrders(limit);
        for (Map<String, Object> order : recentOrders) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "order");
            activity.put("description", "New order #" + order.get("order_id") + " placed by " + order.get("customer_name"));

            // Convert java.sql.Timestamp to java.time.LocalDateTime
            Object orderDateObject = order.get("order_date");
            if (orderDateObject instanceof java.sql.Timestamp) {
                activity.put("timestamp", ((java.sql.Timestamp) orderDateObject).toLocalDateTime());
            } else if (orderDateObject instanceof java.time.LocalDateTime) {
                activity.put("timestamp", orderDateObject);
            } else {
                activity.put("timestamp", null); // Handle other types or null gracefully
            }
            activities.add(activity);
        }

        // Sort by timestamp (most recent first) and limit
        activities.sort((a, b) -> {
            // Safely convert objects to LocalDateTime for comparison
            LocalDateTime timeA = convertToLocalDateTime(a.get("timestamp"));
            LocalDateTime timeB = convertToLocalDateTime(b.get("timestamp"));

            // Handle null timestamps: nulls come last
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1; // b is not null, so b comes first
            if (timeB == null) return -1; // a is not null, so a comes first

            return timeB.compareTo(timeA); // Reverse order for newest first
        });

        // Limit the list size
        return activities.size() <= limit ? activities : activities.subList(0, limit);
    }

    /**
     * Helper method to convert an object to LocalDateTime, handling Timestamp.
     * @param obj The object to convert.
     * @return Converted LocalDateTime or null if conversion is not possible.
     */
    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        }
        if (obj instanceof java.time.LocalDateTime) {
            return (java.time.LocalDateTime) obj;
        }
        // Handle other cases or log a warning if an unexpected type is encountered
        return null;
    }

    /**
     * Get total number of orders across the platform
     * @return Total count of orders
     */
    @Transactional(readOnly = true)
    public long countAllOrders() {
        return orderRepository.countAllOrders();
    }

    /**
     * Calculate average order value across the platform
     * @return Average order value
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAverageOrderValue() {
        BigDecimal totalRevenue = orderRepository.calculateTotalOrderValue();
        long totalOrders = orderRepository.countAllOrders();

        if (totalOrders > 0) {
            return totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, BigDecimal.ROUND_HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get total views for all books across the platform
     * @return Total views
     */
    @Transactional(readOnly = true)
    public int getTotalPlatformViews() {
        Integer total = bookRepository.getTotalViewsAllBooks();
        return total != null ? total : 0;
    }

    /**
     * Get top viewed products across the platform
     * @param limit Maximum number of products to return
     * @return List of top viewed products with views data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopViewedProducts(int limit) {
        List<Object[]> raw = bookRepository.getTopViewedBooks(limit);
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

    /**
     * Get weekly platform revenue for the last several weeks
     *
     * @param weeks Number of weeks to include
     * @return List of weekly revenue data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getWeeklyPlatformRevenue(int weeks) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(weeks - 1);

        // Generate a list of week start dates
        List<LocalDate> weekStartDates = new ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            weekStartDates.add(currentDate);
            currentDate = currentDate.plusWeeks(1);
        }

        // Create a map to store revenue by week
        Map<String, BigDecimal> revenueByWeek = new LinkedHashMap<>();
        for (LocalDate date : weekStartDates) {
            LocalDate weekEnd = date.plusDays(6);
            BigDecimal weeklyRevenue = orderRepository.calculatePlatformRevenue(date, weekEnd, platformCommissionRate);
            String weekLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"));
            revenueByWeek.put(weekLabel, weeklyRevenue);
        }

        // Convert to list of maps for return
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revenueByWeek.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("week", entry.getKey());
            item.put("revenue", entry.getValue());
            result.add(item);
        }

        return result;
    }

    /**
     * Get daily platform revenue for the last 7 days
     *
     * @return List of daily revenue data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDailyPlatformRevenue() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // Last 7 days including today

        // Create a map to store revenue by day with zero values for all days
        Map<String, BigDecimal> revenueByDay = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dayLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/M"));
            revenueByDay.put(dayLabel, BigDecimal.ZERO);
        }

        // Query the database once for all days in the range using the new repository method
        try {
            List<Map<String, Object>> dailyRevenue = orderRepository.getDailyPlatformRevenue(
                    startDate, endDate, platformCommissionRate);

            // Update the map with actual revenue data
            for (Map<String, Object> day : dailyRevenue) {
                java.sql.Date sqlDate = (java.sql.Date) day.get("order_date");
                LocalDate date = sqlDate.toLocalDate();
                String dayLabel = date.format(java.time.format.DateTimeFormatter.ofPattern("dd/M"));
                BigDecimal revenue = (BigDecimal) day.get("daily_revenue");
                revenueByDay.put(dayLabel, revenue);
            }
        } catch (Exception e) {
            log.error("Error fetching daily revenue data: {}", e.getMessage());
            // Continue with zero values if there's an error
        }

        // Convert to list of maps for return
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : revenueByDay.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("day", entry.getKey());
            item.put("revenue", entry.getValue());
            result.add(item);
        }

        return result;
    }

    /**
     * Generate empty daily revenue data for the given date range
     *
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @return List of daily revenue data with zero values
     */
    private List<Map<String, Object>> generateEmptyDailyRevenueData(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            Map<String, Object> item = new HashMap<>();
            String dayLabel = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/M"));
            item.put("day", dayLabel);
            item.put("revenue", BigDecimal.ZERO);
            result.add(item);
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    /**
     * Calculate total revenue for the last 7 days
     *
     * @return Total revenue for the last 7 days
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateLast7DaysRevenue() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // Last 7 days including today
        return orderRepository.calculatePlatformRevenue(startDate, endDate, platformCommissionRate);
    }
}
