package com.saraasansor.api.repository;

import com.saraasansor.api.model.InvoiceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, Long> {
    Page<InvoiceRecord> findByDirectionAndInvoiceDateBetweenAndStatusContainingIgnoreCase(
            InvoiceRecord.Direction direction,
            LocalDate start,
            LocalDate end,
            String status,
            Pageable pageable
    );

    Optional<InvoiceRecord> findByMaintenancePlanId(Long maintenancePlanId);

    List<InvoiceRecord> findByIdIn(List<Long> ids);
}
