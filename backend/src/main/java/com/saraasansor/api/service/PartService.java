package com.saraasansor.api.service;

import com.saraasansor.api.model.Part;
import com.saraasansor.api.repository.PartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PartService {
    
    @Autowired
    private PartRepository partRepository;
    
    public List<Part> getAllParts() {
        return partRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }
    
    public Part getPartById(Long id) {
        return partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found"));
    }
    
    public Part createPart(Part part) {
        if (part.getStock() == null) {
            part.setStock(0);
        }
        return partRepository.save(part);
    }
    
    public Part updatePart(Long id, Part part) {
        Part existing = partRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Part not found"));
        
        existing.setName(part.getName());
        existing.setDescription(part.getDescription());
        existing.setUnitPrice(part.getUnitPrice());
        existing.setStock(part.getStock());
        
        return partRepository.save(existing);
    }
    
    public void deletePart(Long id) {
        if (!partRepository.existsById(id)) {
            throw new RuntimeException("Part not found");
        }
        partRepository.deleteById(id);
    }
}
