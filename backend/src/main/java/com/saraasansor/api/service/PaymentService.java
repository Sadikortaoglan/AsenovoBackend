package com.saraasansor.api.service;

import com.saraasansor.api.dto.BankAccountDto;
import com.saraasansor.api.dto.CashAccountDto;
import com.saraasansor.api.dto.PaymentTransactionDto;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.model.*;
import com.saraasansor.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional
public class PaymentService {
    @Autowired
    private PaymentTransactionRepository paymentRepository;
    @Autowired
    private CurrentAccountRepository currentAccountRepository;
    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private CashAccountRepository cashAccountRepository;
    @Autowired
    private BankAccountRepository bankAccountRepository;

    public Page<PaymentTransactionDto> list(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        LocalDateTime start = from == null ? LocalDateTime.now().minusMonths(1) : from;
        LocalDateTime end = to == null ? LocalDateTime.now().plusDays(1) : to;
        return paymentRepository.findByPaymentDateBetween(start, end, pageable).map(PaymentTransactionDto::fromEntity);
    }

    public PaymentTransactionDto create(PaymentTransactionDto dto) {
        PaymentTransaction tx = new PaymentTransaction();
        map(tx, dto);
        PaymentTransaction saved = paymentRepository.save(tx);
        applyFinancialImpact(saved, saved.getAmount(), true);
        return PaymentTransactionDto.fromEntity(saved);
    }

    public PaymentTransactionDto update(Long id, PaymentTransactionDto dto) {
        PaymentTransaction tx = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));

        applyFinancialImpact(tx, tx.getAmount(), false);
        map(tx, dto);
        PaymentTransaction saved = paymentRepository.save(tx);
        applyFinancialImpact(saved, saved.getAmount(), true);
        return PaymentTransactionDto.fromEntity(saved);
    }

    public void delete(Long id) {
        PaymentTransaction tx = paymentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        applyFinancialImpact(tx, tx.getAmount(), false);
        paymentRepository.delete(tx);
    }

    public java.util.List<CashAccountDto> listCashAccounts() {
        return cashAccountRepository.findAll().stream().map(CashAccountDto::fromEntity).toList();
    }

    public java.util.List<BankAccountDto> listBankAccounts() {
        return bankAccountRepository.findAll().stream().map(BankAccountDto::fromEntity).toList();
    }

    public CashAccountDto saveCashAccount(CashAccountDto dto) {
        CashAccount entity = dto.getId() == null ? new CashAccount() : cashAccountRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Cash account not found"));
        entity.setName(dto.getName());
        entity.setCurrency(dto.getCurrency() == null ? "TRY" : dto.getCurrency());
        return CashAccountDto.fromEntity(cashAccountRepository.save(entity));
    }

    public BankAccountDto saveBankAccount(BankAccountDto dto) {
        BankAccount entity = dto.getId() == null ? new BankAccount() : bankAccountRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Bank account not found"));
        entity.setName(dto.getName());
        entity.setCurrency(dto.getCurrency() == null ? "TRY" : dto.getCurrency());
        return BankAccountDto.fromEntity(bankAccountRepository.save(entity));
    }

    public void deleteCashAccount(Long id) {
        if (!cashAccountRepository.existsById(id)) {
            throw new NotFoundException("Cash account not found");
        }
        cashAccountRepository.deleteById(id);
    }

    public void deleteBankAccount(Long id) {
        if (!bankAccountRepository.existsById(id)) {
            throw new NotFoundException("Bank account not found");
        }
        bankAccountRepository.deleteById(id);
    }

    private void map(PaymentTransaction tx, PaymentTransactionDto dto) {
        tx.setPaymentType(PaymentTransaction.PaymentType.valueOf(dto.getPaymentType().toUpperCase()));
        tx.setAmount(dto.getAmount());
        tx.setDescription(dto.getDescription());
        tx.setPaymentDate(dto.getPaymentDate());

        tx.setCurrentAccount(dto.getCurrentAccountId() == null ? null : currentAccountRepository.findById(dto.getCurrentAccountId())
                .orElseThrow(() -> new NotFoundException("Current account not found")));

        tx.setBuilding(dto.getBuildingId() == null ? null : buildingRepository.findById(dto.getBuildingId())
                .orElseThrow(() -> new NotFoundException("Building not found")));

        tx.setCashAccount(dto.getCashAccountId() == null ? null : cashAccountRepository.findById(dto.getCashAccountId())
                .orElseThrow(() -> new NotFoundException("Cash account not found")));

        tx.setBankAccount(dto.getBankAccountId() == null ? null : bankAccountRepository.findById(dto.getBankAccountId())
                .orElseThrow(() -> new NotFoundException("Bank account not found")));
    }

    private void applyFinancialImpact(PaymentTransaction tx, BigDecimal amount, boolean apply) {
        BigDecimal signedAmount = apply ? amount : amount.negate();

        if (tx.getCurrentAccount() != null) {
            CurrentAccount account = tx.getCurrentAccount();
            account.setCredit(account.getCredit().add(signedAmount));
            account.setBalance(account.getCredit().subtract(account.getDebt()));
            currentAccountRepository.save(account);
        }

        if (tx.getPaymentType() == PaymentTransaction.PaymentType.CASH && tx.getCashAccount() != null) {
            CashAccount cash = tx.getCashAccount();
            cash.setTotalIn(cash.getTotalIn().add(signedAmount));
            cash.setBalance(cash.getTotalIn().subtract(cash.getTotalOut()));
            cashAccountRepository.save(cash);
        }

        if ((tx.getPaymentType() == PaymentTransaction.PaymentType.BANK || tx.getPaymentType() == PaymentTransaction.PaymentType.POS)
                && tx.getBankAccount() != null) {
            BankAccount bank = tx.getBankAccount();
            bank.setTotalIn(bank.getTotalIn().add(signedAmount));
            bank.setBalance(bank.getTotalIn().subtract(bank.getTotalOut()));
            bankAccountRepository.save(bank);
        }
    }
}
