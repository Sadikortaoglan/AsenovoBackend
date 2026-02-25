package com.saraasansor.api.service;

import com.saraasansor.api.model.Building;
import com.saraasansor.api.model.CurrentAccount;
import com.saraasansor.api.repository.BuildingRepository;
import com.saraasansor.api.repository.CurrentAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildingService {
    
    @Autowired
    private BuildingRepository buildingRepository;
    
    @Autowired
    private CurrentAccountRepository currentAccountRepository;
    
    @Autowired
    private CurrentAccountService currentAccountService;
    
    public List<Building> getAllBuildings() {
        return buildingRepository.findAll();
    }
    
    public Building getBuildingById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found"));
    }
    
    public Building createBuilding(Building building) {
        Building saved = buildingRepository.save(building);
        
        // AUTO-CREATE CurrentAccount for building
        // Business rule: Each building must automatically have a CurrentAccount
        currentAccountService.createForBuilding(saved);
        
        return saved;
    }
    
    public Building updateBuilding(Long id, Building building) {
        Building existing = buildingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Building not found"));
        
        existing.setName(building.getName());
        existing.setAddress(building.getAddress());
        existing.setCity(building.getCity());
        existing.setDistrict(building.getDistrict());
        
        Building saved = buildingRepository.save(existing);
        
        // Ensure CurrentAccount exists
        if (!currentAccountRepository.findByBuilding(saved).isPresent()) {
            currentAccountService.createForBuilding(saved);
        }
        
        return saved;
    }
    
    public void deleteBuilding(Long id) {
        if (!buildingRepository.existsById(id)) {
            throw new RuntimeException("Building not found");
        }
        buildingRepository.deleteById(id);
    }
}
