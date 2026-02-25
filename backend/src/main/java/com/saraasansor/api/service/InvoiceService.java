package com.saraasansor.api.service;

import com.saraasansor.api.dto.InvoiceDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.InvoiceRecord;
import com.saraasansor.api.model.MaintenancePlan;
import com.saraasansor.api.repository.InvoiceRecordRepository;
import com.saraasansor.api.repository.MaintenancePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class InvoiceService {
    @Autowired
    private InvoiceRecordRepository repository;

    @Autowired
    private MaintenancePlanRepository maintenancePlanRepository;

    public Page<InvoiceDto> listIncoming(LocalDate start, LocalDate end, String status, Pageable pageable) {
        return listByDirection(InvoiceRecord.Direction.INCOMING, start, end, status, pageable);
    }

    public Page<InvoiceDto> listOutgoing(LocalDate start, LocalDate end, String status, Pageable pageable) {
        return listByDirection(InvoiceRecord.Direction.OUTGOING, start, end, status, pageable);
    }

    public InvoiceDto createManual(InvoiceDto dto) {
        InvoiceRecord entity = new InvoiceRecord();
        mapDto(entity, dto);
        entity.setSource("MANUAL");
        return InvoiceDto.fromEntity(repository.save(entity));
    }

    public InvoiceDto mergeInvoices(List<Long> ids) {
        if (ids == null || ids.size() < 2) {
            throw new RuntimeException("At least 2 invoices required for merge");
        }
        List<InvoiceRecord> invoices = repository.findByIdIn(ids);
        if (invoices.size() < 2) {
            throw new RuntimeException("Invoices not found for merge");
        }

        InvoiceRecord merged = new InvoiceRecord();
        merged.setDirection(invoices.get(0).getDirection());
        merged.setInvoiceDate(LocalDate.now());
        merged.setStatus("MERGED");
        merged.setSource("MERGED");
        merged.setCurrency(invoices.get(0).getCurrency());
        merged.setAmount(invoices.stream().map(InvoiceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        merged.setNote("Merged invoices: " + ids);

        InvoiceRecord saved = repository.save(merged);

        for (InvoiceRecord invoice : invoices) {
            invoice.setStatus("MERGED_CHILD");
            invoice.setMergedInto(saved);
            repository.save(invoice);
        }

        return InvoiceDto.fromEntity(saved);
    }

    public List<InvoiceDto> transferCompletedMaintenancesToSales(List<Long> maintenancePlanIds) {
        if (maintenancePlanIds == null || maintenancePlanIds.isEmpty()) {
            throw new RuntimeException("No maintenance plan selected");
        }

        return maintenancePlanIds.stream().map(this::createFromCompletedPlan).map(InvoiceDto::fromEntity).toList();
    }

    private InvoiceRecord createFromCompletedPlan(Long planId) {
        MaintenancePlan plan = maintenancePlanRepository.findById(planId)
                .orElseThrow(() -> new NotFoundException("Maintenance plan not found: " + planId));

        if (plan.getStatus() != MaintenancePlan.PlanStatus.COMPLETED) {
            throw new RuntimeException("Maintenance plan is not completed: " + planId);
        }

        if (repository.findByMaintenancePlanId(planId).isPresent()) {
            throw new RuntimeException("Already transferred to invoice: " + planId);
        }

        InvoiceRecord entity = new InvoiceRecord();
        entity.setDirection(InvoiceRecord.Direction.OUTGOING);
        entity.setInvoiceDate(LocalDate.now());
        entity.setStatus("READY_FOR_SALE");
        entity.setSource("MAINTENANCE");
        entity.setCurrency("TRY");
        entity.setAmount(plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice());
        entity.setReceiverName(plan.getElevator().getBuildingName());
        entity.setReceiverVknTckn(plan.getElevator().getManagerTcIdentityNo());
        entity.setNote(plan.getNote());
        entity.setMaintenancePlan(plan);
        return repository.save(entity);
    }

    private Page<InvoiceDto> listByDirection(InvoiceRecord.Direction direction, LocalDate start, LocalDate end, String status, Pageable pageable) {
        LocalDate from = start == null ? LocalDate.now().minusMonths(1) : start;
        LocalDate to = end == null ? LocalDate.now().plusDays(1) : end;
        String statusFilter = status == null ? "" : status;
        return repository.findByDirectionAndInvoiceDateBetweenAndStatusContainingIgnoreCase(direction, from, to, statusFilter, pageable)
                .map(InvoiceDto::fromEntity);
    }

    private void mapDto(InvoiceRecord entity, InvoiceDto dto) {
        entity.setInvoiceNo(dto.getInvoiceNo());
        entity.setInvoiceDate(dto.getInvoiceDate());
        entity.setDirection(InvoiceRecord.Direction.valueOf(dto.getDirection().toUpperCase()));
        entity.setProfile(dto.getProfile());
        entity.setStatus(dto.getStatus() == null ? "DRAFT" : dto.getStatus());
        entity.setSenderName(dto.getSenderName());
        entity.setSenderVknTckn(dto.getSenderVknTckn());
        entity.setReceiverName(dto.getReceiverName());
        entity.setReceiverVknTckn(dto.getReceiverVknTckn());
        entity.setCurrency(dto.getCurrency() == null ? "TRY" : dto.getCurrency());
        entity.setAmount(dto.getAmount() == null ? BigDecimal.ZERO : dto.getAmount());
        entity.setNote(dto.getNote());
    }
}
