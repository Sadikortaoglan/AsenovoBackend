package com.saraasansor.api.repository;

import com.saraasansor.api.model.ElevatorContract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElevatorContractRepository extends JpaRepository<ElevatorContract, Long> {
    Page<ElevatorContract> findByElevatorId(Long elevatorId, Pageable pageable);
}
