package com.saraasansor.api.repository;

import com.saraasansor.api.model.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Page<StockTransfer> findAllByOrderByTransferDateDesc(Pageable pageable);
}
