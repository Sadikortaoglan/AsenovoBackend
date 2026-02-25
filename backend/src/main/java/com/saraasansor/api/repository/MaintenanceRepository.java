package com.saraasansor.api.repository;

import com.saraasansor.api.model.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    List<Maintenance> findByElevatorIdOrderByDateDesc(Long elevatorId);
    
    List<Maintenance> findByIsPaidOrderByDateDesc(Boolean isPaid);
    
    @Query("SELECT m FROM Maintenance m WHERE m.date >= :dateFrom AND m.date <= :dateTo ORDER BY m.date DESC")
    List<Maintenance> findByDateBetween(@Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
    
    @Query("SELECT m FROM Maintenance m WHERE m.isPaid = :paid AND m.date >= :dateFrom AND m.date <= :dateTo ORDER BY m.date DESC")
    List<Maintenance> findByIsPaidAndDateBetween(@Param("paid") Boolean paid, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);
    
    @Query("SELECT m FROM Maintenance m WHERE YEAR(m.date) = :year AND MONTH(m.date) = :month ORDER BY m.date DESC")
    List<Maintenance> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
}

