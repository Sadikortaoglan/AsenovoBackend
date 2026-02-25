package com.saraasansor.api.service;

import com.saraasansor.api.dto.PaymentReceiptDto;
import com.saraasansor.api.dto.PaymentSummaryDto;
import com.saraasansor.api.model.Maintenance;
import com.saraasansor.api.model.PaymentReceipt;
import com.saraasansor.api.repository.MaintenanceRepository;
import com.saraasansor.api.repository.PaymentReceiptRepository;
import com.saraasansor.api.util.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentReceiptService {
    
    @Autowired
    private PaymentReceiptRepository paymentReceiptRepository;
    
    @Autowired
    private MaintenanceRepository maintenanceRepository;
    
    @Autowired
    private AuditLogger auditLogger;
    
    public List<PaymentReceiptDto> getAllPaymentReceipts() {
        return paymentReceiptRepository.findAll().stream()
                .map(PaymentReceiptDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<PaymentReceiptDto> getPaymentReceiptsByDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return getAllPaymentReceipts();
        }
        
        LocalDate start = dateFrom != null ? dateFrom : LocalDate.of(1900, 1, 1);
        LocalDate end = dateTo != null ? dateTo : LocalDate.now();
        
        return paymentReceiptRepository.findByDateBetween(start, end).stream()
                .map(PaymentReceiptDto::fromEntity)
                .collect(Collectors.toList());
    }
    
    public PaymentReceiptDto createPaymentReceipt(PaymentReceiptDto dto) {
        Maintenance maintenance = maintenanceRepository.findById(dto.getMaintenanceId())
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));
        
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setMaintenance(maintenance);
        receipt.setAmount(dto.getAmount());
        receipt.setPayerName(dto.getPayerName());
        receipt.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        receipt.setNote(dto.getNote());
        
        PaymentReceipt saved = paymentReceiptRepository.save(receipt);
        
        // Mark maintenance as paid
        maintenance.setIsPaid(true);
        if (maintenance.getPaymentDate() == null) {
            maintenance.setPaymentDate(saved.getDate());
        }
        maintenanceRepository.save(maintenance);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("maintenanceId", saved.getMaintenance().getId());
        metadata.put("amount", saved.getAmount());
        metadata.put("payerName", saved.getPayerName());
        auditLogger.log("PAYMENT_RECEIPT_CREATED", "PAYMENT_RECEIPT", saved.getId(), metadata);
        
        return PaymentReceiptDto.fromEntity(saved);
    }
    
    public PaymentReceiptDto getPaymentReceiptById(Long id) {
        PaymentReceipt receipt = paymentReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment receipt not found"));
        return PaymentReceiptDto.fromEntity(receipt);
    }
    
    public PaymentReceiptDto updatePaymentReceipt(Long id, PaymentReceiptDto dto) {
        PaymentReceipt receipt = paymentReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment receipt not found"));
        
        Maintenance maintenance = maintenanceRepository.findById(dto.getMaintenanceId())
                .orElseThrow(() -> new RuntimeException("Maintenance record not found"));
        
        receipt.setMaintenance(maintenance);
        receipt.setAmount(dto.getAmount());
        receipt.setPayerName(dto.getPayerName());
        receipt.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
        receipt.setNote(dto.getNote());
        
        PaymentReceipt saved = paymentReceiptRepository.save(receipt);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("maintenanceId", saved.getMaintenance().getId());
        metadata.put("amount", saved.getAmount());
        auditLogger.log("PAYMENT_RECEIPT_UPDATED", "PAYMENT_RECEIPT", saved.getId(), metadata);
        
        return PaymentReceiptDto.fromEntity(saved);
    }
    
    public void deletePaymentReceipt(Long id) {
        PaymentReceipt receipt = paymentReceiptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment receipt not found"));
        
        paymentReceiptRepository.delete(receipt);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("amount", receipt.getAmount());
        metadata.put("payerName", receipt.getPayerName());
        auditLogger.log("PAYMENT_RECEIPT_DELETED", "PAYMENT_RECEIPT", id, metadata);
    }
    
    public PaymentSummaryDto getSummary() {
        List<PaymentReceipt> allReceipts = paymentReceiptRepository.findAll();
        
        PaymentSummaryDto summary = new PaymentSummaryDto();
        summary.setTotalCount(allReceipts.size());
        
        double totalAmount = allReceipts.stream()
                .filter(r -> r.getAmount() != null)
                .mapToDouble(PaymentReceipt::getAmount)
                .sum();
        summary.setTotalAmount(totalAmount);
        
        return summary;
    }
}
