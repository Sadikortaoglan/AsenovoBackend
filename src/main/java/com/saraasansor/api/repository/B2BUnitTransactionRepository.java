package com.saraasansor.api.repository;

import com.saraasansor.api.model.B2BUnitTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface B2BUnitTransactionRepository extends JpaRepository<B2BUnitTransaction, Long> {

    @Query("SELECT t FROM B2BUnitTransaction t WHERE " +
            "t.b2bUnit.id = :b2bUnitId AND " +
            "t.transactionDate BETWEEN :startDate AND :endDate AND (" +
            ":search IS NULL OR :search = '' OR " +
            "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "(:transactionType IS NOT NULL AND t.transactionType = :transactionType)" +
            ")")
    Page<B2BUnitTransaction> searchLedger(@Param("b2bUnitId") Long b2bUnitId,
                                          @Param("startDate") java.time.LocalDate startDate,
                                          @Param("endDate") java.time.LocalDate endDate,
                                          @Param("search") String search,
                                          @Param("transactionType") B2BUnitTransaction.TransactionType transactionType,
                                          Pageable pageable);

    java.util.Optional<B2BUnitTransaction> findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(Long b2bUnitId);
}
