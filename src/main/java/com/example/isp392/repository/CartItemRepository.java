package com.example.isp392.repository;

import com.example.isp392.model.User;
import com.example.isp392.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.book b JOIN FETCH b.shop s WHERE ci.cart.user = :user AND ci.book.bookId IN :bookIds")
    List<CartItem> findByCartUserAndBookIds(@Param("user") User user, @Param("bookIds") List<Integer> bookIds);
}
