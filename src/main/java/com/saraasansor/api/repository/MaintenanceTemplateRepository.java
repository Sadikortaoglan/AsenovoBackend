package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTemplateRepository extends JpaRepository<MaintenanceTemplate, Long> {
    List<MaintenanceTemplate> findByStatusOrderByNameAsc(MaintenanceTemplate.TemplateStatus status);
    
    @Query("SELECT DISTINCT t FROM MaintenanceTemplate t " +
           "LEFT JOIN FETCH t.sections s " +
           "LEFT JOIN FETCH s.items i " +
           "WHERE t.id = :id " +
           "ORDER BY s.sortOrder ASC, i.sortOrder ASC")
    Optional<MaintenanceTemplate> findByIdWithSectionsAndItems(@Param("id") Long id);
}
