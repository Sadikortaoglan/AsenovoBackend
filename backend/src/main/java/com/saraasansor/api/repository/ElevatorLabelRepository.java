package com.saraasansor.api.repository;

import com.saraasansor.api.model.ElevatorLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElevatorLabelRepository extends JpaRepository<ElevatorLabel, Long> {
    Page<ElevatorLabel> findByElevatorId(Long elevatorId, Pageable pageable);
}
