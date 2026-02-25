package com.saraasansor.api.repository;

import com.saraasansor.api.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Page<PaymentTransaction> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
