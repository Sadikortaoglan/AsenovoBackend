package com.saraasansor.api.repository;

import com.saraasansor.api.model.RevisionOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RevisionOfferRepository extends JpaRepository<RevisionOffer, Long> {
    List<RevisionOffer> findByElevatorId(Long elevatorId);
    List<RevisionOffer> findByBuildingId(Long buildingId);
    List<RevisionOffer> findByCurrentAccountId(Long currentAccountId);
    List<RevisionOffer> findByStatus(RevisionOffer.Status status);
}
