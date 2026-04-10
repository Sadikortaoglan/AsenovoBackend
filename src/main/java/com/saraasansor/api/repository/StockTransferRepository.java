package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {

    Optional<StockTransfer> findByIdAndActiveTrue(Long id);

    @Query("SELECT t FROM StockTransfer t WHERE " +
            "(:active IS NULL OR t.active = :active) AND " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(COALESCE(t.stock.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.outgoingWarehouse.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.incomingWarehouse.name, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY t.date DESC, t.id DESC")
    Page<StockTransfer> search(@Param("query") String query,
                               @Param("active") Boolean active,
                               Pageable pageable);
}
