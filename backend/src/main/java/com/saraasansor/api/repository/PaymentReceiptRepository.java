package com.saraasansor.api.repository;

import com.saraasansor.api.model.PaymentReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentReceiptRepository extends JpaRepository<PaymentReceipt, Long> {
    List<PaymentReceipt> findByMaintenanceIdOrderByDateDesc(Long maintenanceId);
    
    @Query("SELECT pr FROM PaymentReceipt pr WHERE pr.date >= :dateFrom AND pr.date <= :dateTo ORDER BY pr.date DESC")
    List<PaymentReceipt> findByDateBetween(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
}

