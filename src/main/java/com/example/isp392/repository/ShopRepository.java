package com.example.isp392.repository;

import com.example.isp392.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Integer> {

    List<Shop> findByStatus(String status);

    Shop findByShopId(int shopId);

    @Query("SELECT s FROM Shop s WHERE s.user.email = :email")
    Shop findByUserEmail(String email);
}