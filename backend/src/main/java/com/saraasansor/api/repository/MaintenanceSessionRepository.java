package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceSessionRepository extends JpaRepository<MaintenanceSession, Long> {
    Optional<MaintenanceSession> findByQrProofId(Long qrProofId);
    
    List<MaintenanceSession> findByTechnicianIdAndStartAtBetweenOrderByStartAtDesc(
        Long technicianId, LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT s FROM MaintenanceSession s WHERE s.status = :status AND s.startAt >= :from AND s.startAt <= :to ORDER BY s.startAt DESC")
    Page<MaintenanceSession> findByStatusAndStartAtBetween(
        @Param("status") MaintenanceSession.SessionStatus status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable);
    
    @Query("SELECT s FROM MaintenanceSession s WHERE s.elevator.id = :elevatorId AND s.startAt >= :from AND s.startAt <= :to ORDER BY s.startAt DESC")
    List<MaintenanceSession> findByElevatorIdAndStartAtBetween(
        @Param("elevatorId") Long elevatorId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);
}
