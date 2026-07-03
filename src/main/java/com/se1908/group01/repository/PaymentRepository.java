package com.se1908.group01.repository;

import com.se1908.group01.entity.Payment;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionNo(
            String transactionNo);

    List<Payment> findByUser(User user);

    long countByStatus(PaymentStatus status);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM Payment p
            WHERE p.status = 'SUCCESS'
           """)
    BigDecimal getTotalRevenue();
}