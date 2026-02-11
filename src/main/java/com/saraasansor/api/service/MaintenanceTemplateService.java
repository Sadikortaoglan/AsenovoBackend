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
        return templateRepository.findByIdWithSectionsAndItems(id)
                .orElseGet(() -> templateRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Maintenance template not found")));
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
    
    @Transactional
    public MaintenanceTemplate createSection(Long templateId, MaintenanceSection section) {
        System.out.println("========================================");
        System.out.println("CREATE SECTION: templateId=" + templateId);
        System.out.println("Section name: " + section.getName());
        System.out.println("========================================");
        
        // Validation: Template must exist
        MaintenanceTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Maintenance template not found: " + templateId));
        
        // Validation: Name cannot be empty
        if (section.getName() == null || section.getName().trim().isEmpty()) {
            throw new RuntimeException("Section name cannot be empty");
        }
        
        section.setTemplate(template);
        section.setCreatedAt(LocalDateTime.now());
        
        // Set default active if not provided
        if (section.getActive() == null) {
            section.setActive(true);
        }
        
        // Auto-assign sort order if not provided
        if (section.getSortOrder() == null) {
            List<MaintenanceSection> existingSections = sectionRepository.findByTemplateIdOrderBySortOrderAsc(templateId);
            int maxSortOrder = existingSections.stream()
                    .mapToInt(MaintenanceSection::getSortOrder)
                    .max()
                    .orElse(-1);
            section.setSortOrder(maxSortOrder + 1);
        }
        
        MaintenanceSection saved = sectionRepository.save(section);
        sectionRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("SECTION CREATED: id=" + saved.getId() + ", name=" + saved.getName());
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    @Transactional
    public MaintenanceTemplate updateSection(Long sectionId, MaintenanceSection sectionData) {
        System.out.println("========================================");
        System.out.println("UPDATE SECTION: sectionId=" + sectionId);
        System.out.println("New name: " + sectionData.getName());
        System.out.println("========================================");
        
        MaintenanceSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found: " + sectionId));
        
        // Validation: Name cannot be empty
        if (sectionData.getName() != null && sectionData.getName().trim().isEmpty()) {
            throw new RuntimeException("Section name cannot be empty");
        }
        
        Long templateId = section.getTemplate().getId();
        
        if (sectionData.getName() != null) {
            section.setName(sectionData.getName());
        }
        if (sectionData.getSortOrder() != null) {
            section.setSortOrder(sectionData.getSortOrder());
        }
        if (sectionData.getActive() != null) {
            section.setActive(sectionData.getActive());
        }
        
        sectionRepository.save(section);
        sectionRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("SECTION UPDATED: id=" + section.getId());
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    @Transactional
    public MaintenanceTemplate deleteSection(Long sectionId) {
        System.out.println("========================================");
        System.out.println("DELETE SECTION: sectionId=" + sectionId);
        System.out.println("========================================");
        
        MaintenanceSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found: " + sectionId));
        
        Long templateId = section.getTemplate().getId();
        
        sectionRepository.delete(section);
        sectionRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("SECTION DELETED: id=" + sectionId);
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    public List<MaintenanceItem> getItemsBySectionId(Long sectionId) {
        return itemRepository.findBySectionIdOrderBySortOrderAsc(sectionId);
    }
    
    @Transactional
    public MaintenanceTemplate createItem(Long sectionId, MaintenanceItem item) {
        System.out.println("========================================");
        System.out.println("CREATE ITEM: sectionId=" + sectionId);
        System.out.println("Item title: " + item.getTitle());
        System.out.println("========================================");
        
        // Validation: Section must exist
        MaintenanceSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Maintenance section not found: " + sectionId));
        
        // Validation: Title cannot be empty
        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Item title cannot be empty");
        }
        
        Long templateId = section.getTemplate().getId();
        
        item.setSection(section);
        item.setCreatedAt(LocalDateTime.now());
        
        // Set defaults
        if (item.getSortOrder() == null) {
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
        if (item.getMandatory() == null) {
            item.setMandatory(false);
        }
        if (item.getAllowPhoto() == null) {
            item.setAllowPhoto(false);
        }
        if (item.getAllowNote() == null) {
            item.setAllowNote(true);
        }
        
        MaintenanceItem saved = itemRepository.save(item);
        itemRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("ITEM CREATED: id=" + saved.getId() + ", title=" + saved.getTitle());
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    @Transactional
    public MaintenanceTemplate updateItem(Long itemId, MaintenanceItem itemData) {
        System.out.println("========================================");
        System.out.println("UPDATE ITEM: itemId=" + itemId);
        System.out.println("New title: " + itemData.getTitle());
        System.out.println("========================================");
        
        MaintenanceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found: " + itemId));
        
        // Validation: Title cannot be empty
        if (itemData.getTitle() != null && itemData.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Item title cannot be empty");
        }
        
        Long templateId = item.getSection().getTemplate().getId();
        
        if (itemData.getTitle() != null) {
            item.setTitle(itemData.getTitle());
        }
        if (itemData.getDescription() != null) {
            item.setDescription(itemData.getDescription());
        }
        if (itemData.getMandatory() != null) {
            item.setMandatory(itemData.getMandatory());
        }
        if (itemData.getAllowPhoto() != null) {
            item.setAllowPhoto(itemData.getAllowPhoto());
        }
        if (itemData.getAllowNote() != null) {
            item.setAllowNote(itemData.getAllowNote());
        }
        if (itemData.getSortOrder() != null) {
            item.setSortOrder(itemData.getSortOrder());
        }
        if (itemData.getIsActive() != null) {
            item.setIsActive(itemData.getIsActive());
        }
        
        itemRepository.save(item);
        itemRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("ITEM UPDATED: id=" + item.getId());
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    @Transactional
    public MaintenanceTemplate deleteItem(Long itemId) {
        System.out.println("========================================");
        System.out.println("DELETE ITEM: itemId=" + itemId);
        System.out.println("========================================");
        
        MaintenanceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found: " + itemId));
        
        Long templateId = item.getSection().getTemplate().getId();
        
        itemRepository.delete(item);
        itemRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("ITEM DELETED: id=" + itemId);
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
    
    @Transactional
    public MaintenanceTemplate toggleItemActive(Long itemId) {
        System.out.println("========================================");
        System.out.println("TOGGLE ITEM ACTIVE: itemId=" + itemId);
        System.out.println("========================================");
        
        MaintenanceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found: " + itemId));
        
        Long templateId = item.getSection().getTemplate().getId();
        
        item.setIsActive(!item.getIsActive());
        itemRepository.save(item);
        itemRepository.flush(); // Force immediate persistence
        
        System.out.println("========================================");
        System.out.println("ITEM ACTIVE TOGGLED: id=" + itemId + ", active=" + item.getIsActive());
        System.out.println("========================================");
        
        // Return full template with sections and items
        return getTemplateById(templateId);
    }
}
