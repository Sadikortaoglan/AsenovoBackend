package com.saraasansor.api.service;

import com.saraasansor.api.model.*;
import com.saraasansor.api.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MaintenanceSessionService {
    
    @Autowired
    private MaintenanceSessionRepository sessionRepository;
    
    @Autowired
    private MaintenancePlanRepository planRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private MaintenanceTemplateRepository templateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private QrProofRepository qrProofRepository;
    
    @Autowired
    private MaintenanceStepResultRepository stepResultRepository;
    
    @Autowired
    private MaintenanceItemRepository itemRepository;
    
    @Value("${app.maintenance.min-duration-minutes:3}")
    private int minDurationMinutes = 3;
    
    /**
     * Start a maintenance session (with QR validation)
     */
    public MaintenanceSession startSession(Long elevatorId, Long templateId, String qrToken, Long planId) {
        // 1. Validate QR token if provided
        QrProof qrProof = null;
        if (qrToken != null && !qrToken.trim().isEmpty()) {
            // Get technician from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
                throw new RuntimeException("User not authenticated");
            }
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User technician = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Technician not found"));
            
            qrProof = qrProofRepository.findByTokenHash(hashToken(qrToken))
                    .orElseThrow(() -> new RuntimeException("Invalid QR token"));
            
            // Validate token is fresh and unused
            if (qrProof.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("QR token has expired");
            }
            if (qrProof.getUsedAt() != null) {
                throw new RuntimeException("QR token has already been used");
            }
            
            // Mark token as used
            qrProof.setUsedAt(LocalDateTime.now());
            qrProof.setUsedBy(technician);
            qrProofRepository.save(qrProof);
        }
        
        // 2. Get entities
        Elevator elevator = elevatorRepository.findById(elevatorId)
                .orElseThrow(() -> new RuntimeException("Elevator not found"));
        
        MaintenanceTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Maintenance template not found"));
        
        // Get technician from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new RuntimeException("User not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User technician = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Technician not found"));
        
        // 3. Create session
        MaintenanceSession session = new MaintenanceSession();
        session.setElevator(elevator);
        session.setTemplate(template);
        session.setTechnician(technician);
        session.setStartAt(LocalDateTime.now());
        session.setStatus(MaintenanceSession.SessionStatus.IN_PROGRESS);
        session.setStartedByQrScan(qrToken != null && !qrToken.trim().isEmpty());
        session.setQrProof(qrProof);
        
        // Link to plan if provided
        if (planId != null) {
            MaintenancePlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Maintenance plan not found"));
            session.setPlan(plan);
            // Update plan status
            plan.setStatus(MaintenancePlan.PlanStatus.IN_PROGRESS);
            planRepository.save(plan);
        }
        
        return sessionRepository.save(session);
    }
    
    /**
     * Get session with full context (sections, items, results)
     */
    public SessionContext getSessionContext(Long sessionId) {
        MaintenanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Maintenance session not found"));
        
        // Load template with sections and items
        MaintenanceTemplate template = templateRepository.findById(session.getTemplate().getId())
                .orElseThrow(() -> new RuntimeException("Template not found"));
        
        // Load existing results
        List<MaintenanceStepResult> results = stepResultRepository.findBySessionIdOrderByItemIdAsc(sessionId);
        
        SessionContext context = new SessionContext();
        context.setSession(session);
        context.setTemplate(template);
        context.setResults(results);
        
        return context;
    }
    
    /**
     * Update step result for an item
     */
    public MaintenanceStepResult updateStepResult(Long sessionId, Long itemId, MaintenanceStepResult.StepResult result, String note) {
        MaintenanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Maintenance session not found"));
        
        MaintenanceItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Maintenance item not found"));
        
        // Find or create result
        Optional<MaintenanceStepResult> existingResult = stepResultRepository.findBySessionIdAndItemId(sessionId, itemId);
        MaintenanceStepResult stepResult;
        
        if (existingResult.isPresent()) {
            stepResult = existingResult.get();
            stepResult.setResult(result);
            stepResult.setNote(note);
        } else {
            stepResult = new MaintenanceStepResult();
            stepResult.setSession(session);
            stepResult.setItem(item);
            stepResult.setResult(result);
            stepResult.setNote(note);
        }
        
        return stepResultRepository.save(stepResult);
    }
    
    /**
     * Finalize maintenance session
     */
    public MaintenanceSession finalizeSession(Long sessionId, String overallNote, String signatureUrl) {
        MaintenanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Maintenance session not found"));
        
        // Validate QR proof exists if started by QR scan
        if (session.getStartedByQrScan() && session.getQrProof() == null) {
            throw new RuntimeException("Session started by QR scan but QR proof is missing");
        }
        
        // Validate minimum duration
        long durationMinutes = ChronoUnit.MINUTES.between(session.getStartAt(), LocalDateTime.now());
        if (durationMinutes < minDurationMinutes) {
            throw new RuntimeException("Session duration must be at least " + minDurationMinutes + " minutes");
        }
        
        // Validate mandatory items
        List<MaintenanceItem> mandatoryItems = itemRepository.findBySectionIdAndIsActiveTrueOrderBySortOrderAsc(
                session.getTemplate().getId()); // This needs to be fixed - should get all items from template
        
        // Get all active items from template sections
        List<MaintenanceItem> allActiveItems = session.getTemplate().getSections().stream()
                .flatMap(section -> section.getItems().stream())
                .filter(MaintenanceItem::getIsActive)
                .filter(MaintenanceItem::getMandatory)
                .collect(java.util.stream.Collectors.toList());
        
        List<MaintenanceStepResult> results = stepResultRepository.findBySessionIdOrderByItemIdAsc(sessionId);
        for (MaintenanceItem mandatoryItem : allActiveItems) {
            boolean hasResult = results.stream()
                    .anyMatch(r -> r.getItem().getId().equals(mandatoryItem.getId()) 
                            && r.getResult() != MaintenanceStepResult.StepResult.NOT_APPLICABLE);
            if (!hasResult) {
                throw new RuntimeException("Mandatory item '" + mandatoryItem.getTitle() + "' must be completed");
            }
        }
        
        // Finalize session
        session.setEndAt(LocalDateTime.now());
        session.setStatus(MaintenanceSession.SessionStatus.COMPLETED);
        session.setOverallNote(overallNote);
        session.setSignatureUrl(signatureUrl);
        
        // Update linked plan if exists
        if (session.getPlan() != null) {
            MaintenancePlan plan = session.getPlan();
            plan.setStatus(MaintenancePlan.PlanStatus.COMPLETED);
            planRepository.save(plan);
        }
        
        return sessionRepository.save(session);
    }
    
    /**
     * Get completed sessions (paginated)
     */
    public Page<MaintenanceSession> getCompletedSessions(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return sessionRepository.findByStatusAndStartAtBetween(
                MaintenanceSession.SessionStatus.COMPLETED, from, to, pageable);
    }
    
    /**
     * Get upcoming sessions (from plans)
     * Returns PLANNED, IN_PROGRESS plans with plannedDate >= CURRENT_DATE
     * CANCELLED and COMPLETED are excluded
     */
    public List<MaintenancePlan> getUpcomingPlans(LocalDate from, LocalDate to, MaintenancePlan.PlanStatus status) {
        System.out.println("========================================");
        System.out.println("MaintenanceSessionService.getUpcomingPlans CALLED");
        System.out.println("Parameters: from=" + from + ", to=" + to + ", status=" + status);
        System.out.println("========================================");
        
        // If no date range provided, default to today and future
        LocalDate today = LocalDate.now();
        if (from == null) {
            from = today; // CRITICAL: Only future and today's plans
        }
        if (to == null) {
            to = LocalDate.now().plusYears(10); // Far future
        }
        
        // CRITICAL: Ensure from >= today (no past dates)
        if (from.isBefore(today)) {
            from = today;
        }
        
        System.out.println("========================================");
        System.out.println("Date range after adjustment: from=" + from + ", to=" + to);
        System.out.println("Today: " + today);
        System.out.println("Filter: status IN (PLANNED, IN_PROGRESS) AND plannedDate >= " + from);
        System.out.println("========================================");
        
        // If status is null, return PLANNED and IN_PROGRESS (upcoming)
        if (status == null) {
            // Use repository method that filters by status IN (PLANNED, IN_PROGRESS) at SQL level
            List<MaintenancePlan> result = planRepository.findByPlannedDateBetweenOrderByPlannedDateAsc(
                    from, to, 
                    java.util.Arrays.asList(MaintenancePlan.PlanStatus.PLANNED, MaintenancePlan.PlanStatus.IN_PROGRESS));
            
            // Additional filter: plannedDate >= today (double check)
            result = result.stream()
                    .filter(p -> !p.getPlannedDate().isBefore(today))
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("========================================");
            System.out.println("QUERY RESULT: " + result.size() + " plans returned");
            System.out.println("Plan IDs: " + result.stream().map(p -> p.getId()).collect(java.util.stream.Collectors.toList()));
            System.out.println("Plan statuses: " + result.stream().map(p -> p.getStatus().name()).collect(java.util.stream.Collectors.toList()));
            System.out.println("Plan dates: " + result.stream().map(p -> p.getPlannedDate()).collect(java.util.stream.Collectors.toList()));
            System.out.println("========================================");
            
            return result;
        } else {
            // Specific status requested - but must be PLANNED or IN_PROGRESS for upcoming
            if (status != MaintenancePlan.PlanStatus.PLANNED && status != MaintenancePlan.PlanStatus.IN_PROGRESS) {
                System.out.println("WARNING: Status " + status + " requested for upcoming plans. Returning empty list.");
                return new java.util.ArrayList<>();
            }
            
            List<MaintenancePlan> result = planRepository.findWithFilters(status, from, to);
            result = result.stream()
                    .filter(p -> !p.getPlannedDate().isBefore(today))
                    .collect(java.util.stream.Collectors.toList());
            
            System.out.println("========================================");
            System.out.println("QUERY RESULT: " + result.size() + " plans with status " + status);
            System.out.println("Plan IDs: " + result.stream().map(p -> p.getId()).collect(java.util.stream.Collectors.toList()));
            System.out.println("========================================");
            
            return result;
        }
    }
    
    /**
     * Hash token (same as QrProofService)
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
    
    /**
     * Session context DTO
     */
    public static class SessionContext {
        private MaintenanceSession session;
        private MaintenanceTemplate template;
        private List<MaintenanceStepResult> results;
        
        public MaintenanceSession getSession() {
            return session;
        }
        
        public void setSession(MaintenanceSession session) {
            this.session = session;
        }
        
        public MaintenanceTemplate getTemplate() {
            return template;
        }
        
        public void setTemplate(MaintenanceTemplate template) {
            this.template = template;
        }
        
        public List<MaintenanceStepResult> getResults() {
            return results;
        }
        
        public void setResults(List<MaintenanceStepResult> results) {
            this.results = results;
        }
    }
}
