package com.example.isp392.repository;

import com.example.isp392.model.ShopApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopApprovalHistoryRepository extends JpaRepository<ShopApprovalHistory, Long> {
}
