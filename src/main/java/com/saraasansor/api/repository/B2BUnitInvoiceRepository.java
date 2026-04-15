package com.saraasansor.api.repository;

import com.saraasansor.api.model.B2BUnitInvoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface B2BUnitInvoiceRepository extends JpaRepository<B2BUnitInvoice, Long> {

    @EntityGraph(attributePaths = {"b2bUnit", "facility", "elevator", "warehouse", "lines", "lines.stock"})
    Optional<B2BUnitInvoice> findByIdAndB2bUnitId(Long id, Long b2bUnitId);
}
