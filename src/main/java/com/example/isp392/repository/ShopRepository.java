package com.example.isp392.repository;

import com.example.isp392.model.ApprovalStatus;
import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {
    Optional<Shop> findByUserUserId(Integer userId);


    List<Shop> findByApprovalStatus(ApprovalStatus status);
}