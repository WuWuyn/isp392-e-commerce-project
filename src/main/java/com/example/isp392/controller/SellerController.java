package com.example.isp392.controller;

import com.example.isp392.dto.BookFormDTO;
import com.example.isp392.dto.UserRegistrationDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/seller")
public class SellerController {

    private static final Logger log = LoggerFactory.getLogger(SellerController.class);
    private final UserService userService;
    private final BookService bookService;
    private final ShopService shopService;
    private final CategoryService categoryService;
    private final PublisherService publisherService;
    private final OrderService orderService;
    private final OtpService otpService;
    private final EmailService emailService;

    /**
     * Constructor for dependency injection
     * @param userService Service for user-related operations
     * @param bookService Service for book-related operations
     * @param shopService Service for shop-related operations
     * @param categoryService Service for category-related operations
     * @param publisherService Service for publisher-related operations
     * @param orderService Service for order-related operations
     */
    public SellerController(UserService userService, BookService bookService, ShopService shopService, 
                           CategoryService categoryService, PublisherService publisherService,
                           OrderService orderService, OtpService otpService, EmailService emailService) {
        this.userService = userService;
        this.bookService = bookService;
        this.shopService = shopService;
        this.categoryService = categoryService;
        this.publisherService = publisherService;
        this.orderService = orderService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "seller/seller-login";
    }

    //TODO: WHEN signup redirect to buyer register then to seller-registration.html
//    @GetMapping("/signup")
//    public String showSignupPage(Model model) {
//        model.addAttribute("userRegistrationDTO", new UserRegistrationDTO());
//        return "seller/seller-signup";
//    }
//
//    @PostMapping("/signup")
//    public String registerSeller(
//            @Valid @ModelAttribute("userRegistrationDTO") UserRegistrationDTO userRegistrationDTO,
//            BindingResult bindingResult,
//            RedirectAttributes redirectAttributes) {
//        if (bindingResult.hasErrors()) {
//            return "seller/seller-signup";
//        }
//        try {
//            userService.registerNewUser(userRegistrationDTO, "SELLER");
//            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
//            return "redirect:/seller/login";
//        } catch (Exception e) {
//            log.error("Error registering seller: {}", e.getMessage());
//            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
//            return "redirect:/seller/signup";
//        }
//    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        
        try {
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/dashboard";
            }
            
            // Get dashboard statistics from real data
            // 1. Recent orders count (last 7 days)
            int newOrdersCount = orderService.getNewOrdersCount(shop.getShopId(), 7);
            
            // 2. Today's revenue
            BigDecimal todayRevenue = orderService.getTodayRevenue(shop.getShopId());
            
            // 3. Active products count
            long activeProductsCount = bookService.countActiveBooksByShopId(shop.getShopId());
            
            // 4. Low stock products
            List<Book> lowStockProducts = bookService.findLowStockBooksByShopId(shop.getShopId(), 5);
            
            // 5. Recent orders for activity feed
            List<Map<String, Object>> recentOrders = orderService.getRecentOrders(shop.getShopId(), 5);
            
            // 6. Weekly revenue data for chart
            List<BigDecimal> weeklyRevenue = orderService.getWeeklyRevenue(shop.getShopId());
            
            // NEW: Get total revenue and total orders for the shop (all time)
            LocalDate registrationDate;
            LocalDateTime regDateTime = shopService.getRegistrationDateByShopId(shop.getShopId());
            if (regDateTime != null) {
                registrationDate = regDateTime.toLocalDate();
            } else {
                registrationDate = LocalDate.of(2000, 1, 1);
            }
            LocalDate now = LocalDate.now();
            BigDecimal totalRevenue = orderService.getTotalRevenue(shop.getShopId(), registrationDate, now);
            Long totalOrders = orderService.getTotalOrders(shop.getShopId(), registrationDate, now);
            if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
            if (totalOrders == null) totalOrders = 0L;
            BigDecimal averageOrderValue = BigDecimal.ZERO;
            if (totalOrders > 0) {
                averageOrderValue = totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, BigDecimal.ROUND_HALF_UP);
            }
            model.addAttribute("averageOrderValue", averageOrderValue);
            
            // Views: total and per-product
            int totalViews = bookService.getTotalViewsByShopId(shop.getShopId());
            List<Map<String, Object>> productViews = bookService.getViewsByProductInShop(shop.getShopId());
            model.addAttribute("totalViews", totalViews);
            model.addAttribute("productViews", productViews);
            // For chart.js: push product titles and views as JSON arrays
            List<String> productTitles = new ArrayList<>();
            List<Integer> productViewsCounts = new ArrayList<>();
            for (Map<String, Object> pv : productViews) {
                productTitles.add((String) pv.get("title"));
                productViewsCounts.add(pv.get("viewsCount") != null ? ((Number) pv.get("viewsCount")).intValue() : 0);
            }

            model.addAttribute("productViewsLabelsJson", safeConvertToJsonArray(productTitles));
            model.addAttribute("productViewsDataJson", safeConvertToJsonArray(productViewsCounts));
            
            // Add all attributes to model
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("shop", shop);
            model.addAttribute("newOrdersCount", newOrdersCount);
            model.addAttribute("todayRevenue", todayRevenue);
            model.addAttribute("activeProductsCount", activeProductsCount);
            model.addAttribute("lowStockProducts", lowStockProducts);
            model.addAttribute("recentOrders", recentOrders);
            model.addAttribute("weeklyRevenue", weeklyRevenue);
            
            // Convert weekly revenue to JSON array for chart.js
            // Ensure we're sending numeric values, not strings
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < weeklyRevenue.size(); i++) {
                sb.append(weeklyRevenue.get(i).toString());
                if (i < weeklyRevenue.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            String weeklyRevenueJson = sb.toString();
            
            model.addAttribute("weeklyRevenueJson", weeklyRevenueJson);
            
            log.debug("Dashboard loaded for shop ID: {}", shop.getShopId());
            log.debug("Weekly revenue JSON: {}", weeklyRevenueJson);
            
        } catch (Exception e) {
            log.error("Error loading dashboard data: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading dashboard data: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
        }
        
        return "seller/dashboard";
    }
    
    /**
     * Display analytics and reports page
     *
     * @param model Model to add attributes
     * @param period Period for analytics (daily, weekly, monthly)
     * @param startDate Start date for custom period
     * @param endDate End date for custom period
     * @param compareMode Compare mode (previous, year)
     * @return analytics page view
     */
    @GetMapping("/analytics")
    public String showAnalytics(
            Model model,
            @RequestParam(defaultValue = "monthly") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "previous") String compareMode) {
        
        User user = getCurrentUser();
        if (user == null) {
            return "redirect:/seller/login";
        }
        
        try {
            // Get seller's shop
            Shop shop = shopService.getShopByUserId(user.getUserId());
            if (shop == null) {
                model.addAttribute("errorMessage", "Shop not found.");
                model.addAttribute("user", user);
                model.addAttribute("roles", userService.getUserRoles(user));
                return "seller/dashboard";
            }

            // Sanitize period, set default dates, validate range
            if (!Arrays.asList("daily", "weekly", "monthly").contains(period)) {
                period = "monthly"; // Default to monthly if invalid period
            }

            LocalDate now = LocalDate.now();
            LocalDateTime registrationDateTime = shopService.getRegistrationDateByShopId(shop.getShopId());
            LocalDate registrationDate = registrationDateTime != null ? registrationDateTime.toLocalDate() : now.minusMonths(6);
            if (startDate == null || endDate == null) {
                switch (period) {
                    case "daily":
                        startDate = registrationDate;
                        endDate = now;
                        break;
                    case "weekly":
                        startDate = registrationDate;
                        endDate = now;
                        break;
                    case "monthly":
                    default:
                        startDate = registrationDate;
                        endDate = now;
                        period = "monthly";
                        break;
                }
            } else {
                if (startDate.isBefore(registrationDate)) {
                    startDate = registrationDate;
                }
            }

            if (startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }

            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 366) {
                startDate = endDate.minusYears(1);
                log.info("Date range exceeded maximum allowed, limiting to 1 year");
            }

            // Get total revenue and orders directly for the entire period
            BigDecimal totalRevenue = orderService.getTotalRevenue(shop.getShopId(), registrationDate, endDate);
            long totalOrders = orderService.getTotalOrders(shop.getShopId(), registrationDate, endDate);
            
            // Generate labels for x-axis based on period
            List<String> timeLabels = generateTimeLabels(startDate, endDate, period);
            
            // Fetch the combined data for revenue and orders per period
            List<Map<String, Object>> periodicDataRaw;
            try {
                periodicDataRaw = orderService.getRevenueByPeriod(shop.getShopId(), startDate, endDate, period);
                if (periodicDataRaw == null) {
                    periodicDataRaw = new ArrayList<>();
                    log.warn("Revenue data returned null for shop ID: {}", shop.getShopId());
                }
            } catch (Exception e) {
                periodicDataRaw = new ArrayList<>();
                log.error("Error retrieving revenue data: {}", e.getMessage());
            }
            log.debug("raw revenue {}",periodicDataRaw);

            // Lấy revenue cho kỳ trước
            LocalDate previousStartDate, previousEndDate;
            if ("year".equals(compareMode)) {
                previousStartDate = startDate.minusYears(1);
                previousEndDate = endDate.minusYears(1);
            } else { // previous
                long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                previousEndDate = startDate.minusDays(1);
                previousStartDate = previousEndDate.minusDays(days-1);
            }
            List<String> previousTimeLabels = generateTimeLabels(previousStartDate, previousEndDate, period);
            List<Map<String, Object>> previousDataRaw;
            try {
                previousDataRaw = orderService.getRevenueByPeriod(shop.getShopId(), previousStartDate, previousEndDate, period);
                if (previousDataRaw == null) previousDataRaw = new ArrayList<>();
            } catch (Exception e) {
                previousDataRaw = new ArrayList<>();
                log.error("Error retrieving previous period revenue: {}", e.getMessage());
            }
            // Map time label to revenue for previous
            Map<String, Map<String, Object>> previousDataByPeriod = new HashMap<>();
            for (Map<String, Object> data : previousDataRaw) {
                if (data == null) continue;
                String timePeriod = (String) data.get("time_period");
                if (timePeriod != null) previousDataByPeriod.put(timePeriod, data);
            }
            List<BigDecimal> previousPeriodRevenue = new ArrayList<>();
            for (String label : previousTimeLabels) {
                Map<String, Object> data = previousDataByPeriod.getOrDefault(label, new HashMap<>());
                BigDecimal revenue = getBigDecimalFromMap(data, "revenue");
                previousPeriodRevenue.add(revenue);
            }
            String previousRevenueDataJson = safeConvertToJsonArray(previousPeriodRevenue);
            model.addAttribute("previousRevenueDataJson", previousRevenueDataJson);
            model.addAttribute("compareMode", compareMode);

            // FIX: Re-structure the data processing loop to handle all chart data at once
            Map<String, Map<String, Object>> dataByPeriod = new HashMap<>();
            for (Map<String, Object> data : periodicDataRaw) {
                if (data == null) continue;
                String timePeriod = (String) data.get("time_period");
                if (timePeriod != null) {
                    dataByPeriod.put(timePeriod, data);
                }
            }

            // Get views data separately from BookService
            int totalViews = bookService.getTotalViewsByShopId(shop.getShopId());
            List<Map<String, Object>> productViews = bookService.getViewsByProductInShop(shop.getShopId());

            List<String> productTitlesForChart = new ArrayList<>();
            List<Integer> productViewsCountsForChart = new ArrayList<>();
            for (Map<String, Object> pv : productViews) {
                productTitlesForChart.add((String) pv.get("title"));
                productViewsCountsForChart.add(pv.get("viewsCount") != null ? ((Number) pv.get("viewsCount")).intValue() : 0);
            }

            // ADD: Initialize all data lists here
            List<BigDecimal> periodRevenue = new ArrayList<>();
            List<Integer> periodOrders = new ArrayList<>();
            List<Double> periodConversionRate = new ArrayList<>();

            // FIX: A single, clean loop to populate all lists
            for (String label : timeLabels) {
                Map<String, Object> data = dataByPeriod.getOrDefault(label, new HashMap<>());
                
                // Process Revenue
                BigDecimal revenue = getBigDecimalFromMap(data, "revenue");
                periodRevenue.add(revenue);
                
                // Process Orders
                int orders = getIntFromMap(data, "order_count");
                periodOrders.add(orders);
                
                // Process Conversion Rate (using totalViews for overall avg for now, per-period views not fetched)
                // This will be based on the overall totalViews for the period, not per-interval views
                // If you want per-interval views, you'll need to adjust the BookService query.
                double conversionRate = (totalOrders > 0) ? ((double) orders / totalOrders) * 100 : 0.0;
                periodConversionRate.add(Math.round(conversionRate * 10) / 10.0);
            }

            // Calculate average conversion rate with safeguard against division by zero
            // This calculation should use totalOrders and totalViews over the whole period
            double avgConversionRate = (totalOrders > 0 && totalViews > 0) ? (totalOrders * 100.0 / totalViews) : 0;
            avgConversionRate = Math.round(avgConversionRate * 10) / 10.0;

            // ... (The rest of your code for top products and geo distribution is fine)
            List<Map<String, Object>> topProductsRaw;
            List<Map<String, Object>> geoDistributionRaw;

            try {
                topProductsRaw = orderService.getBestsellingBooks(shop.getShopId(), 5);
                if (topProductsRaw == null) topProductsRaw = new ArrayList<>();
            } catch (Exception e) {
                topProductsRaw = new ArrayList<>();
                log.error("Error retrieving bestselling products: {}", e.getMessage());
            }

            try {
                geoDistributionRaw = orderService.getGeographicDistribution(shop.getShopId());
                if (geoDistributionRaw == null) geoDistributionRaw = new ArrayList<>();
            } catch (Exception e) {
                geoDistributionRaw = new ArrayList<>();
                log.error("Error retrieving geographic distribution: {}", e.getMessage());
            }

            List<Map<String, Object>> topProducts = new ArrayList<>();
            for (Map<String, Object> mp : topProductsRaw) {
                if (mp == null) continue;
                Map<String, Object> np = new HashMap<>();
                np.put("title", mp.getOrDefault("title", "N/A"));
                np.put("totalQuantity", getIntFromMap(mp, "total_quantity"));
                np.put("totalRevenue", getBigDecimalFromMap(mp, "total_revenue"));
                topProducts.add(np);
            }

            List<Map<String, Object>> geoDistribution = new ArrayList<>();
            List<String> geoLabels = new ArrayList<>();
            List<Integer> geoCounts = new ArrayList<>();
            for (Map<String, Object> mp : geoDistributionRaw) {
                if (mp == null) continue;
                Map<String, Object> np = new HashMap<>();
                String region = String.valueOf(mp.getOrDefault("region", "Unknown"));
                np.put("region", region);
                int count = getIntFromMap(mp, "order_count");
                np.put("orderCount", count);
                geoDistribution.add(np);
                geoLabels.add(region);
                geoCounts.add(count);
            }
            
            // Safe conversion of data to JSON strings for chart.js
            String geoLabelsJson = safeConvertToJsonArray(geoLabels);
            String geoDataJson = safeConvertToJsonArray(geoCounts);
            String timeLabelsJson = safeConvertToJsonArray(timeLabels);
            String revenueDataJson = safeConvertToJsonArray(periodRevenue);
            String ordersDataJson = safeConvertToJsonArray(periodOrders);
            String conversionRateDataJson = safeConvertToJsonArray(periodConversionRate);
            
            // Add view-specific data
            model.addAttribute("productViewsLabelsJson", safeConvertToJsonArray(productTitlesForChart));
            model.addAttribute("productViewsDataJson", safeConvertToJsonArray(productViewsCountsForChart));

            // Add data to model
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("shop", shop);
            model.addAttribute("period", period);
            model.addAttribute("startDate", startDate);
            model.addAttribute("endDate", endDate);

            // Add chart data as JSON strings
            model.addAttribute("timeLabelsJson", timeLabelsJson);
            model.addAttribute("revenueDataJson", revenueDataJson);
            model.addAttribute("ordersDataJson", ordersDataJson);
            model.addAttribute("viewsDataJson", safeConvertToJsonArray(productViewsCountsForChart)); // Ensure this maps to actual views data
            model.addAttribute("conversionRateDataJson", conversionRateDataJson);
            model.addAttribute("geoLabelsJson", geoLabelsJson);
            model.addAttribute("geoDataJson", geoDataJson);

            // Add processed data for tables
            model.addAttribute("topProducts", topProducts);
            model.addAttribute("geoDistribution", geoDistribution);
            
            // Summary statistics
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalViews", totalViews); // Use the correct totalViews from BookService
            model.addAttribute("avgConversionRate", avgConversionRate);
            
            int totalGeoOrders = geoCounts.stream().mapToInt(Integer::intValue).sum();
            if (totalGeoOrders == 0) {
                totalGeoOrders = 1;
            }
            
            model.addAttribute("geoTotalOrders", totalGeoOrders);
            log.debug("Analytics loaded for shop ID: {} with period: {}", shop.getShopId(), period);
            log.debug("Revenue Data Json Type: {} with data: {}",shop.getShopId(),revenueDataJson);
            return "seller/analytics";
            
        } catch (Exception e) {
            // ... (Your existing catch block is good)
            log.error("Error loading analytics: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Error loading analytics: " + e.getMessage());
            model.addAttribute("user", user);
            model.addAttribute("roles", userService.getUserRoles(user));
            model.addAttribute("timeLabelsJson", "[]");
            model.addAttribute("revenueDataJson", "[]");
            model.addAttribute("ordersDataJson", "[]");
            model.addAttribute("viewsDataJson", "[]"); // Ensure this is empty array on error
            model.addAttribute("conversionRateDataJson", "[]");
            model.addAttribute("topProducts", new ArrayList<>());
            model.addAttribute("geoDistribution", new ArrayList<>());
            model.addAttribute("totalRevenue", BigDecimal.ZERO);
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalViews", 0);
            model.addAttribute("avgConversionRate", 0.0);
            model.addAttribute("period", "monthly");
            model.addAttribute("startDate", LocalDate.now().minusMonths(6));
            model.addAttribute("endDate", LocalDate.now());

            return "seller/analytics";
        }
    }

    // ADD: Helper methods to safely extract numbers from the map
    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        return BigDecimal.ZERO;
    }

    private int getIntFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            // Handle potential Long from SQL COUNT
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Convert a list to a JSON array string safely using the standard Jackson library.
     * This is the recommended approach.
     * 
     * @param <T> Type of list elements
     * @param list List to convert
     * @return JSON array string
     */
    private <T> String safeConvertToJsonArray(List<T> list) {
        if (list == null) {
            return "[]";
        }
        try {
            // ObjectMapper is thread-safe, you can make it a private final field
            // in your class for even better performance.
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("Error converting list to JSON array using Jackson: {}", e.getMessage());
            return "[]"; // Fallback on error
        }
    }
    
    /**
     * Generate time labels for x-axis based on period
     * 
     * @param startDate Start date of the period
     * @param endDate End date of the period
     * @param period Period type (daily, weekly, monthly)
     * @return List of time labels
     */
    private List<String> generateTimeLabels(LocalDate startDate, LocalDate endDate, String period) {
        List<String> labels = new ArrayList<>();
        DateTimeFormatter formatter;
        
        switch (period) {
            case "daily":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    labels.add(date.format(formatter));
                }
                break;
                
            case "weekly":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'w");
                LocalDate weekStart = startDate;
                while (!weekStart.isAfter(endDate)) {
                    labels.add(weekStart.format(formatter));
                    weekStart = weekStart.plusWeeks(1);
                }
                break;
                
            case "monthly":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                LocalDate monthStart = startDate.withDayOfMonth(1);
                while (!monthStart.isAfter(endDate)) {
                    labels.add(monthStart.format(formatter));
                    monthStart = monthStart.plusMonths(1);
                }
                break;
        }
        
        return labels;
    }

    /**
     * Display account info page
     *
     * @param model Model to add attributes
     * @return account info page view
     */
    @GetMapping("/account-info")
    public String showAccountInfo(Model model, Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user == null) {
            return "redirect:/seller/login";
        }
        model.addAttribute("user", user);
        model.addAttribute("roles", userService.getUserRoles(user));
        return "seller/account-info";
    }

    @GetMapping("/delete-shop")
    public String showDeleteShopPage(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/seller/login";
        }
        User currentUser = userService.findByEmailDirectly(principal.getName());
        if (currentUser != null) {
            Optional<Shop> shopOptional = shopService.findShopByUserId(currentUser.getUserId());
            if (shopOptional.isEmpty()) {
                model.addAttribute("error", "You do not own a shop.");
                return "seller/dashboard"; // Or another appropriate page
            }
        }
        return "seller/delete-shop";
    }

    @PostMapping("/shop-delete-request")
    public String requestShopDeletion(@RequestParam("password") String password,
                                      Principal principal,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/seller/login";
        }

        User currentUser = userService.findByEmailDirectly(principal.getName());
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/seller/delete-shop";
        }

        Shop shop = shopService.getShopByUserId(currentUser.getUserId());
        if (shop == null) {
            redirectAttributes.addFlashAttribute("error", "No shop found for your account.");
            return "redirect:/seller/delete-shop";
        }

        if (!userService.checkIfValidOldPassword(currentUser, password)) {
            redirectAttributes.addFlashAttribute("error", "Invalid password. Please try again.");
            return "redirect:/seller/delete-shop";
        }



        String baseUrl = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                .replacePath(null)
                .build().toUriString();

        boolean emailSent = shopService.requestShopDeletion(shop, currentUser, baseUrl);

        if (emailSent) {
            redirectAttributes.addFlashAttribute("success", "A confirmation email has been sent to your email address. Please click the link in the email to complete the shop deletion.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Failed to send confirmation email. Please try again later.");
        }
        return "redirect:/seller/delete-shop";
    }

    @GetMapping("/shop-delete-confirm")
    public String confirmShopDeletion(@RequestParam("token") String token,
                                      RedirectAttributes redirectAttributes,
                                      HttpServletRequest request) {
        User seller = shopService.confirmShopDeletion(token);

        if (seller == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid or expired shop deletion link.");
            return "redirect:/seller/login";
        }

        // Invalidate seller session after shop deletion
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        emailService.sendShopDeletionSuccessEmail(seller.getEmail());

        redirectAttributes.addFlashAttribute("success", "Your shop has been successfully deleted.");
        return "redirect:/seller/login";
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            username = ((OAuth2User) principal).getAttribute("email");
        } else {
            username = principal.toString();
        }
        return userService.findByEmailDirectly(username);
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2AuthenticationToken) {
            OAuth2User oauth2User = ((OAuth2AuthenticationToken) principal).getPrincipal();
            username = oauth2User.getAttribute("email");
            if (username == null) {
                // Fallback for other OAuth2 providers that might use different attributes
                username = oauth2User.getName();
            }
        } else {
            username = principal.toString();
        }
        return userService.findByEmailDirectly(username);
    }

    //all order
    @GetMapping("/orders")
    public String listOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) OrderStatus status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Model model,
            Authentication authentication
    ) {
        // 1. Lấy thông tin người bán đang đăng nhập
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User seller = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Seller not found"));
        Integer sellerId = seller.getUserId();

        // 2. Tạo Pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());

        // 3. Lấy danh sách Order gốc từ Service (vẫn có thể chứa sản phẩm của người khác)
        Page<Order> originalOrderPage = orderService.searchOrdersForSeller(sellerId, keyword, status, startDate, endDate, pageable);

        // ===== START: PHẦN SỬA LỖI QUAN TRỌNG =====
        // 4. Lọc lại danh sách orderItems bên trong mỗi Order
        originalOrderPage.getContent().forEach(order -> {
            // Lọc danh sách orderItems, chỉ giữ lại những item thuộc về người bán này
            List<OrderItem> sellerItems = order.getOrderItems().stream()
                    .filter(item -> {
                        // Kiểm tra xem item này có thuộc về người bán đang đăng nhập không
                        // Cẩn thận NullPointerException nếu có dữ liệu không nhất quán
                        return item.getBook() != null &&
                                item.getBook().getShop() != null &&
                                item.getBook().getShop().getUser() != null &&
                                item.getBook().getShop().getUser().getUserId().equals(sellerId);
                    })
                    .toList();

            // Cập nhật lại danh sách items của đơn hàng bằng danh sách đã được lọc
            order.setOrderItems(sellerItems);
        });

        // 5. Gửi dữ liệu đã được xử lý an toàn tới view
        model.addAttribute("orderPage", originalOrderPage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);

        return "seller/orders";
    }

    @GetMapping("/orders/{id}")
    public String showOrderDetailPage(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return "redirect:/seller/login";
        }
        Optional<Order> orderOpt = orderService.findOrderByIdForSeller(id, currentUser.getUserId());
        if (orderOpt.isPresent()) {
            model.addAttribute("order", orderOpt.get());
            model.addAttribute("user", currentUser);
            return "seller/order-detail";
        } else {
            return "redirect:/seller/orders";
        }
    }

    @PostMapping("/orders/update-status/{orderId}")
    public String updateOrderStatus(@PathVariable("orderId") Integer orderId,
                                    @RequestParam("newStatus") OrderStatus newStatus,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {

        // Lấy thông tin người bán để kiểm tra quyền sở hữu
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User seller = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // Kiểm tra xem đơn hàng có thuộc về người bán này không trước khi cập nhật
        Optional<Order> orderOptional = orderService.findOrderByIdForSeller(orderId, seller.getUserId());

        if (orderOptional.isPresent()) {
            // Nếu có, thực hiện cập nhật
            boolean isSuccess = orderService.updateOrderStatus(orderId, newStatus);
            if (isSuccess) {
                redirectAttributes.addFlashAttribute("successMessage", "Order #" + orderId + " status updated to " + newStatus.name());
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to update order status.");
            }
        } else {
            // Nếu không, báo lỗi không có quyền
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found or you do not have permission to modify it.");
        }

        return "redirect:/seller/orders";
    }
    @GetMapping("/orders/print-label/{id}")
    public String showPrintLabelPage(@PathVariable("id") Integer orderId, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        // 1. Lấy thông tin người bán đang đăng nhập
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User seller = userService.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        // 2. Tìm đơn hàng, đảm bảo đơn hàng này thuộc về người bán đang đăng nhập
        Optional<Order> orderOptional = orderService.findOrderByIdForSeller(orderId, seller.getUserId());

        // 3. Xử lý kết quả
        if (orderOptional.isPresent()) {
            // Nếu tìm thấy đơn hàng, gửi thông tin đơn hàng tới trang view
            model.addAttribute("order", orderOptional.get());
            // Trả về một template được thiết kế riêng cho việc in
            return "seller/print-label";
        } else {
            // Nếu không tìm thấy (do sai ID hoặc không thuộc quyền sở hữu), redirect về trang danh sách và báo lỗi
            redirectAttributes.addFlashAttribute("errorMessage", "Order not found or you do not have permission to view it.");
            return "redirect:/seller/orders";
        }
    }

    /**
     * REST API endpoint to check ISBN availability
     * 
     * @param isbn ISBN to check
     * @return JSON response indicating if ISBN is available
     */
    @GetMapping("/api/check-isbn")
    @ResponseBody
    public Map<String, Object> checkIsbnAvailability(@RequestParam String isbn) {
        Map<String, Object> response = new HashMap<>();
        
        if (isbn == null || isbn.trim().isEmpty()) {
            response.put("available", false);
            response.put("message", "ISBN cannot be empty");
            return response;
        }
        
        boolean isAvailable = !bookService.isbnExists(isbn.trim());
        response.put("available", isAvailable);
        response.put("message", isAvailable ? "ISBN is available" : "ISBN already exists");
        
        return response;
    }

}