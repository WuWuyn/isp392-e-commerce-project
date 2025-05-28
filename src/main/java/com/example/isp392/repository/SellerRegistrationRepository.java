package com.example.isp392.repository;

import com.example.isp392.model.SellerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    boolean existsByShopNameIgnoreCase(String shopName);
    boolean existsByContactEmailIgnoreCase(String contactEmail);
    List<SellerRegistration> findAllByOrderByRegistrationDateDesc();
}
