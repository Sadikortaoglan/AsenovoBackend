package com.saraasansor.api.repository;

import com.saraasansor.api.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {
    @Query("SELECT DISTINCT o FROM Offer o LEFT JOIN FETCH o.elevator LEFT JOIN FETCH o.items")
    List<Offer> findAll();
    
    @Query("SELECT DISTINCT o FROM Offer o LEFT JOIN FETCH o.elevator LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Offer> findById(Long id);
    
    @Query("SELECT DISTINCT o FROM Offer o LEFT JOIN FETCH o.elevator LEFT JOIN FETCH o.items WHERE o.elevator.id = :elevatorId")
    List<Offer> findByElevatorId(Long elevatorId);
}

