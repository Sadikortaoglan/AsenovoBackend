package com.saraasansor.api.service;

import com.saraasansor.api.model.MaintenanceSection;
import com.saraasansor.api.model.MaintenanceTemplate;
import com.saraasansor.api.model.MaintenanceItem;
import com.saraasansor.api.repository.MaintenanceTemplateRepository;
import com.saraasansor.api.repository.MaintenanceSectionRepository;
import com.saraasansor.api.repository.MaintenanceItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class MaintenanceTemplateService {
    
    @Autowired
    private MaintenanceTemplateRepository templateRepository;
    
    @Autowired
    private MaintenanceSectionRepository sectionRepository;
    
    @Autowired
    private MaintenanceItemRepository itemRepository;
    
    public List<MaintenanceTemplate> getAllTemplates() {
        return templateRepository.findAll();
    }
    
    public List<MaintenanceTemplate> getActiveTemplates() {
        return templateRepository.findByStatusOrderByNameAsc(MaintenanceTemplate.TemplateStatus.ACTIVE);
    }
    
    public MaintenanceTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
    }
    
    public MaintenanceTemplate createTemplate(MaintenanceTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        if (template.getStatus() == null) {
            template.setStatus(MaintenanceTemplate.TemplateStatus.ACTIVE);
        }
        return templateRepository.save(template);
    }
    
    public MaintenanceTemplate updateTemplate(Long id, MaintenanceTemplate templateData) {
        MaintenanceTemplate template = getTemplateById(id);
        template.setName(templateData.getName());
        template.setStatus(templateData.getStatus());
        template.setFrequencyDays(templateData.getFrequencyDays());
        template.setUpdatedAt(LocalDateTime.now());
        return templateRepository.save(template);
    }
    
    public MaintenanceTemplate duplicateTemplate(Long id) {
        MaintenanceTemplate original = getTemplateById(id);
        
        // Create new template
        MaintenanceTemplate duplicate = new MaintenanceTemplate();
        duplicate.setName(original.getName() + " (Copy)");
        duplicate.setStatus(MaintenanceTemplate.TemplateStatus.ACTIVE);
        duplicate.setFrequencyDays(original.getFrequencyDays());
        duplicate.setCreatedAt(LocalDateTime.now());
        duplicate.setUpdatedAt(LocalDateTime.now());
        duplicate = templateRepository.save(duplicate);
        
        // Copy sections and items
        for (MaintenanceSection originalSection : original.getSections()) {
            MaintenanceSection duplicateSection = new MaintenanceSection();
            duplicateSection.setTemplate(duplicate);
            duplicateSection.setName(originalSection.getName());
            duplicateSection.setSortOrder(originalSection.getSortOrder());
            duplicateSection.setCreatedAt(LocalDateTime.now());
            duplicateSection = sectionRepository.save(duplicateSection);
            
            // Copy items
            for (MaintenanceItem originalItem : originalSection.getItems()) {
                MaintenanceItem duplicateItem = new MaintenanceItem();
                duplicateItem.setSection(duplicateSection);
                duplicateItem.setTitle(originalItem.getTitle());
                duplicateItem.setDescription(originalItem.getDescription());
                duplicateItem.setMandatory(originalItem.getMandatory());
                duplicateItem.setAllowPhoto(originalItem.getAllowPhoto());
                duplicateItem.setAllowNote(originalItem.getAllowNote());
                duplicateItem.setSortOrder(originalItem.getSortOrder());
                duplicateItem.setIsActive(originalItem.getIsActive());
                duplicateItem.setCreatedAt(LocalDateTime.now());
                itemRepository.save(duplicateItem);
            }
        }
        
        return duplicate;
    }
    
    public void deleteTemplate(Long id) {
        MaintenanceTemplate template = getTemplateById(id);
        // Cascade delete will handle sections and items
        templateRepository.delete(template);
    }
    
    public List<MaintenanceSection> getSectionsByTemplateId(Long templateId) {
        return sectionRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
    }
    
    public MaintenanceSection createSection(Long templateId, MaintenanceSection section) {
        MaintenanceTemplate template = getTemplateById(templateId);
        section.setTemplate(template);
        section.setCreatedAt(LocalDateTime.now());
        if (section.getSortOrder() == null) {
            // Auto-assign sort order
            List<MaintenanceSection> existingSections = sectionRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
            int maxSortOrder = existingSections.stream()
                    .mapToInt(MaintenanceSection::getSortOrder)
                    .max()
                    .orElse(-1);
            section.setSortOrder(maxSortOrder + 1);
        }
        return sectionRepository.save(section);
    }
    
    public MaintenanceSection updateSection(Long id, MaintenanceSection sectionData) {
        MaintenanceSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found"));
        section.setName(sectionData.getName());
        section.setSortOrder(sectionData.getSortOrder());
        return sectionRepository.save(section);
    }
    
    public void deleteSection(Long id) {
        MaintenanceSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found"));
        sectionRepository.delete(section);
    }
    
    public List<MaintenanceItem> getItemsBySectionId(Long sectionId) {
        return itemRepository.findBySectionIdOrderBySortOrderAsc(sectionId);
    }
    
    public MaintenanceItem createItem(Long sectionId, MaintenanceItem item) {
        MaintenanceSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found"));
        item.setSection(section);
        item.setCreatedAt(LocalDateTime.now());
        if (item.getSortOrder() == null) {
            // Auto-assign sort order
            List<MaintenanceItem> existingItems = itemRepository.findBySectionIdOrderBySortOrderAsc(sectionId);
            int maxSortOrder = existingItems.stream()
                    .mapToInt(MaintenanceItem::getSortOrder)
                    .max()
                    .orElse(-1);
            item.setSortOrder(maxSortOrder + 1);
        }
        if (item.getIsActive() == null) {
            item.setIsActive(true);
        }
        return itemRepository.save(item);
    }
    
    public MaintenanceItem updateItem(Long id, MaintenanceItem itemData) {
        MaintenanceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found"));
        item.setTitle(itemData.getTitle());
        item.setDescription(itemData.getDescription());
        item.setMandatory(itemData.getMandatory());
        item.setAllowPhoto(itemData.getAllowPhoto());
        item.setAllowNote(itemData.getAllowNote());
        item.setSortOrder(itemData.getSortOrder());
        item.setIsActive(itemData.getIsActive());
        return itemRepository.save(item);
    }
    
    public MaintenanceItem toggleItemActive(Long id) {
        MaintenanceItem item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found"));
        item.setIsActive(!item.getIsActive());
        return itemRepository.save(item);
    }
}
