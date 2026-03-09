package com.saraasansor.api.repository;

import com.saraasansor.api.model.BankAccount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByIdAndActiveTrue(Long id);

    @Query("SELECT b FROM BankAccount b WHERE b.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY b.name ASC")
    List<BankAccount> findLookup(@Param("query") String query, Pageable pageable);
}
