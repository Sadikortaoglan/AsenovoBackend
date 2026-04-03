package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.BankCollectionCreateRequest;
import com.saraasansor.api.dto.CashCollectionCreateRequest;
import com.saraasansor.api.dto.CheckCollectionCreateRequest;
import com.saraasansor.api.dto.CreditCardCollectionCreateRequest;
import com.saraasansor.api.dto.PaytrCollectionCreateRequest;
import com.saraasansor.api.dto.PromissoryNoteCollectionCreateRequest;
import com.saraasansor.api.dto.QuickCollectionCreateRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class QuickCollectionService {

    private final B2BUnitTransactionService transactionService;

    public QuickCollectionService(B2BUnitTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public B2BUnitCollectionTransactionResponse createCollection(QuickCollectionCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("request is required");
        }
        if (request.getB2bUnitId() == null) {
            throw new RuntimeException("b2bUnitId is required");
        }
        if (request.getCollectionType() == null) {
            throw new RuntimeException("collectionType is required");
        }

        return switch (request.getCollectionType()) {
            case CASH -> transactionService.createCashCollection(request.getB2bUnitId(), toCashRequest(request));
            case PAYTR -> transactionService.createPaytrCollection(request.getB2bUnitId(), toPaytrRequest(request));
            case CREDIT_CARD -> transactionService.createCreditCardCollection(request.getB2bUnitId(), toCreditCardRequest(request));
            case BANK -> transactionService.createBankCollection(request.getB2bUnitId(), toBankRequest(request));
            case CHEQUE -> transactionService.createCheckCollection(request.getB2bUnitId(), toCheckRequest(request));
            case PROMISSORY_NOTE -> transactionService.createPromissoryNoteCollection(request.getB2bUnitId(), toPromissoryNoteRequest(request));
        };
    }

    private CashCollectionCreateRequest toCashRequest(QuickCollectionCreateRequest request) {
        CashCollectionCreateRequest target = new CashCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(request.getDescription());
        target.setCashAccountId(resolveCashboxId(request));
        return target;
    }

    private PaytrCollectionCreateRequest toPaytrRequest(QuickCollectionCreateRequest request) {
        PaytrCollectionCreateRequest target = new PaytrCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(resolvePaytrDescription(request));
        return target;
    }

    private CreditCardCollectionCreateRequest toCreditCardRequest(QuickCollectionCreateRequest request) {
        CreditCardCollectionCreateRequest target = new CreditCardCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(request.getDescription());
        target.setBankAccountId(request.getCardBankId() != null ? request.getCardBankId() : request.getBankAccountId());
        return target;
    }

    private BankCollectionCreateRequest toBankRequest(QuickCollectionCreateRequest request) {
        BankCollectionCreateRequest target = new BankCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(request.getDescription());
        target.setBankAccountId(request.getBankAccountId());
        return target;
    }

    private CheckCollectionCreateRequest toCheckRequest(QuickCollectionCreateRequest request) {
        CheckCollectionCreateRequest target = new CheckCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(request.getDescription());
        target.setDueDate(request.getDueDate());
        target.setSerialNumber(request.getChequeSerialNo());
        return target;
    }

    private PromissoryNoteCollectionCreateRequest toPromissoryNoteRequest(QuickCollectionCreateRequest request) {
        PromissoryNoteCollectionCreateRequest target = new PromissoryNoteCollectionCreateRequest();
        target.setTransactionDate(request.getCollectionDate());
        target.setFacilityId(request.getFacilityId());
        target.setAmount(request.getAmount());
        target.setDescription(request.getDescription());
        target.setDueDate(request.getDueDate());
        target.setSerialNumber(request.getPromissorySerialNo());
        return target;
    }

    private String resolvePaytrDescription(QuickCollectionCreateRequest request) {
        String description = request.getDescription();
        if (!StringUtils.hasText(request.getPaytrReference())) {
            return description;
        }
        String paytrReferenceSection = "PayTR Ref: " + request.getPaytrReference().trim();
        if (!StringUtils.hasText(description)) {
            return paytrReferenceSection;
        }
        return description + " | " + paytrReferenceSection;
    }

    private Long resolveCashboxId(QuickCollectionCreateRequest request) {
        return request.getCashboxId() != null ? request.getCashboxId() : request.getCashAccountId();
    }
}
