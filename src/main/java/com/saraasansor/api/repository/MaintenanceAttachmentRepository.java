package com.saraasansor.api.repository;

import com.saraasansor.api.model.MaintenanceAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceAttachmentRepository extends JpaRepository<MaintenanceAttachment, Long> {
    List<MaintenanceAttachment> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<MaintenanceAttachment> findBySessionIdAndItemIdOrderByCreatedAtAsc(Long sessionId, Long itemId);
}
