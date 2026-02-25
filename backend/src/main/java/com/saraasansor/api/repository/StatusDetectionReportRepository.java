package com.saraasansor.api.repository;

import com.saraasansor.api.model.StatusDetectionReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface StatusDetectionReportRepository extends JpaRepository<StatusDetectionReport, Long> {
    Page<StatusDetectionReport> findByReportDateBetweenAndBuildingNameContainingIgnoreCaseAndStatusContainingIgnoreCase(
            LocalDate start,
            LocalDate end,
            String buildingName,
            String status,
            Pageable pageable
    );
}
