package com.example.isp392.controller;

import com.example.isp392.model.CustomerOrder;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.service.CustomerOrderService;
import com.example.isp392.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/buyer/customer-orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final UserService userService;

    public CustomerOrderController(CustomerOrderService customerOrderService, UserService userService) {
        this.customerOrderService = customerOrderService;
        this.userService = userService;
    }

    @GetMapping
    public String getCustomerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Model model,
            Authentication authentication) {
        
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get customer orders with status filter
        Page<CustomerOrder> customerOrderPage = customerOrderService.findCustomerOrders(
            user, status, dateFrom, dateTo, PageRequest.of(page, 10)
        );

        model.addAttribute("customerOrders", customerOrderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customerOrderPage.getTotalPages());
        
        // Preserve filter parameters for pagination links
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramDateFrom", dateFrom);
        model.addAttribute("paramDateTo", dateTo);
        
        return "buyer/customer-orders";
    }

    @GetMapping("/{customerOrderId}")
    public String getCustomerOrderDetail(@PathVariable Integer customerOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return "redirect:/buyer/customer-orders";
        }
        
        model.addAttribute("customerOrder", customerOrderOpt.get());
        return "buyer/customer-order-detail";
    }
    
    @PostMapping("/{customerOrderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelCustomerOrder(@PathVariable Integer customerOrderId, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng không tồn tại");
        }
        
        CustomerOrder customerOrder = customerOrderOpt.get();
        if (!customerOrder.canCancel()) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn hàng ở trạng thái hiện tại");
        }
        
        customerOrderService.cancelCustomerOrder(customerOrderId, user, "Hủy bởi khách hàng");
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đơn hàng đã được hủy thành công");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam Integer customerOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return "redirect:/buyer/customer-orders";
        }

        model.addAttribute("customerOrder", customerOrderOpt.get());
        return "buyer/payment-success";
    }
    
    @GetMapping("/payment-failed")
    public String paymentFailed(@RequestParam Integer customerOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<CustomerOrder> customerOrderOpt = customerOrderService.findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return "redirect:/buyer/customer-orders";
        }

        model.addAttribute("customerOrder", customerOrderOpt.get());
        return "buyer/payment-failed";
    }
}
