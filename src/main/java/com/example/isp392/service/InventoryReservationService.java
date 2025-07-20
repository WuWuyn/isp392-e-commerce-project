package com.example.isp392.service;

import com.example.isp392.dto.CartItemDTO;
import com.example.isp392.dto.OrderDTO;
import com.example.isp392.model.Book;
import com.example.isp392.model.CustomerOrder;
import com.example.isp392.model.Order;
import com.example.isp392.model.OrderItem;
import com.example.isp392.model.PaymentReservation;
import com.example.isp392.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing inventory reservations during payment processing
 */
@Service
@RequiredArgsConstructor
public class InventoryReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryReservationService.class);
    
    private final BookRepository bookRepository;
    private final CustomerOrderService customerOrderService;
    private final PaymentReservationService paymentReservationService;
    
    // Map to track reserved inventory: CustomerOrderId -> ReservationInfo
    private final Map<Integer, ReservationInfo> reservations = new ConcurrentHashMap<>();
    
    /**
     * Reserve inventory for a customer order
     */
    @Transactional
    public void reserveInventory(CustomerOrder customerOrder) {
        logger.info("Reserving inventory for CustomerOrder ID: {}", customerOrder.getCustomerOrderId());
        
        try {
            for (Order order : customerOrder.getOrders()) {
                for (OrderItem item : order.getOrderItems()) {
                    reserveBookInventory(item.getBook().getBookId(), item.getQuantity());
                }
            }
            
            // Track the reservation
            ReservationInfo reservation = new ReservationInfo(
                customerOrder.getCustomerOrderId(),
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(1) // 1-minute timeout for testing
            );
            reservations.put(customerOrder.getCustomerOrderId(), reservation);
            
            logger.info("Successfully reserved inventory for CustomerOrder ID: {}", customerOrder.getCustomerOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to reserve inventory for CustomerOrder ID: {}", customerOrder.getCustomerOrderId(), e);
            // Rollback any partial reservations
            rollbackReservation(customerOrder.getCustomerOrderId());
            throw e;
        }
    }
    
    /**
     * Reserve inventory for a specific book
     */
    @Transactional
    public void reserveBookInventory(Integer bookId, int quantity) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        
        if (book.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for book: " + book.getTitle() + 
                ". Available: " + book.getStockQuantity() + ", Requested: " + quantity);
        }
        
        // Deduct from available stock (this is the reservation)
        book.setStockQuantity(book.getStockQuantity() - quantity);
        bookRepository.save(book);
        
        logger.info("Reserved {} units of book ID: {}, remaining stock: {}", 
                   quantity, bookId, book.getStockQuantity());
    }
    
    /**
     * Confirm reservation and permanently deduct inventory (called on successful payment)
     */
    @Transactional
    public void confirmReservation(Integer customerOrderId) {
        logger.info("Confirming inventory reservation for CustomerOrder ID: {}", customerOrderId);
        
        ReservationInfo reservation = reservations.remove(customerOrderId);
        if (reservation != null) {
            logger.info("Inventory reservation confirmed for CustomerOrder ID: {}", customerOrderId);
        } else {
            logger.warn("No reservation found for CustomerOrder ID: {}", customerOrderId);
        }
        
        // Inventory is already deducted, so no further action needed
        // Just remove from tracking
    }
    
    /**
     * Rollback reservation and return inventory (called on payment failure)
     */
    @Transactional
    public void rollbackReservation(Integer customerOrderId) {
        logger.info("Rolling back inventory reservation for CustomerOrder ID: {}", customerOrderId);
        
        try {
            CustomerOrder customerOrder = customerOrderService.findById(customerOrderId)
                    .orElse(null);
            
            if (customerOrder != null) {
                for (Order order : customerOrder.getOrders()) {
                    for (OrderItem item : order.getOrderItems()) {
                        returnBookInventory(item.getBook().getBookId(), item.getQuantity());
                    }
                }
            }
            
            reservations.remove(customerOrderId);
            logger.info("Successfully rolled back inventory reservation for CustomerOrder ID: {}", customerOrderId);
            
        } catch (Exception e) {
            logger.error("Failed to rollback inventory reservation for CustomerOrder ID: {}", customerOrderId, e);
        }
    }
    
    /**
     * Return inventory for a specific book
     */
    @Transactional
    public void returnBookInventory(Integer bookId, int quantity) {
        Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found: " + bookId));
        
        // Return to available stock
        book.setStockQuantity(book.getStockQuantity() + quantity);
        bookRepository.save(book);
        
        logger.info("Returned {} units of book ID: {}, new stock: {}", 
                   quantity, bookId, book.getStockQuantity());
    }
    
    /**
     * Scheduled task to clean up expired reservations (runs every minute)
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime now = LocalDateTime.now();
        
        reservations.entrySet().removeIf(entry -> {
            ReservationInfo reservation = entry.getValue();
            if (reservation.getExpiresAt().isBefore(now)) {
                logger.info("Cleaning up expired reservation for CustomerOrder ID: {}", entry.getKey());
                rollbackReservation(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Check if a reservation exists and is still valid
     */
    public boolean isReservationValid(Integer customerOrderId) {
        ReservationInfo reservation = reservations.get(customerOrderId);
        if (reservation == null) {
            return false;
        }
        
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Expired, clean it up
            rollbackReservation(customerOrderId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Inner class to track reservation information
     */
    private static class ReservationInfo {
        private final Integer customerOrderId;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        
        public ReservationInfo(Integer customerOrderId, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.customerOrderId = customerOrderId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
        
        public Integer getCustomerOrderId() { return customerOrderId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    // Map to track PaymentReservation inventory: TxnRef -> PaymentReservationInfo
    private final Map<String, PaymentReservationInfo> paymentReservations = new ConcurrentHashMap<>();

    /**
     * Reserve inventory for a payment reservation
     */
    @Transactional
    public void reserveInventoryForPayment(PaymentReservation paymentReservation) {
        logger.info("Reserving inventory for PaymentReservation ID: {}, TxnRef: {}",
                   paymentReservation.getReservationId(), paymentReservation.getVnpayTxnRef());

        try {
            // Parse OrderDTO from reservation data to get cart items
            OrderDTO orderDTO = paymentReservationService.getOrderDTOFromReservation(paymentReservation);

            for (CartItemDTO item : orderDTO.getSelectedItems()) {
                reserveBookInventory(item.getBookId(), item.getQuantity());
            }

            // Track the payment reservation
            PaymentReservationInfo reservation = new PaymentReservationInfo(
                paymentReservation.getVnpayTxnRef(),
                paymentReservation.getCreatedAt(),
                paymentReservation.getExpiresAt()
            );
            paymentReservations.put(paymentReservation.getVnpayTxnRef(), reservation);

            logger.info("Successfully reserved inventory for PaymentReservation TxnRef: {}",
                       paymentReservation.getVnpayTxnRef());

        } catch (Exception e) {
            logger.error("Failed to reserve inventory for PaymentReservation TxnRef: {}",
                        paymentReservation.getVnpayTxnRef(), e);
            throw new RuntimeException("Failed to reserve inventory for payment", e);
        }
    }

    /**
     * Confirm inventory reservation for payment (permanently deduct)
     */
    @Transactional
    public void confirmReservationByTxnRef(String vnpayTxnRef) {
        logger.info("Confirming inventory reservation for TxnRef: {}", vnpayTxnRef);

        PaymentReservationInfo reservation = paymentReservations.get(vnpayTxnRef);
        if (reservation == null) {
            logger.warn("No inventory reservation found for TxnRef: {}", vnpayTxnRef);
            return;
        }

        // Remove from tracking (inventory already deducted during reservation)
        paymentReservations.remove(vnpayTxnRef);

        logger.info("Successfully confirmed inventory reservation for TxnRef: {}", vnpayTxnRef);
    }

    /**
     * Rollback inventory reservation for payment
     */
    @Transactional
    public void rollbackReservationByTxnRef(String vnpayTxnRef, PaymentReservation paymentReservation) {
        logger.info("Rolling back inventory reservation for TxnRef: {}", vnpayTxnRef);

        try {
            // Parse OrderDTO from reservation data to get cart items
            OrderDTO orderDTO = paymentReservationService.getOrderDTOFromReservation(paymentReservation);

            for (CartItemDTO item : orderDTO.getSelectedItems()) {
                returnBookInventory(item.getBookId(), item.getQuantity());
            }

            // Remove from tracking
            paymentReservations.remove(vnpayTxnRef);

            logger.info("Successfully rolled back inventory reservation for TxnRef: {}", vnpayTxnRef);

        } catch (Exception e) {
            logger.error("Failed to rollback inventory reservation for TxnRef: {}", vnpayTxnRef, e);
            throw new RuntimeException("Failed to rollback inventory reservation", e);
        }
    }

    /**
     * Clean up expired payment reservations
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredPaymentReservations() {
        LocalDateTime now = LocalDateTime.now();

        paymentReservations.entrySet().removeIf(entry -> {
            PaymentReservationInfo reservation = entry.getValue();
            if (now.isAfter(reservation.getExpiresAt())) {
                logger.info("Cleaning up expired payment reservation for TxnRef: {}", entry.getKey());
                // Note: We don't rollback inventory here as it should be handled by payment failure
                return true;
            }
            return false;
        });
    }

    /**
     * Information about payment reservation
     */
    private static class PaymentReservationInfo {
        private final String vnpayTxnRef;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;

        public PaymentReservationInfo(String vnpayTxnRef, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.vnpayTxnRef = vnpayTxnRef;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public String getVnpayTxnRef() { return vnpayTxnRef; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }
}
