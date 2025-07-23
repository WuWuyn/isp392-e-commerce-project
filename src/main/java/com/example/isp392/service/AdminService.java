package com.example.isp392.service;

import com.example.isp392.model.Book;
import com.example.isp392.model.Shop;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private final BlogRepository blogRepository;
    private final BlogCommentRepository blogCommentRepository;

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
                        ShopRepository shopRepository,
                        BlogRepository blogRepository,
                        BlogCommentRepository blogCommentRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.userRoleRepository = userRoleRepository;
        this.bookRepository = bookRepository;
        this.orderRepository = orderRepository;
        this.shopRepository = shopRepository;
        this.blogRepository = blogRepository;
        this.blogCommentRepository = blogCommentRepository;
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
            } else if (userRegistrationDateObject instanceof LocalDateTime) {
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
            } else if (orderDateObject instanceof LocalDateTime) {
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
        if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
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

    /**
     * Get comprehensive revenue analytics data for reports
     *
     * @param startDate Start date for the period
     * @param endDate End date for the period
     * @param period Period grouping (daily, weekly, monthly, yearly)
     * @param sellerId Optional seller ID to filter by
     * @param compareMode Comparison mode (previous, year)
     * @return Map containing all revenue analytics data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueAnalytics(LocalDate startDate, LocalDate endDate, String period, Integer sellerId, String compareMode) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Calculate total revenue for the period
            BigDecimal totalRevenue = orderRepository.calculatePlatformRevenue(startDate, endDate, platformCommissionRate);
            result.put("totalRevenue", totalRevenue);

            // 2. Calculate comparison period revenue and growth
            LocalDate comparisonStartDate, comparisonEndDate;
            long daysDifference = ChronoUnit.DAYS.between(startDate, endDate);

            if ("year".equals(compareMode)) {
                comparisonStartDate = startDate.minusYears(1);
                comparisonEndDate = endDate.minusYears(1);
            } else { // previous period
                comparisonStartDate = startDate.minusDays(daysDifference + 1);
                comparisonEndDate = startDate.minusDays(1);
            }

            BigDecimal comparisonRevenue = orderRepository.calculatePlatformRevenue(comparisonStartDate, comparisonEndDate, platformCommissionRate);
            double revenueGrowth = 0.0;
            if (comparisonRevenue.compareTo(BigDecimal.ZERO) > 0) {
                revenueGrowth = totalRevenue.subtract(comparisonRevenue)
                    .divide(comparisonRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            }
            result.put("comparisonRevenue", comparisonRevenue);
            result.put("revenueGrowth", revenueGrowth);

            // 3. Get revenue trend data based on period
            List<Map<String, Object>> revenueData = getRevenueTrendData(startDate, endDate, period);
            List<String> chartLabels = new ArrayList<>();
            List<BigDecimal> chartData = new ArrayList<>();

            for (Map<String, Object> data : revenueData) {
                chartLabels.add((String) data.get("label"));
                chartData.add((BigDecimal) data.get("revenue"));
            }

            result.put("revenueChartLabels", convertToJsonArray(chartLabels));
            result.put("revenueChartData", convertToJsonArray(chartData));
            result.put("revenueTrendData", revenueData);

            // 4. Get top sellers data (filtered by sellerId if provided)
            List<Map<String, Object>> topSellers;
            if (sellerId != null) {
                topSellers = getSellerRevenueDetails(sellerId, startDate, endDate);
            } else {
                topSellers = getTopSellersByRevenue(startDate, endDate, 10);
            }
            result.put("topSellers", topSellers);

            // 5. Calculate additional metrics
            long totalOrders = orderRepository.countOrdersByDateRange(startDate, endDate);
            result.put("totalOrders", totalOrders);

            BigDecimal averageOrderValue = totalOrders > 0 ?
                orderRepository.calculateTotalOrderValueByDateRange(startDate, endDate)
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;
            result.put("averageOrderValue", averageOrderValue);

            // 6. Get seller count for the period
            long activeSellers = orderRepository.countActiveSellersByDateRange(startDate, endDate);
            result.put("activeSellers", activeSellers);

        } catch (Exception e) {
            log.error("Error calculating revenue analytics: {}", e.getMessage(), e);
            // Return default values on error
            result.put("totalRevenue", BigDecimal.ZERO);
            result.put("comparisonRevenue", BigDecimal.ZERO);
            result.put("revenueGrowth", 0.0);
            result.put("revenueChartLabels", "[]");
            result.put("revenueChartData", "[]");
            result.put("topSellers", new ArrayList<>());
            result.put("totalOrders", 0L);
            result.put("averageOrderValue", BigDecimal.ZERO);
            result.put("activeSellers", 0L);
        }

        return result;
    }

    /**
     * Get revenue trend data based on period grouping
     *
     * @param startDate Start date
     * @param endDate End date
     * @param period Period grouping (daily, weekly, monthly, yearly)
     * @return List of revenue trend data
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueTrendData(LocalDate startDate, LocalDate endDate, String period) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            switch (period.toLowerCase()) {
                case "daily":
                    return getDailyRevenueTrend(startDate, endDate);
                case "weekly":
                    return getWeeklyRevenueTrend(startDate, endDate);
                case "yearly":
                    return getYearlyRevenueTrend(startDate, endDate);
                default: // monthly
                    return getMonthlyRevenueTrend(startDate, endDate);
            }
        } catch (Exception e) {
            log.error("Error getting revenue trend data: {}", e.getMessage(), e);
            return result;
        }
    }

    /**
     * Get daily revenue trend
     */
    private List<Map<String, Object>> getDailyRevenueTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate nextDate = date.plusDays(1);
            BigDecimal revenue = orderRepository.calculatePlatformRevenue(date, nextDate.minusDays(1), platformCommissionRate);

            Map<String, Object> item = new HashMap<>();
            item.put("label", date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
            item.put("revenue", revenue);
            item.put("date", date);
            result.add(item);
        }

        return result;
    }

    /**
     * Get weekly revenue trend
     */
    private List<Map<String, Object>> getWeeklyRevenueTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate weekStart = startDate.with(java.time.DayOfWeek.MONDAY);
        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            BigDecimal revenue = orderRepository.calculatePlatformRevenue(weekStart, weekEnd, platformCommissionRate);

            Map<String, Object> item = new HashMap<>();
            item.put("label", weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));
            item.put("revenue", revenue);
            item.put("date", weekStart);
            result.add(item);

            weekStart = weekStart.plusWeeks(1);
        }

        return result;
    }

    /**
     * Get monthly revenue trend
     */
    private List<Map<String, Object>> getMonthlyRevenueTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate monthStart = startDate.withDayOfMonth(1);
        while (!monthStart.isAfter(endDate)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            BigDecimal revenue = orderRepository.calculatePlatformRevenue(monthStart, monthEnd, platformCommissionRate);

            Map<String, Object> item = new HashMap<>();
            item.put("label", monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")));
            item.put("revenue", revenue);
            item.put("date", monthStart);
            result.add(item);

            monthStart = monthStart.plusMonths(1);
        }

        return result;
    }

    /**
     * Get yearly revenue trend
     */
    private List<Map<String, Object>> getYearlyRevenueTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate yearStart = startDate.withDayOfYear(1);
        while (!yearStart.isAfter(endDate)) {
            LocalDate yearEnd = yearStart.withDayOfYear(yearStart.lengthOfYear());
            if (yearEnd.isAfter(endDate)) {
                yearEnd = endDate;
            }

            BigDecimal revenue = orderRepository.calculatePlatformRevenue(yearStart, yearEnd, platformCommissionRate);

            Map<String, Object> item = new HashMap<>();
            item.put("label", String.valueOf(yearStart.getYear()));
            item.put("revenue", revenue);
            item.put("date", yearStart);
            result.add(item);

            yearStart = yearStart.plusYears(1);
        }

        return result;
    }

    /**
     * Convert list to JSON array string
     */
    private String convertToJsonArray(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            Object item = list.get(i);
            if (item instanceof String) {
                json.append("\"").append(item.toString().replace("\"", "\\\"")).append("\"");
            } else {
                json.append(item.toString());
            }
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Get top sellers by revenue for a specific period
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopSellersByRevenue(LocalDate startDate, LocalDate endDate, int limit) {
        try {
            return orderRepository.getTopSellersByRevenue(startDate, endDate, platformCommissionRate, limit);
        } catch (Exception e) {
            log.error("Error getting top sellers by revenue: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get detailed revenue information for a specific seller
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSellerRevenueDetails(Integer sellerId, LocalDate startDate, LocalDate endDate) {
        try {
            return orderRepository.getSellerRevenueDetails(sellerId, startDate, endDate, platformCommissionRate);
        } catch (Exception e) {
            log.error("Error getting seller revenue details: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Get comprehensive consolidated reports data
     *
     * @param startDate Start date for the period
     * @param endDate End date for the period
     * @param period Period grouping (daily, weekly, monthly, yearly)
     * @return Map containing all consolidated reports data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConsolidatedReports(LocalDate startDate, LocalDate endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        try {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            // 1. Orders Analytics
            Map<String, Object> ordersData = getOrdersAnalytics(startDate, endDate, period);
            result.put("orders", ordersData);

            // 2. User Registration Analytics
            Map<String, Object> usersData = getUserRegistrationAnalytics(startDateTime, endDateTime, period);
            result.put("users", usersData);

            // 3. Product Listings Analytics
            Map<String, Object> productsData = getProductListingAnalytics(startDate, endDate, period);
            result.put("products", productsData);

            // 4. Forum Activity Analytics
            Map<String, Object> forumData = getForumActivityAnalytics(startDateTime, endDateTime, period);
            result.put("forum", forumData);

            // 5. Revenue Analytics
            Map<String, Object> revenueData = getRevenueAnalytics(startDate, endDate, period, null, "previous");
            result.put("revenue", revenueData);

            // 6. Overall Platform Statistics
            Map<String, Object> platformStats = getPlatformStatistics(startDate, endDate);
            result.put("platform", platformStats);

        } catch (Exception e) {
            log.error("Error generating consolidated reports: {}", e.getMessage(), e);
            // Return empty data structure on error
            result.put("orders", new HashMap<>());
            result.put("users", new HashMap<>());
            result.put("products", new HashMap<>());
            result.put("forum", new HashMap<>());
            result.put("revenue", new HashMap<>());
            result.put("platform", new HashMap<>());
        }

        return result;
    }

    /**
     * Get orders analytics data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrdersAnalytics(LocalDate startDate, LocalDate endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        // Total orders in period
        long totalOrders = orderRepository.countOrdersByDateRange(startDate, endDate);
        result.put("totalOrders", totalOrders);

        // Average order value
        BigDecimal totalOrderValue = orderRepository.calculateTotalOrderValueByDateRange(startDate, endDate);
        BigDecimal averageOrderValue = totalOrders > 0 ?
            totalOrderValue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) :
            BigDecimal.ZERO;
        result.put("averageOrderValue", averageOrderValue);
        result.put("totalOrderValue", totalOrderValue);

        // Orders trend data
        List<Map<String, Object>> ordersTrend = getOrdersTrendData(startDate, endDate, period);
        result.put("trend", ordersTrend);
        result.put("trendLabels", convertToJsonArray(ordersTrend.stream().map(m -> m.get("label")).toList()));
        result.put("trendData", convertToJsonArray(ordersTrend.stream().map(m -> m.get("count")).toList()));

        return result;
    }

    /**
     * Get user registration analytics data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserRegistrationAnalytics(LocalDateTime startDate, LocalDateTime endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        // Total new users in period
        long newUsers = userRepository.countByRegistrationDateBetween(startDate, endDate);
        result.put("newUsers", newUsers);

        // User registration trend
        List<Map<String, Object>> userTrend = userRepository.getUserRegistrationTrend(startDate, endDate);
        result.put("trend", userTrend);
        result.put("trendLabels", convertToJsonArray(userTrend.stream().map(m -> m.get("registration_date")).toList()));
        result.put("trendData", convertToJsonArray(userTrend.stream().map(m -> m.get("count")).toList()));

        // User role distribution
        long totalBuyers = countUsersByRole("BUYER");
        long totalSellers = countUsersByRole("SELLER");
        result.put("totalBuyers", totalBuyers);
        result.put("totalSellers", totalSellers);

        return result;
    }

    /**
     * Get product listing analytics data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProductListingAnalytics(LocalDate startDate, LocalDate endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        // Total new products in period
        long newProducts = bookRepository.countByDateAddedBetween(startDate, endDate);
        result.put("newProducts", newProducts);

        // Total active products
        long totalProducts = bookRepository.countByIsActiveTrue();
        result.put("totalProducts", totalProducts);

        // Product listing trend
        List<Map<String, Object>> productTrend = bookRepository.getProductListingTrend(startDate, endDate);
        result.put("trend", productTrend);
        result.put("trendLabels", convertToJsonArray(productTrend.stream().map(m -> m.get("date_added")).toList()));
        result.put("trendData", convertToJsonArray(productTrend.stream().map(m -> m.get("count")).toList()));

        // Product statistics by category
        List<Map<String, Object>> categoryStats = bookRepository.getProductStatsByCategory();
        result.put("categoryStats", categoryStats);

        return result;
    }

    /**
     * Get forum activity analytics data
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getForumActivityAnalytics(LocalDateTime startDate, LocalDateTime endDate, String period) {
        Map<String, Object> result = new HashMap<>();

        // Blog posts in period
        long newPosts = blogRepository.countByCreatedDateBetween(startDate, endDate);
        result.put("newPosts", newPosts);

        // Comments in period
        long newComments = blogCommentRepository.countByCreatedDateBetween(startDate, endDate);
        result.put("newComments", newComments);

        // Forum activity statistics
        Map<String, Object> forumStats = blogRepository.getForumActivityStats(startDate, endDate);
        result.putAll(forumStats);

        // Blog creation trend
        List<Map<String, Object>> blogTrend = blogRepository.getBlogCreationTrend(startDate, endDate);
        result.put("blogTrend", blogTrend);
        result.put("blogTrendLabels", convertToJsonArray(blogTrend.stream().map(m -> m.get("creation_date")).toList()));
        result.put("blogTrendData", convertToJsonArray(blogTrend.stream().map(m -> m.get("count")).toList()));

        // Comment creation trend
        List<Map<String, Object>> commentTrend = blogCommentRepository.getCommentCreationTrend(startDate, endDate);
        result.put("commentTrend", commentTrend);
        result.put("commentTrendLabels", convertToJsonArray(commentTrend.stream().map(m -> m.get("creation_date")).toList()));
        result.put("commentTrendData", convertToJsonArray(commentTrend.stream().map(m -> m.get("count")).toList()));

        return result;
    }

    /**
     * Get platform statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        // Overall platform metrics
        result.put("totalUsers", userRepository.count());
        result.put("totalProducts", bookRepository.count());
        result.put("totalShops", shopRepository.count());
        result.put("activeShops", shopRepository.countByApprovalStatus(Shop.ApprovalStatus.APPROVED));
        result.put("totalBlogs", blogRepository.count());
        result.put("totalBlogViews", blogRepository.getTotalBlogViews());

        return result;
    }

    /**
     * Get orders trend data based on period grouping
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdersTrendData(LocalDate startDate, LocalDate endDate, String period) {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            switch (period.toLowerCase()) {
                case "daily":
                    return getDailyOrdersTrend(startDate, endDate);
                case "weekly":
                    return getWeeklyOrdersTrend(startDate, endDate);
                case "yearly":
                    return getYearlyOrdersTrend(startDate, endDate);
                default: // monthly
                    return getMonthlyOrdersTrend(startDate, endDate);
            }
        } catch (Exception e) {
            log.error("Error getting orders trend data: {}", e.getMessage(), e);
            return result;
        }
    }

    /**
     * Get daily orders trend
     */
    private List<Map<String, Object>> getDailyOrdersTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDate nextDate = date.plusDays(1);
            long count = orderRepository.countOrdersByDateRange(date, nextDate.minusDays(1));

            Map<String, Object> item = new HashMap<>();
            item.put("label", date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")));
            item.put("count", count);
            item.put("date", date);
            result.add(item);
        }

        return result;
    }

    /**
     * Get weekly orders trend
     */
    private List<Map<String, Object>> getWeeklyOrdersTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate weekStart = startDate.with(java.time.DayOfWeek.MONDAY);
        while (!weekStart.isAfter(endDate)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            long count = orderRepository.countOrdersByDateRange(weekStart, weekEnd);

            Map<String, Object> item = new HashMap<>();
            item.put("label", weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));
            item.put("count", count);
            item.put("date", weekStart);
            result.add(item);

            weekStart = weekStart.plusWeeks(1);
        }

        return result;
    }

    /**
     * Get monthly orders trend
     */
    private List<Map<String, Object>> getMonthlyOrdersTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate monthStart = startDate.withDayOfMonth(1);
        while (!monthStart.isAfter(endDate)) {
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            if (monthEnd.isAfter(endDate)) {
                monthEnd = endDate;
            }

            long count = orderRepository.countOrdersByDateRange(monthStart, monthEnd);

            Map<String, Object> item = new HashMap<>();
            item.put("label", monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")));
            item.put("count", count);
            item.put("date", monthStart);
            result.add(item);

            monthStart = monthStart.plusMonths(1);
        }

        return result;
    }

    /**
     * Get yearly orders trend
     */
    private List<Map<String, Object>> getYearlyOrdersTrend(LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate yearStart = startDate.withDayOfYear(1);
        while (!yearStart.isAfter(endDate)) {
            LocalDate yearEnd = yearStart.withDayOfYear(yearStart.lengthOfYear());
            if (yearEnd.isAfter(endDate)) {
                yearEnd = endDate;
            }

            long count = orderRepository.countOrdersByDateRange(yearStart, yearEnd);

            Map<String, Object> item = new HashMap<>();
            item.put("label", String.valueOf(yearStart.getYear()));
            item.put("count", count);
            item.put("date", yearStart);
            result.add(item);

            yearStart = yearStart.plusYears(1);
        }

        return result;
    }
}
