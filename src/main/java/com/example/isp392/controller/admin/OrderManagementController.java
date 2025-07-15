package com.example.isp392.controller.admin;

import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.service.OrderService;
import com.example.isp392.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

/**
 * Controller for handling admin order management
 */
@Controller
@RequestMapping("/admin/orders")
public class OrderManagementController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderManagementController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    /**
     * Display order list page with filters
     */
    @GetMapping
    public String listOrders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "orderDate") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Model model) {

        // Create pageable with sorting
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // Convert status string to enum if provided
        OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("all")) {
            try {
                orderStatus = OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, ignore and treat as null
            }
        }

        // Convert date strings to LocalDate objects
        LocalDate fromDate = null;
        if (startDate != null && !startDate.isEmpty()) {
            try {
                fromDate = LocalDate.parse(startDate);
            } catch (Exception e) {
                // Invalid date format, ignore
            }
        }

        LocalDate toDate = null;
        if (endDate != null && !endDate.isEmpty()) {
            try {
                toDate = LocalDate.parse(endDate);
            } catch (Exception e) {
                // Invalid date format, ignore
            }
        }

        // Get orders with filters
        Page<Order> orders = orderService.findOrdersForAdmin(search, orderStatus, fromDate, toDate, pageable);

        // Add attributes to the model
        model.addAttribute("orders", orders);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", orders.getTotalPages());
        model.addAttribute("totalItems", orders.getTotalElements());
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("orderStatusList", OrderStatus.values());

        return "admin/order/order-list";
    }

    /**
     * Display order detail page
     */
    @GetMapping("/{orderId}")
    public String viewOrderDetail(@PathVariable Integer orderId, Model model) {
        Optional<Order> orderOpt = orderService.findOrderById(orderId);
        
        if (orderOpt.isEmpty()) {
            return "redirect:/admin/orders";
        }
        
        Order order = orderOpt.get();
        model.addAttribute("order", order);
        model.addAttribute("allStatuses", OrderStatus.values());
        
        return "admin/order/order-detail";
    }

    /**
     * Update order status
     */
    @PostMapping("/{orderId}/update-status")
    public String updateOrderStatus(
            @PathVariable Integer orderId,
            @RequestParam("newStatus") OrderStatus newStatus,
            @RequestParam(value = "adminNotes", required = false) String adminNotes,
            RedirectAttributes redirectAttributes) {
        
        boolean success = orderService.updateOrderStatusByAdmin(orderId, newStatus, adminNotes);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Order status updated successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update order status.");
        }
        
        return "redirect:/admin/orders/" + orderId;
    }

    /**
     * Process refund
     */
    @PostMapping("/{orderId}/process-refund")
    public String processRefund(
            @PathVariable Integer orderId,
            @RequestParam(value = "refundReason", required = false) String refundReason,
            RedirectAttributes redirectAttributes) {
        
        boolean success = orderService.processRefund(orderId, refundReason);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Refund processed successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to process refund.");
        }
        
        return "redirect:/admin/orders/" + orderId;
    }

    /**
     * Add internal notes to an order
     */
    @PostMapping("/{orderId}/add-note")
    public String addInternalNote(
            @PathVariable Integer orderId,
            @RequestParam("adminNote") String adminNote,
            RedirectAttributes redirectAttributes) {
        
        boolean success = orderService.addAdminNote(orderId, adminNote);
        
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Note added successfully.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to add note.");
        }
        
        return "redirect:/admin/orders/" + orderId;
    }
} 