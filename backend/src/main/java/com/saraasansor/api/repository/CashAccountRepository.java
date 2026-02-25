package com.saraasansor.api.repository;

import com.saraasansor.api.model.CashAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashAccountRepository extends JpaRepository<CashAccount, Long> {
}
