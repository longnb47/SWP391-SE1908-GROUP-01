package com.se1908.group01.repository;

import com.se1908.group01.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionNo(
            String transactionNo);
}