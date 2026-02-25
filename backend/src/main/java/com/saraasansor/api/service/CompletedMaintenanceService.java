package com.saraasansor.api.service;

import com.saraasansor.api.dto.MaintenancePlanResponseDto;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.repository.MaintenancePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class CompletedMaintenanceService {
    @Autowired
    private MaintenancePlanRepository maintenancePlanRepository;

    public Page<MaintenancePlanResponseDto> listCompleted(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        LocalDateTime start = from == null ? LocalDateTime.now().minusYears(2) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(1) : to;
        return maintenancePlanRepository.findCompletedPage(MaintenancePlan.PlanStatus.COMPLETED, start, end, pageable)
                .map(MaintenancePlanResponseDto::fromEntity);
    }
}
