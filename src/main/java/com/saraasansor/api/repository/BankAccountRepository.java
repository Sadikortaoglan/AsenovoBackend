package com.saraasansor.api.repository;

import com.saraasansor.api.model.BankAccount;
import org.springframework.data.domain.Page;
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
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT b FROM BankAccount b WHERE b.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(b.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY b.name ASC")
    List<BankAccount> findLookup(@Param("query") String query, Pageable pageable);

    @Query("SELECT b FROM BankAccount b WHERE " +
            "(:active IS NULL OR b.active = :active) AND (" +
            ":query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(b.iban, '')) LIKE LOWER(CONCAT('%', :query, '%'))" +
            ") ORDER BY b.createdAt DESC, b.id DESC")
    Page<BankAccount> search(@Param("query") String query,
                             @Param("active") Boolean active,
                             Pageable pageable);
}
