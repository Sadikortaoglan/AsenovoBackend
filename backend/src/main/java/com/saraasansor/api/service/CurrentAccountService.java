package com.saraasansor.api.service;

import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.repository.BuildingRepository;
import com.saraasansor.api.repository.CurrentAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CurrentAccountService {
    
    @Autowired
    private CurrentAccountRepository currentAccountRepository;
    
    @Autowired
    private BuildingRepository buildingRepository;
    
    public List<CurrentAccount> getAllCurrentAccounts() {
        return currentAccountRepository.findAll();
    }
    
    public CurrentAccount getCurrentAccountById(Long id) {
        return currentAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Current account not found"));
    }
    
    public Optional<CurrentAccount> getByBuildingId(Long buildingId) {
        return currentAccountRepository.findByBuildingId(buildingId);
    }
    
    /**
     * Auto-create CurrentAccount for a building
     * Business rule: Each building must automatically have a CurrentAccount
     */
    public CurrentAccount createForBuilding(Building building) {
        // Check if account already exists
        Optional<CurrentAccount> existing = currentAccountRepository.findByBuilding(building);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new account
        CurrentAccount account = new CurrentAccount();
        account.setBuilding(building);
        account.setName(building.getName()); // Use building name as account name
        account.setAuthorizedPerson(""); // Will be updated later
        account.setPhone(""); // Will be updated later
        account.setDebt(BigDecimal.ZERO);
        account.setCredit(BigDecimal.ZERO);
        account.setBalance(BigDecimal.ZERO);
        
        return currentAccountRepository.save(account);
    }
    
    public CurrentAccount updateCurrentAccount(Long id, CurrentAccount account) {
        CurrentAccount existing = currentAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Current account not found"));
        
        existing.setName(account.getName());
        existing.setAuthorizedPerson(account.getAuthorizedPerson());
        existing.setPhone(account.getPhone());
        existing.setDebt(account.getDebt());
        existing.setCredit(account.getCredit());
        // Balance is auto-calculated in @PreUpdate
        
        return currentAccountRepository.save(existing);
    }
    
    /**
     * Update debt/credit and recalculate balance
     */
    public CurrentAccount updateBalance(Long id, BigDecimal deltaDebt, BigDecimal deltaCredit) {
        CurrentAccount account = currentAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Current account not found"));
        
        account.setDebt(account.getDebt().add(deltaDebt != null ? deltaDebt : BigDecimal.ZERO));
        account.setCredit(account.getCredit().add(deltaCredit != null ? deltaCredit : BigDecimal.ZERO));
        // Balance is auto-calculated in @PreUpdate
        
        return currentAccountRepository.save(account);
    }
    
    public void deleteCurrentAccount(Long id) {
        if (!currentAccountRepository.existsById(id)) {
            throw new RuntimeException("Current account not found");
        }
        currentAccountRepository.deleteById(id);
    }
}
