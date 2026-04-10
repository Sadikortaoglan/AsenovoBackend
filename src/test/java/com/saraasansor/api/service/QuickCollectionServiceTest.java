package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitCollectionTransactionResponse;
import com.saraasansor.api.dto.CashCollectionCreateRequest;
import com.saraasansor.api.dto.CheckCollectionCreateRequest;
import com.saraasansor.api.dto.CreditCardCollectionCreateRequest;
import com.saraasansor.api.dto.QuickCollectionCreateRequest;
import com.saraasansor.api.model.B2BUnitTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickCollectionServiceTest {

    @Mock
    private B2BUnitTransactionService transactionService;

    @Test
    void shouldDelegateCashCollectionToExistingTransactionService() {
        QuickCollectionService quickCollectionService = new QuickCollectionService(transactionService);
        QuickCollectionCreateRequest request = baseRequest(QuickCollectionCreateRequest.CollectionType.CASH);
        request.setCashboxId(14L);

        B2BUnitCollectionTransactionResponse expected = new B2BUnitCollectionTransactionResponse();
        expected.setTransactionType(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        when(transactionService.createCashCollection(eq(6L), any(CashCollectionCreateRequest.class))).thenReturn(expected);

        B2BUnitCollectionTransactionResponse response = quickCollectionService.createCollection(request);

        ArgumentCaptor<CashCollectionCreateRequest> captor = ArgumentCaptor.forClass(CashCollectionCreateRequest.class);
        verify(transactionService).createCashCollection(eq(6L), captor.capture());
        assertThat(captor.getValue().getCashAccountId()).isEqualTo(14L);
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(response.getTransactionType()).isEqualTo(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
    }

    @Test
    void shouldUseCardBankIdForCreditCardCollectionWhenProvided() {
        QuickCollectionService quickCollectionService = new QuickCollectionService(transactionService);
        QuickCollectionCreateRequest request = baseRequest(QuickCollectionCreateRequest.CollectionType.CREDIT_CARD);
        request.setBankAccountId(10L);
        request.setCardBankId(22L);

        B2BUnitCollectionTransactionResponse expected = new B2BUnitCollectionTransactionResponse();
        expected.setTransactionType(B2BUnitTransaction.TransactionType.CREDIT_CARD_COLLECTION);
        when(transactionService.createCreditCardCollection(eq(6L), any(CreditCardCollectionCreateRequest.class))).thenReturn(expected);

        quickCollectionService.createCollection(request);

        ArgumentCaptor<CreditCardCollectionCreateRequest> captor = ArgumentCaptor.forClass(CreditCardCollectionCreateRequest.class);
        verify(transactionService).createCreditCardCollection(eq(6L), captor.capture());
        assertThat(captor.getValue().getBankAccountId()).isEqualTo(22L);
    }

    @Test
    void shouldUseBankIdAliasForBankCollection() {
        QuickCollectionService quickCollectionService = new QuickCollectionService(transactionService);
        QuickCollectionCreateRequest request = baseRequest(QuickCollectionCreateRequest.CollectionType.BANK);
        request.setBankId(33L);

        B2BUnitCollectionTransactionResponse expected = new B2BUnitCollectionTransactionResponse();
        expected.setTransactionType(B2BUnitTransaction.TransactionType.BANK_COLLECTION);
        when(transactionService.createBankCollection(eq(6L), any())).thenReturn(expected);

        quickCollectionService.createCollection(request);

        ArgumentCaptor<com.saraasansor.api.dto.BankCollectionCreateRequest> captor =
                ArgumentCaptor.forClass(com.saraasansor.api.dto.BankCollectionCreateRequest.class);
        verify(transactionService).createBankCollection(eq(6L), captor.capture());
        assertThat(captor.getValue().getBankAccountId()).isEqualTo(33L);
    }

    @Test
    void shouldMapChequeFieldsForCheckCollection() {
        QuickCollectionService quickCollectionService = new QuickCollectionService(transactionService);
        QuickCollectionCreateRequest request = baseRequest(QuickCollectionCreateRequest.CollectionType.CHEQUE);
        request.setDueDate(LocalDate.of(2026, 5, 5));
        request.setChequeSerialNo("CHK-900");

        B2BUnitCollectionTransactionResponse expected = new B2BUnitCollectionTransactionResponse();
        expected.setTransactionType(B2BUnitTransaction.TransactionType.CHECK_COLLECTION);
        when(transactionService.createCheckCollection(eq(6L), any(CheckCollectionCreateRequest.class))).thenReturn(expected);

        quickCollectionService.createCollection(request);

        ArgumentCaptor<CheckCollectionCreateRequest> captor = ArgumentCaptor.forClass(CheckCollectionCreateRequest.class);
        verify(transactionService).createCheckCollection(eq(6L), captor.capture());
        assertThat(captor.getValue().getDueDate()).isEqualTo(LocalDate.of(2026, 5, 5));
        assertThat(captor.getValue().getSerialNumber()).isEqualTo("CHK-900");
    }

    @Test
    void shouldUseLegacyCashAccountIdWhenCashboxIdIsNotProvided() {
        QuickCollectionService quickCollectionService = new QuickCollectionService(transactionService);
        QuickCollectionCreateRequest request = baseRequest(QuickCollectionCreateRequest.CollectionType.CASH);
        request.setCashAccountId(44L);

        B2BUnitCollectionTransactionResponse expected = new B2BUnitCollectionTransactionResponse();
        expected.setTransactionType(B2BUnitTransaction.TransactionType.CASH_COLLECTION);
        when(transactionService.createCashCollection(eq(6L), any(CashCollectionCreateRequest.class))).thenReturn(expected);

        quickCollectionService.createCollection(request);

        ArgumentCaptor<CashCollectionCreateRequest> captor = ArgumentCaptor.forClass(CashCollectionCreateRequest.class);
        verify(transactionService).createCashCollection(eq(6L), captor.capture());
        assertThat(captor.getValue().getCashAccountId()).isEqualTo(44L);
    }

    private QuickCollectionCreateRequest baseRequest(QuickCollectionCreateRequest.CollectionType type) {
        QuickCollectionCreateRequest request = new QuickCollectionCreateRequest();
        request.setCollectionDate(LocalDate.of(2026, 4, 1));
        request.setB2bUnitId(6L);
        request.setFacilityId(11L);
        request.setCollectionType(type);
        request.setAmount(new BigDecimal("150.00"));
        request.setDescription("quick collection");
        return request;
    }
}
