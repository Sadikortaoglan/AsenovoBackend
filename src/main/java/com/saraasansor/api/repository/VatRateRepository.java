package com.saraasansor.api.repository;

import com.saraasansor.api.model.VatRate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface VatRateRepository extends JpaRepository<VatRate, Long> {

    boolean existsByRate(BigDecimal rate);

    boolean existsByRateAndIdNot(BigDecimal rate, Long id);

    @Query("SELECT v FROM VatRate v WHERE " +
            "(:active IS NULL OR v.active = :active) AND (" +
            ":query IS NULL OR :query = '' OR STR(v.rate) LIKE CONCAT('%', :query, '%')" +
            ") ORDER BY v.rate ASC, v.id ASC")
    Page<VatRate> search(@Param("query") String query,
                         @Param("active") Boolean active,
                         Pageable pageable);

    @Query("SELECT v FROM VatRate v WHERE v.active = true AND (" +
            ":query IS NULL OR :query = '' OR STR(v.rate) LIKE CONCAT('%', :query, '%')" +
            ") ORDER BY v.rate ASC, v.id ASC")
    List<VatRate> lookup(@Param("query") String query, Pageable pageable);

    VatRate getVatRateByRate(BigDecimal normalizedRate);
}
