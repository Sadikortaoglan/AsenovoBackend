package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceSectionRepository extends JpaRepository<MaintenanceSection, Long> {
    List<MaintenanceSection> findByTemplateIdOrderBySortOrderAsc(Long templateId);
}
