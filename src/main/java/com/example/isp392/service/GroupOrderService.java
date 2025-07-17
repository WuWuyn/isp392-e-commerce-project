package com.example.isp392.service;

import com.example.isp392.model.GroupOrder;
import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import com.example.isp392.repository.GroupOrderRepository;
import com.example.isp392.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Transactional
public class GroupOrderService {
    private final GroupOrderRepository groupOrderRepository;
    private final OrderRepository orderRepository;
    private final BookService bookService;
    private final Lock orderStatusLock = new ReentrantLock();
    
    public GroupOrderService(GroupOrderRepository groupOrderRepository, 
                           OrderRepository orderRepository,
                           BookService bookService) {
        this.groupOrderRepository = groupOrderRepository;
        this.orderRepository = orderRepository;
        this.bookService = bookService;
    }
    
    public List<GroupOrder> findByUser(User user) {
        return groupOrderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public List<GroupOrder> findByUserAndStatus(User user, OrderStatus status) {
        return groupOrderRepository.findByUserAndStatusOrderByCreatedAtDesc(user, status);
    }
    
    public Optional<GroupOrder> findById(Integer groupOrderId) {
        return groupOrderRepository.findById(groupOrderId);
    }
    
    public Optional<GroupOrder> findByIdAndUser(Integer groupOrderId, User user) {
        return groupOrderRepository.findByGroupOrderIdAndUser(groupOrderId, user);
    }
    
    public Page<GroupOrder> findGroupOrders(User user, String status, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return groupOrderRepository.findAll((Specification<GroupOrder>) (root, query, cb) -> {
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
    
    @Transactional
    public void cancelGroupOrder(Integer groupOrderId) {
        try {
            orderStatusLock.lock();
            GroupOrder groupOrder = groupOrderRepository.findById(groupOrderId)
                .orElseThrow(() -> new RuntimeException("Group order not found"));
                
            // Cancel all orders in the group
            for (Order order : groupOrder.getOrders()) {
                if (order.getOrderStatus() != OrderStatus.CANCELLED) {
                    OrderStatus oldStatus = order.getOrderStatus();
                    order.setOrderStatus(OrderStatus.CANCELLED);
                    order.setCancelledAt(LocalDateTime.now());
                    
                    // Return items to inventory
                    if (oldStatus == OrderStatus.PENDING || oldStatus == OrderStatus.PROCESSING) {
                        order.getOrderItems().forEach(item -> {
                            bookService.increaseStockQuantity(item.getBook().getBookId(), item.getQuantity());
                        });
                    }
                    
                    orderRepository.save(order);
                }
            }
            
            groupOrder.setStatus(OrderStatus.CANCELLED);
            groupOrder.setUpdatedAt(LocalDateTime.now());
            groupOrderRepository.save(groupOrder);
        } finally {
            orderStatusLock.unlock();
        }
    }
    
    @Transactional
    public void updateGroupOrderStatus(GroupOrder groupOrder) {
        try {
            orderStatusLock.lock();
            // Get all orders in the group
            List<Order> orders = groupOrder.getOrders();
            
            // Check if all orders are in the same status
            boolean allCancelled = orders.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.CANCELLED);
            boolean allCompleted = orders.stream().allMatch(o -> o.getOrderStatus() == OrderStatus.SHIPPED);
            
            if (allCancelled) {
                groupOrder.setStatus(OrderStatus.CANCELLED);
            } else if (allCompleted) {
                groupOrder.setStatus(OrderStatus.SHIPPED);
            } else {
                // Set to the least progressed status
                OrderStatus minStatus = orders.stream()
                    .map(Order::getOrderStatus)
                    .min((s1, s2) -> {
                        // Custom ordering for statuses
                        return Integer.compare(getStatusOrder(s1), getStatusOrder(s2));
                    })
                    .orElse(OrderStatus.PENDING);
                groupOrder.setStatus(minStatus);
            }
            
            groupOrder.setUpdatedAt(LocalDateTime.now());
            groupOrderRepository.save(groupOrder);
        } finally {
            orderStatusLock.unlock();
        }
    }
    
    private int getStatusOrder(OrderStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case PROCESSING -> 1;
            case SHIPPED -> 2;
            case CANCELLED -> 3;
            default -> 4;
        };
    }
} 