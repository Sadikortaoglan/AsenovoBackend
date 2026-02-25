package com.saraasansor.api.dto;

import com.saraasansor.api.model.CashAccount;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class CashAccountDto {
    private Long id;

    @NotBlank
    private String name;

    private String currency = "TRY";
    private BigDecimal totalIn;
    private BigDecimal totalOut;
    private BigDecimal balance;

    public static CashAccountDto fromEntity(CashAccount entity) {
        CashAccountDto dto = new CashAccountDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCurrency(entity.getCurrency());
        dto.setTotalIn(entity.getTotalIn());
        dto.setTotalOut(entity.getTotalOut());
        dto.setBalance(entity.getBalance());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getTotalIn() { return totalIn; }
    public void setTotalIn(BigDecimal totalIn) { this.totalIn = totalIn; }
    public BigDecimal getTotalOut() { return totalOut; }
    public void setTotalOut(BigDecimal totalOut) { this.totalOut = totalOut; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
}
