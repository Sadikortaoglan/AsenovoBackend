package com.saraasansor.api.repository;

import com.saraasansor.api.model.RevisionOffer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RevisionOfferRepository extends JpaRepository<RevisionOffer, Long> {

    @EntityGraph(attributePaths = {
            "elevator",
            "elevator.facility",
            "elevator.facility.b2bUnit",
            "building",
            "currentAccount",
            "currentAccount.building",
            "convertedToSale",
            "items",
            "items.part"
    })
    List<RevisionOffer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {
            "elevator",
            "elevator.facility",
            "elevator.facility.b2bUnit",
            "building",
            "currentAccount",
            "currentAccount.building",
            "convertedToSale",
            "items",
            "items.part"
    })
    Optional<RevisionOffer> findDetailedById(Long id);

    @EntityGraph(attributePaths = {
            "elevator",
            "elevator.facility",
            "elevator.facility.b2bUnit",
            "building",
            "currentAccount",
            "currentAccount.building",
            "convertedToSale",
            "items",
            "items.part"
    })
    List<RevisionOffer> findAllByElevatorIdOrderByCreatedAtDesc(Long elevatorId);

    @EntityGraph(attributePaths = {
            "elevator",
            "elevator.facility",
            "elevator.facility.b2bUnit",
            "building",
            "currentAccount",
            "currentAccount.building",
            "convertedToSale",
            "items",
            "items.part"
    })
    List<RevisionOffer> findAllByBuildingIdOrderByCreatedAtDesc(Long buildingId);

    List<RevisionOffer> findByElevatorId(Long elevatorId);
    List<RevisionOffer> findByBuildingId(Long buildingId);
    List<RevisionOffer> findByCurrentAccountId(Long currentAccountId);
    List<RevisionOffer> findByStatus(RevisionOffer.Status status);
}
