package com.saraasansor.api.dto;

import com.saraasansor.api.model.CurrentAccount;

import java.math.BigDecimal;

public class CurrentAccountDto {
    private Long id;
    private Long buildingId;
    private String buildingName;
    private String name;
    private String authorizedPerson;
    private String phone;
    private BigDecimal debt;
    private BigDecimal credit;
    private BigDecimal balance;

    public CurrentAccountDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthorizedPerson() {
        return authorizedPerson;
    }

    public void setAuthorizedPerson(String authorizedPerson) {
        this.authorizedPerson = authorizedPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public BigDecimal getDebt() {
        return debt;
    }

    public void setDebt(BigDecimal debt) {
        this.debt = debt;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public static CurrentAccountDto fromEntity(CurrentAccount account) {
        CurrentAccountDto dto = new CurrentAccountDto();
        dto.setId(account.getId());
        if (account.getBuilding() != null) {
            dto.setBuildingId(account.getBuilding().getId());
            dto.setBuildingName(account.getBuilding().getName());
        }
        dto.setName(account.getName());
        dto.setAuthorizedPerson(account.getAuthorizedPerson());
        dto.setPhone(account.getPhone());
        dto.setDebt(account.getDebt());
        dto.setCredit(account.getCredit());
        dto.setBalance(account.getBalance());
        return dto;
    }
}
