package com.example.isp392.service;

import com.example.isp392.dto.OrderDTO;
import com.example.isp392.model.PaymentReservation;
import com.example.isp392.model.User;
import com.example.isp392.model.enums.PaymentReservationStatus;
import com.example.isp392.repository.PaymentReservationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentReservationService.class);
    
    private final PaymentReservationRepository paymentReservationRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a VNPay payment reservation
     */
    public PaymentReservation createVNPayReservation(User user, OrderDTO orderDTO, 
                                                     BigDecimal totalAmount, BigDecimal shippingFee, 
                                                     BigDecimal discountAmount) {
        try {
            PaymentReservation reservation = new PaymentReservation();
            reservation.setUser(user);
            reservation.setVnpayTxnRef(generateUniqueTxnRef());
            reservation.setTotalAmount(totalAmount);
            reservation.setShippingFee(shippingFee);
            reservation.setDiscountAmount(discountAmount);
            reservation.setPaymentMethod(com.example.isp392.model.PaymentMethod.VNPAY);
            reservation.setStatus(PaymentReservationStatus.PENDING);
            
            // Store shipping address
            reservation.setShippingAddressDetail(orderDTO.getAddressDetail());
            reservation.setShippingWard(orderDTO.getWard());
            reservation.setShippingDistrict(orderDTO.getDistrict());
            reservation.setShippingProvince(orderDTO.getProvince());
            reservation.setRecipientName(orderDTO.getRecipientName());
            reservation.setRecipientPhone(orderDTO.getRecipientPhone());
            reservation.setNotes(orderDTO.getNotes());
            
            // Serialize OrderDTO to JSON for later order creation
            String reservationData = objectMapper.writeValueAsString(orderDTO);
            reservation.setReservationData(reservationData);
            
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setExpiresAt(LocalDateTime.now().plusMinutes(15)); // 15 minutes for VNPay
            
            PaymentReservation saved = paymentReservationRepository.save(reservation);
            logger.info("Created payment reservation with ID: {}, TxnRef: {}", 
                       saved.getReservationId(), saved.getVnpayTxnRef());
            
            return saved;
            
        } catch (Exception e) {
            logger.error("Failed to create VNPay payment reservation", e);
            throw new RuntimeException("Failed to create payment reservation", e);
        }
    }
    
    /**
     * Find payment reservation by VNPay transaction reference
     */
    public Optional<PaymentReservation> findByVnpayTxnRef(String vnpayTxnRef) {
        return paymentReservationRepository.findByVnpayTxnRef(vnpayTxnRef);
    }
    
    /**
     * Confirm payment reservation (mark as paid)
     */
    public void confirmReservation(PaymentReservation reservation) {
        if (!reservation.canBeConfirmed()) {
            throw new IllegalStateException("Cannot confirm reservation in current state: " + reservation.getStatus());
        }
        
        reservation.confirm();
        paymentReservationRepository.save(reservation);
        
        logger.info("Confirmed payment reservation with ID: {}", reservation.getReservationId());
    }
    
    /**
     * Cancel payment reservation
     */
    public void cancelReservation(PaymentReservation reservation) {
        reservation.cancel();
        paymentReservationRepository.save(reservation);
        
        logger.info("Cancelled payment reservation with ID: {}", reservation.getReservationId());
    }
    
    /**
     * Get OrderDTO from reservation data
     */
    public OrderDTO getOrderDTOFromReservation(PaymentReservation reservation) {
        try {
            return objectMapper.readValue(reservation.getReservationData(), OrderDTO.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize OrderDTO from reservation data", e);
            throw new RuntimeException("Failed to parse reservation data", e);
        }
    }
    
    /**
     * Generate unique transaction reference for VNPay
     */
    private String generateUniqueTxnRef() {
        String txnRef;
        do {
            // Generate a unique transaction reference
            txnRef = "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (paymentReservationRepository.findByVnpayTxnRef(txnRef).isPresent());
        
        return txnRef;
    }
    
    /**
     * Clean up expired reservations (should be called by scheduled task)
     */
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        var expiredReservations = paymentReservationRepository.findByStatusAndExpiresAtBefore(
            PaymentReservationStatus.PENDING, now);
        
        for (PaymentReservation reservation : expiredReservations) {
            reservation.expire();
            paymentReservationRepository.save(reservation);
        }
        
        if (!expiredReservations.isEmpty()) {
            logger.info("Expired {} payment reservations", expiredReservations.size());
        }
    }
}
