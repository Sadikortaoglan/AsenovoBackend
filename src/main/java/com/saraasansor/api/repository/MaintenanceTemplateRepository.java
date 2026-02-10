package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceTemplateRepository extends JpaRepository<MaintenanceTemplate, Long> {
    List<MaintenanceTemplate> findByStatusOrderByNameAsc(MaintenanceTemplate.TemplateStatus status);
}
