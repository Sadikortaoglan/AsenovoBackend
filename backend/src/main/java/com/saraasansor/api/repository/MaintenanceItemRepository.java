package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceItemRepository extends JpaRepository<MaintenanceItem, Long> {
    List<MaintenanceItem> findBySectionIdOrderBySortOrderAsc(Long sectionId);
    List<MaintenanceItem> findBySectionIdAndIsActiveTrueOrderBySortOrderAsc(Long sectionId);
}
