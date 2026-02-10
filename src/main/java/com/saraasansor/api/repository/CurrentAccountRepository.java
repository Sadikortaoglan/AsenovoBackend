package com.saraasansor.api.repository;

import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CurrentAccountRepository extends JpaRepository<CurrentAccount, Long> {
    Optional<CurrentAccount> findByBuilding(Building building);
    Optional<CurrentAccount> findByBuildingId(Long buildingId);
}
