
import com.example.isp392.dto.OrderDTO;
import com.example.isp392.model.*;
import com.example.isp392.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/buyer")
public class ProcessCheckoutController {

    private static final Logger logger = LoggerFactory.getLogger(ProcessCheckoutController.class);
    
    private final UserService userService;
    private final UserAddressService userAddressService;
    private final OrderService orderService;
    private final CartService cartService;
    private final PromotionService promotionService;

    public ProcessCheckoutController(UserService userService,
                                   UserAddressService userAddressService,
                                   OrderService orderService,
                                   CartService cartService,
                                   PromotionService promotionService) {
        this.userService = userService;
        this.userAddressService = userAddressService;
        this.orderService = orderService;
        this.cartService = cartService;
        this.promotionService = promotionService;
    }

    @PostMapping("/process-checkout")
    public String processCheckout(@ModelAttribute OrderDTO orderDTO,
                                 Authentication authentication,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        
        logger.info("Processing checkout for order: {}", orderDTO);
        
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // Get current user
            String email = authentication.getName();
            User user = userService.findByEmail(email).orElseThrow(() -> 
                new IllegalArgumentException("User not found"));

            // Get selected items from session
            @SuppressWarnings("unchecked")
            List<CartItem> selectedItems = (List<CartItem>) session.getAttribute("checkoutItems");
            if (selectedItems == null || selectedItems.isEmpty()) {
                return "redirect:/buyer/cart?error=no_items";
            }

            // Create new order
            Order order = new Order();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setOrderStatus(OrderStatus.PENDING);
            
            // Set payment information
            PaymentMethod paymentMethod = PaymentMethod.valueOf(orderDTO.getPaymentMethod());
            order.setPaymentMethod(paymentMethod);
            order.setPaymentStatus(PaymentStatus.PENDING);
            
            // Set order notes
            order.setNotes(orderDTO.getNotes());
            
            // Process shipping address
            // Check if an existing address is selected or if we should use default address
            if (orderDTO.getExistingAddressId() != null && !orderDTO.getExistingAddressId().isEmpty() && !orderDTO.getExistingAddressId().equals("new")) {
                // Use existing address
                UserAddress address = userAddressService.findAddressById(Integer.valueOf(orderDTO.getExistingAddressId()))
                    .orElseThrow(() -> new IllegalArgumentException("Address not found"));

                order.setRecipientName(address.getRecipientName());
                order.setRecipientPhone(address.getRecipientPhone());
                order.setShippingProvince(address.getProvince());
                order.setShippingDistrict(address.getDistrict());
                order.setShippingWard(address.getWard());
                order.setShippingAddressDetail(address.getAddressDetail());
                order.setShippingCompany(address.getCompany());
                order.setShippingAddressType(address.getAddress_type());
            } else if (orderDTO.getExistingAddressId() == null || orderDTO.getExistingAddressId().isEmpty()) {
                // No address specified, try to use default address
                UserAddress defaultAddress = userAddressService.findDefaultAddress(user);
                if (defaultAddress != null) {
                    order.setRecipientName(defaultAddress.getRecipientName());
                    order.setRecipientPhone(defaultAddress.getRecipientPhone());
                    order.setShippingProvince(defaultAddress.getProvince());
                    order.setShippingDistrict(defaultAddress.getDistrict());
                    order.setShippingWard(defaultAddress.getWard());
                    order.setShippingAddressDetail(defaultAddress.getAddressDetail());
                    order.setShippingCompany(defaultAddress.getCompany());
                    order.setShippingAddressType(defaultAddress.getAddress_type());
                } else {
                    // No default address, use new address from form
                    order.setRecipientName(orderDTO.getRecipientName());
                    order.setRecipientPhone(orderDTO.getRecipientPhone());
                    order.setShippingProvince(orderDTO.getProvince());
                    order.setShippingDistrict(orderDTO.getDistrict());
                    order.setShippingWard(orderDTO.getWard());
                    order.setShippingAddressDetail(orderDTO.getAddressDetail());
                    order.setShippingCompany(orderDTO.getCompany());
                    order.setShippingAddressType(orderDTO.getAddressType());
                }
            } else {
                // Use new address
                order.setRecipientName(orderDTO.getRecipientName());
                order.setRecipientPhone(orderDTO.getRecipientPhone());
                order.setShippingProvince(orderDTO.getProvince());
                order.setShippingDistrict(orderDTO.getDistrict());
                order.setShippingWard(orderDTO.getWard());
                order.setShippingAddressDetail(orderDTO.getAddressDetail());
                order.setShippingCompany(orderDTO.getCompany());
                order.setShippingAddressType(orderDTO.getAddressType());
                
                // Save address if requested
                if (Boolean.TRUE.equals(orderDTO.getSaveAddress())) {
                    UserAddress newAddress = new UserAddress();
                    newAddress.setUser(user);
                    newAddress.setRecipientName(orderDTO.getRecipientName());
                    newAddress.setRecipientPhone(orderDTO.getRecipientPhone());
                    newAddress.setProvince(orderDTO.getProvince());
                    newAddress.setDistrict(orderDTO.getDistrict());
                    newAddress.setWard(orderDTO.getWard());
                    newAddress.setAddressDetail(orderDTO.getAddressDetail());
                    newAddress.setCompany(orderDTO.getCompany());
                    newAddress.setAddress_type(orderDTO.getAddressType());
                    newAddress.setDefault(false); // Not setting as default automatically
                    
                    userAddressService.saveAddress(newAddress);
                }
            }
            
            // Calculate order totals
            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItems = new ArrayList<>();
            
            for (CartItem item : selectedItems) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(order);
                orderItem.setBook(item.getBook());
                orderItem.setQuantity(item.getQuantity());
                orderItem.setUnitPrice(item.getBook().getSellingPrice());
                orderItem.setSubtotal(
                    item.getBook().getSellingPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
                
                orderItems.add(orderItem);
                subtotal = subtotal.add(orderItem.getSubtotal());
            }
            
            order.setOrderItems(orderItems);
            order.setSubTotal(subtotal);
            
            // Set shipping fee (fixed 30,000 VND)
            BigDecimal shippingFee = new BigDecimal(30000);
            order.setShippingFee(shippingFee);
            
            // Apply discount if applicable
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (orderDTO.getDiscountCode() != null && !orderDTO.getDiscountCode().isEmpty()) {
                try {
                    orderService.applyPromotion(order, orderDTO.getDiscountCode());
                    discountAmount = order.getDiscountAmount();
                } catch (Exception e) {
                    logger.warn("Failed to apply promotion: {}", e.getMessage());
                    // Continue without applying promotion
                    order.setDiscountAmount(BigDecimal.ZERO);
                }
            } else {
                order.setDiscountAmount(BigDecimal.ZERO);
            }
            
            // Calculate total amount
            BigDecimal totalAmount = subtotal.add(shippingFee).subtract(order.getDiscountAmount());
            order.setTotalAmount(totalAmount);
            
            // Save order
            Order savedOrder = orderService.save(order);
            
            // Remove items from cart only if not a Buy Now order
            Boolean isBuyNow = (Boolean) session.getAttribute("isBuyNow");
            if (!Boolean.TRUE.equals(isBuyNow)) {
                for (CartItem item : selectedItems) {
                    cartService.removeItem(user, item.getBook().getBook_id());
                }
            }
            
            // Clear session data
            session.removeAttribute("checkoutItems");
            session.removeAttribute("buyNowSession");
            session.removeAttribute("isBuyNow");
            session.removeAttribute("discountCode");
            session.removeAttribute("discountAmount");
            
            // Redirect to success page
            return "redirect:/buyer/order-success?orderId=" + savedOrder.getOrderId();
            
        } catch (Exception e) {
            logger.error("Error processing checkout: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/buyer/checkout?error";
        }
    }
} 