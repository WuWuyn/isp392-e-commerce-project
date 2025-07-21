package com.example.isp392.service;

import com.example.isp392.model.CustomerOrder;
import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.repository.CustomerOrderRepository;
import com.example.isp392.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class CustomerOrderService {
    private final CustomerOrderRepository customerOrderRepository;
    private final OrderRepository orderRepository;
    private final BookService bookService;
    private final Lock orderStatusLock = new ReentrantLock();
    
    public CustomerOrderService(CustomerOrderRepository customerOrderRepository, 
                               OrderRepository orderRepository,
                               BookService bookService) {
        this.customerOrderRepository = customerOrderRepository;
        this.orderRepository = orderRepository;
        this.bookService = bookService;
    }
    
    /**
     * Find customer orders by user
     * @param user the user
     * @return list of customer orders
     */
    public List<CustomerOrder> findByUser(User user) {
        return customerOrderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    /**
     * Find customer orders by user and status
     * @param user the user
     * @param status the order status
     * @return list of customer orders
     */
    public List<CustomerOrder> findByUserAndStatus(User user, OrderStatus status) {
        return customerOrderRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status);
    }
    
    /**
     * Find customer order by ID
     * @param customerOrderId the customer order ID
     * @return optional customer order
     */
    public Optional<CustomerOrder> findById(Integer customerOrderId) {
        return customerOrderRepository.findById(customerOrderId);
    }
    
    /**
     * Find customer order by ID and user
     * @param customerOrderId the customer order ID
     * @param user the user
     * @return optional customer order
     */
    public Optional<CustomerOrder> findByIdAndUser(Integer customerOrderId, User user) {
        return customerOrderRepository.findByCustomerOrderIdAndUser(customerOrderId, user);
    }
    
    /**
     * Save customer order
     * @param customerOrder the customer order to save
     * @return saved customer order
     */
    public CustomerOrder save(CustomerOrder customerOrder) {
        // Calculate final total amount before saving
        customerOrder.setFinalTotalAmount(customerOrder.calculateFinalTotalAmount());
        return customerOrderRepository.save(customerOrder);
    }

    /**
     * Save customer order without recalculating total amount
     * Use this when total amount is already correctly set (e.g., from payment reservation)
     * @param customerOrder the customer order to save
     * @return saved customer order
     */
    public CustomerOrder saveWithoutRecalculation(CustomerOrder customerOrder) {
        return customerOrderRepository.save(customerOrder);
    }
    
    /**
     * Find customer orders with pagination and filtering
     * @param user the user
     * @param status the order status (optional)
     * @param dateFrom start date (optional)
     * @param dateTo end date (optional)
     * @param pageable pagination information
     * @return page of customer orders
     */
    public Page<CustomerOrder> findCustomerOrders(User user, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return customerOrderRepository.findAll((Specification<CustomerOrder>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            predicates.add(cb.equal(root.get("user"), user));
            
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), OrderStatus.valueOf(status)));
            }
            
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            }
            
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.plusDays(1).atStartOfDay()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }
    
    /**
     * Cancel customer order
     * @param customerOrderId the customer order ID
     * @param user the user
     * @param reason cancellation reason
     * @return true if cancelled successfully
     */
    public boolean cancelCustomerOrder(Integer customerOrderId, User user, String reason) {
        Optional<CustomerOrder> customerOrderOpt = findByIdAndUser(customerOrderId, user);
        if (customerOrderOpt.isEmpty()) {
            return false;
        }
        
        CustomerOrder customerOrder = customerOrderOpt.get();
        if (!customerOrder.canCancel()) {
            return false;
        }
        
        orderStatusLock.lock();
        try {
            // Cancel all individual orders
            for (Order order : customerOrder.getOrders()) {
                if (order.canCancel()) {
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancelledAt(LocalDateTime.now());
                    order.setCancellationReason(reason);
                    orderRepository.save(order);
                }
            }
            
            // Update customer order status
            customerOrder.setStatus(OrderStatus.CANCELLED);
            customerOrder.setUpdatedAt(LocalDateTime.now());
            save(customerOrder);
            
            return true;
        } finally {
            orderStatusLock.unlock();
        }
    }
    
    /**
     * Update customer order status based on individual order statuses
     * @param customerOrderId the customer order ID
     */
    public void updateCustomerOrderStatus(Integer customerOrderId) {
        Optional<CustomerOrder> customerOrderOpt = findById(customerOrderId);
        if (customerOrderOpt.isEmpty()) {
            return;
        }
        
        orderStatusLock.lock();
        try {
            CustomerOrder customerOrder = customerOrderOpt.get();
            List<Order> orders = customerOrder.getOrders();
            
            if (orders.isEmpty()) {
                return;
            }
            
            customerOrder.updateStatusFromOrders();
            customerOrder.setUpdatedAt(LocalDateTime.now());
            customerOrderRepository.save(customerOrder);
        } finally {
            orderStatusLock.unlock();
        }
    }
    
    /**
     * Get customer order count by user
     * @param user the user
     * @return count of customer orders
     */
    public long getCustomerOrderCount(User user) {
        return customerOrderRepository.countByUser(user);
    }
    
    /**
     * Find pending payment orders
     * @return list of customer orders with pending payment
     */
    public List<CustomerOrder> findPendingPaymentOrders() {
        return customerOrderRepository.findPendingPaymentOrders();
    }
    
    /**
     * Calculate total revenue from customer orders
     * @param startDate start date
     * @param endDate end date
     * @return total revenue
     */
    public BigDecimal calculateTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        List<CustomerOrder> customerOrders = customerOrderRepository.findByUserAndDateRange(null, startDate, endDate);
        return customerOrders.stream()
                .filter(co -> co.getStatus() != OrderStatus.CANCELLED)
                .map(CustomerOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Delete customer order
     * @param customerOrderId the customer order ID
     */
    public void delete(Integer customerOrderId) {
        customerOrderRepository.deleteById(customerOrderId);
    }
}
