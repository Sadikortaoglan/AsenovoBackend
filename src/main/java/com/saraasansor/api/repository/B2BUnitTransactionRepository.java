package com.saraasansor.api.repository;

import com.saraasansor.api.model.B2BUnitTransaction;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {"b2bUnit", "facility"})
    java.util.Optional<B2BUnitTransaction> findByIdAndB2bUnitId(Long id, Long b2bUnitId);

    @EntityGraph(attributePaths = {"b2bUnit", "facility"})
    @Query("SELECT t FROM B2BUnitTransaction t " +
            "LEFT JOIN t.b2bUnit b " +
            "LEFT JOIN t.facility f " +
            "WHERE t.transactionType IN :collectionTypes " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "AND (:b2bUnitId IS NULL OR b.id = :b2bUnitId) " +
            "AND (" +
            ":search IS NULL OR :search = '' OR " +
            "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(b.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(COALESCE(f.name, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "(:searchCollectionType IS NOT NULL AND t.transactionType = :searchCollectionType)" +
            ")")
    Page<B2BUnitTransaction> searchCollectionReceipts(
            @Param("collectionTypes") java.util.Set<B2BUnitTransaction.TransactionType> collectionTypes,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("search") String search,
            @Param("searchCollectionType") B2BUnitTransaction.TransactionType searchCollectionType,
            @Param("b2bUnitId") Long b2bUnitId,
            Pageable pageable);

    @EntityGraph(attributePaths = {"b2bUnit", "facility"})
    @Query("SELECT t FROM B2BUnitTransaction t WHERE t.id = :id")
    java.util.Optional<B2BUnitTransaction> findWithDetailsById(@Param("id") Long id);

    java.util.List<B2BUnitTransaction> findByB2bUnitIdAndTransactionDateBetweenOrderByTransactionDateAscIdAsc(Long b2bUnitId,
                                                                                                               java.time.LocalDate startDate,
                                                                                                               java.time.LocalDate endDate);

    java.util.Optional<B2BUnitTransaction> findTopByB2bUnitIdAndTransactionDateLessThanOrderByTransactionDateDescIdDesc(Long b2bUnitId,
                                                                                                                          java.time.LocalDate date);

    java.util.Optional<B2BUnitTransaction> findTopByB2bUnitIdAndTransactionDateLessThanEqualOrderByTransactionDateDescIdDesc(Long b2bUnitId,
                                                                                                                               java.time.LocalDate date);

    java.util.Optional<B2BUnitTransaction> findTopByB2bUnitIdOrderByTransactionDateDescIdDesc(Long b2bUnitId);
}
