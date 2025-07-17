package com.example.isp392.controller;

import com.example.isp392.model.GroupOrder;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.service.GroupOrderService;
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
@RequestMapping("/buyer/group-orders")
public class GroupOrderController {

    private final GroupOrderService groupOrderService;
    private final UserService userService;

    public GroupOrderController(GroupOrderService groupOrderService, UserService userService) {
        this.groupOrderService = groupOrderService;
        this.userService = userService;
    }

    @GetMapping
    public String getGroupOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            Model model,
            Authentication authentication) {
        
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get group orders with status filter
        Page<GroupOrder> groupOrderPage = groupOrderService.findGroupOrders(
            user, status, dateFrom, dateTo, PageRequest.of(page, 10)
        );

        model.addAttribute("groupOrders", groupOrderPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", groupOrderPage.getTotalPages());
        
        // Preserve filter parameters for pagination links
        model.addAttribute("paramStatus", status);
        model.addAttribute("paramDateFrom", dateFrom);
        model.addAttribute("paramDateTo", dateTo);
        
        return "buyer/group-orders";
    }

    @GetMapping("/{groupOrderId}")
    public String getGroupOrderDetail(@PathVariable Integer groupOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<GroupOrder> groupOrderOpt = groupOrderService.findByIdAndUser(groupOrderId, user);
        if (groupOrderOpt.isEmpty()) {
            return "redirect:/buyer/group-orders";
        }
        
        model.addAttribute("groupOrder", groupOrderOpt.get());
        return "buyer/group-order-detail";
    }
    
    @PostMapping("/{groupOrderId}/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelGroupOrder(@PathVariable Integer groupOrderId, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        Optional<GroupOrder> groupOrderOpt = groupOrderService.findByIdAndUser(groupOrderId, user);
        if (groupOrderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng nhóm không tồn tại");
        }
        
        GroupOrder groupOrder = groupOrderOpt.get();
        if (!groupOrder.canCancel()) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn hàng nhóm ở trạng thái hiện tại");
        }
        
        groupOrderService.cancelGroupOrder(groupOrderId);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đơn hàng đã được hủy thành công");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/payment-success")
    public String paymentSuccess(@RequestParam Integer groupOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<GroupOrder> groupOrderOpt = groupOrderService.findByIdAndUser(groupOrderId, user);
        if (groupOrderOpt.isEmpty()) {
            return "redirect:/buyer/group-orders";
        }

        model.addAttribute("groupOrder", groupOrderOpt.get());
        return "buyer/payment-success";
    }
    
    @GetMapping("/payment-failed")
    public String paymentFailed(@RequestParam Integer groupOrderId, Model model, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<GroupOrder> groupOrderOpt = groupOrderService.findByIdAndUser(groupOrderId, user);
        if (groupOrderOpt.isEmpty()) {
            return "redirect:/buyer/group-orders";
        }

        model.addAttribute("groupOrder", groupOrderOpt.get());
        return "buyer/payment-failed";
    }
} 