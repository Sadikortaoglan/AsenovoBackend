package com.saraasansor.api.repository;

import com.saraasansor.api.model.OfferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferItemRepository extends JpaRepository<OfferItem, Long> {
    List<OfferItem> findByOfferId(Long offerId);
    void deleteByOfferId(Long offerId);
}

