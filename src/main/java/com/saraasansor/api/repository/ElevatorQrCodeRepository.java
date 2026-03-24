package com.saraasansor.api.repository;

import com.saraasansor.api.model.ElevatorQrCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ElevatorQrCodeRepository extends JpaRepository<ElevatorQrCode, Long> {

    interface ElevatorQrListProjection {
        Long getQrId();
        java.util.UUID getUuid();
        Long getElevatorId();
        String getElevatorName();
        String getBuildingName();
        Long getFacilityId();
        String getFacilityName();
        String getCustomerName();
        java.time.LocalDateTime getCreatedAt();
        String getQrValue();
    }

    boolean existsByQrValue(String qrValue);

    java.util.Optional<ElevatorQrCode> findByIdAndCompanyId(Long id, Long companyId);

    @Query(value = """
        SELECT q.id AS qrId,
               q.uuid AS uuid,
               e.id AS elevatorId,
               e.elevatorNumber AS elevatorName,
               e.buildingName AS buildingName,
               f.id AS facilityId,
               COALESCE(f.name, e.buildingName) AS facilityName,
               e.managerName AS customerName,
               q.createdAt AS createdAt,
               q.qrValue AS qrValue
        FROM Elevator e
        LEFT JOIN e.facility f
        LEFT JOIN ElevatorQrCode q ON q.elevator = e AND q.companyId = :companyId
        WHERE (
              q.id IS NULL OR q.id = (
                 SELECT MAX(q2.id)
                 FROM ElevatorQrCode q2
                 WHERE q2.elevator = e AND q2.companyId = :companyId
              )
          )
          AND (
              :search IS NULL OR :search = '' OR
              LOWER(e.buildingName) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(e.elevatorNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(e.identityNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          )
        """,
        countQuery = """
        SELECT COUNT(e.id)
        FROM Elevator e
        WHERE (
              :search IS NULL OR :search = '' OR
              LOWER(e.buildingName) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(e.elevatorNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
              LOWER(e.identityNumber) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<ElevatorQrListProjection> findAllBySearchAndCompanyId(@Param("search") String search,
                                                                @Param("companyId") Long companyId,
                                                                Pageable pageable);

    java.util.Optional<ElevatorQrCode> findTopByElevatorIdAndCompanyIdOrderByCreatedAtDesc(Long elevatorId, Long companyId);
}
