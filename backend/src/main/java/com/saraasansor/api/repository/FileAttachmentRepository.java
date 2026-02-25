package com.saraasansor.api.repository;

import com.saraasansor.api.model.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {
    List<FileAttachment> findByEntityTypeAndEntityId(FileAttachment.EntityType entityType, Long entityId);
}

