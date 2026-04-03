package com.saraasansor.api.repository;

import com.saraasansor.api.model.CashAccount;
import org.springframework.data.domain.Page;
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
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT c FROM CashAccount c WHERE c.active = true AND " +
            "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY c.name ASC")
    List<CashAccount> findLookup(@Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM CashAccount c WHERE " +
            "(:active IS NULL OR c.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY c.createdAt DESC, c.id DESC")
    Page<CashAccount> search(@Param("query") String query,
                             @Param("active") Boolean active,
                             Pageable pageable);
}
