package com.saraasansor.api.service;

import com.saraasansor.api.dto.ElevatorLabelCreateRequest;
import com.saraasansor.api.dto.ElevatorLabelListItemResponse;
import com.saraasansor.api.dto.ElevatorLabelResponse;
import com.saraasansor.api.dto.ElevatorLabelUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ElevatorLabelService {

    Page<ElevatorLabelListItemResponse> list(Pageable pageable, String search);

    ElevatorLabelResponse getById(Long id);

    ElevatorLabelResponse create(ElevatorLabelCreateRequest request);

    ElevatorLabelResponse update(Long id, ElevatorLabelUpdateRequest request);
}
