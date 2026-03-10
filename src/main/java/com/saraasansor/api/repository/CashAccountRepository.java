package com.saraasansor.api.repository;

import com.saraasansor.api.model.CashAccount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashAccountRepository extends JpaRepository<CashAccount, Long> {

    Optional<CashAccount> findByIdAndActiveTrue(Long id);

    @Query("SELECT c FROM CashAccount c WHERE c.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY c.name ASC")
    List<CashAccount> findLookup(@Param("query") String query, Pageable pageable);
}
