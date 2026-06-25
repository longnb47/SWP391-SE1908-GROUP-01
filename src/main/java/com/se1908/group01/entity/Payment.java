package com.se1908.group01.entity;

import com.se1908.group01.enums.PaymentMethod;
import com.se1908.group01.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(
                        name = "idx_payment_txn",
                        columnList = "transactionNo"
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Column(unique = true, nullable = false, length = 50)
    private String transactionNo;

    private String vnpTransactionNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @Column(length = 500)
    private String paymentNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @PrePersist
    public void prePersist() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (transactionNo == null || transactionNo.isBlank()) {
            transactionNo = UUID.randomUUID()
                    .toString()
                    .replace("-", "");
        }

        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
}