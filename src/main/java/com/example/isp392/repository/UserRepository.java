package com.example.isp392.repository;

import com.example.isp392.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
// SỬA LỖI CÚ PHÁP: Dùng dấu phẩy (,) để kế thừa nhiều interface
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // GIỮ NGUYÊN PHƯƠNG THỨC NÀY: Để giải quyết Lazy Loading cho addresses
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.addresses WHERE u.userId = :userId")
    Optional<User> findByIdWithAddresses(@Param("userId") Integer userId);
}