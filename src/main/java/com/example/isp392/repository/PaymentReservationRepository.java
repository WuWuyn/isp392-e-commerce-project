package com.example.isp392.repository;

import com.example.isp392.model.PaymentReservation;
import com.example.isp392.model.enums.PaymentReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentReservationRepository extends JpaRepository<PaymentReservation, Integer> {
    
    /**
     * Find payment reservation by VNPay transaction reference
     */
    Optional<PaymentReservation> findByVnpayTxnRef(String vnpayTxnRef);
    
    /**
     * Find reservations by user ID and status
     */
    List<PaymentReservation> findByUserUserIdAndStatus(Integer userId, PaymentReservationStatus status);
    
    /**
     * Find expired reservations for cleanup
     */
    List<PaymentReservation> findByStatusAndExpiresAtBefore(PaymentReservationStatus status, LocalDateTime expiresAt);
    
    /**
     * Find reservations by status
     */
    List<PaymentReservation> findByStatus(PaymentReservationStatus status);
}
