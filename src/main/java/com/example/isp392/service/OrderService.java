package com.example.isp392.service;

import com.example.isp392.model.Order;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Lấy danh sách đơn hàng cho một người bán cụ thể.
     * @param sellerId ID của người bán.
     * @return Danh sách đơn hàng.
     */
    public List<Order> getOrdersForSeller(Integer sellerId) {
        return orderRepository.findOrdersBySellerId(sellerId);
    }

    /**
     * Cập nhật trạng thái của một đơn hàng.
     * @param orderId ID của đơn hàng.
     * @param newStatus Trạng thái mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    @Transactional
    public boolean updateOrderStatus(Integer orderId, OrderStatus newStatus) {
        return orderRepository.findById(orderId).map(order -> {
            order.setOrderStatus(newStatus);
            orderRepository.save(order);
            return true;
        }).orElse(false);
    }
    public Optional<Order> findOrderById(Integer orderId) {
        return orderRepository.findByIdWithItems(orderId);
    }
}
