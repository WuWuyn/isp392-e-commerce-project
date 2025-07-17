package com.example.isp392.repository;

import com.example.isp392.model.GroupOrder;
import com.example.isp392.model.OrderStatus;
import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupOrderRepository extends JpaRepository<GroupOrder, Integer>, JpaSpecificationExecutor<GroupOrder> {
    List<GroupOrder> findByUserUserIdOrderByCreatedAtDesc(Integer userId);
    
    List<GroupOrder> findByUserOrderByCreatedAtDesc(User user);
    
    List<GroupOrder> findByUserAndStatusOrderByCreatedAtDesc(User user, OrderStatus status);
    
    Optional<GroupOrder> findByGroupOrderIdAndUser(Integer groupOrderId, User user);
} 