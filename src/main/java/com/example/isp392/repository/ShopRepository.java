package com.example.isp392.repository;

import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {

    Optional<Shop> findByUserUserId(Integer userId);

    @Query("SELECT s FROM Shop s WHERE s.approval_status = :status")
    List<Shop> findByApprovalStatus(@Param("status") Shop.ApprovalStatus status);

    @Query("SELECT count(s) FROM Shop s WHERE s.approval_status = :status")
    long countByApprovalStatus(@Param("status") Shop.ApprovalStatus status);


}