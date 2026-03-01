package com.saraasansor.api.service;

import com.saraasansor.api.dto.CreateB2BUnitGroupRequest;
import com.saraasansor.api.dto.UpdateB2BUnitGroupRequest;
import com.saraasansor.api.model.B2BUnitGroup;
import com.saraasansor.api.repository.B2BUnitGroupRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class B2BUnitGroupService {

    private final B2BUnitGroupRepository b2bUnitGroupRepository;

    public B2BUnitGroupService(B2BUnitGroupRepository b2bUnitGroupRepository) {
        this.b2bUnitGroupRepository = b2bUnitGroupRepository;
    }

    @Transactional(readOnly = true)
    public List<B2BUnitGroup> getAllGroups() {
        return b2bUnitGroupRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public B2BUnitGroup getGroupById(Long id) {
        return b2bUnitGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("B2B unit group not found"));
    }

    public B2BUnitGroup createGroup(CreateB2BUnitGroupRequest request) {
        String normalizedName = normalizeRequired(request.getName());

        if (b2bUnitGroupRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new RuntimeException("B2B unit group name already exists");
        }

        B2BUnitGroup group = new B2BUnitGroup();
        group.setName(normalizedName);
        group.setDescription(normalizeNullable(request.getDescription()));
        return b2bUnitGroupRepository.save(group);
    }

    public B2BUnitGroup updateGroup(Long id, UpdateB2BUnitGroupRequest request) {
        B2BUnitGroup group = b2bUnitGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("B2B unit group not found"));

        String normalizedName = normalizeRequired(request.getName());
        if (!group.getName().equalsIgnoreCase(normalizedName)
                && b2bUnitGroupRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new RuntimeException("B2B unit group name already exists");
        }

        group.setName(normalizedName);
        group.setDescription(normalizeNullable(request.getDescription()));
        return b2bUnitGroupRepository.save(group);
    }

    public void deleteGroup(Long id) {
        if (!b2bUnitGroupRepository.existsById(id)) {
            throw new RuntimeException("B2B unit group not found");
        }
        b2bUnitGroupRepository.deleteById(id);
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
