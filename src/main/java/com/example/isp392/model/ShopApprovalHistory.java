package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shop_approval_history")
public class ShopApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id", nullable = false)
    private User processedBy; // Admin đã thực hiện

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shop.ApprovalStatus status; // Trạng thái mới (APPROVED, REJECTED)

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String reason; // Lý do từ chối

    @Column(nullable = false)
    private LocalDateTime processedAt;

    public ShopApprovalHistory(Shop shop, User processedBy, Shop.ApprovalStatus status, String reason) {
        this.shop = shop;
        this.processedBy = processedBy;
        this.status = status;
        this.reason = reason;
        this.processedAt = LocalDateTime.now();
    }
}