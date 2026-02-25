package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {
    Page<StockItem> findByProductNameContainingIgnoreCase(String q, Pageable pageable);

    @Query("select distinct s.modelName from StockItem s where s.modelName is not null and s.modelName <> '' order by s.modelName asc")
    List<String> findDistinctModelNames();

    @Query("select distinct s.vatRate from StockItem s order by s.vatRate asc")
    List<BigDecimal> findDistinctVatRates();
}
