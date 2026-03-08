package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitTransactionPageResponse;
import com.saraasansor.api.dto.B2BUnitTransactionResponse;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitTransaction;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.B2BUnitTransactionRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@Transactional
public class B2BUnitTransactionService {

    private static final LocalDate DEFAULT_START_DATE = LocalDate.of(1970, 1, 1);
    private static final LocalDate DEFAULT_END_DATE = LocalDate.of(2999, 12, 31);

    private final B2BUnitTransactionRepository transactionRepository;
    private final B2BUnitRepository b2bUnitRepository;
    private final UserRepository userRepository;

    public B2BUnitTransactionService(B2BUnitTransactionRepository transactionRepository,
                                     B2BUnitRepository b2bUnitRepository,
                                     UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.b2bUnitRepository = b2bUnitRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public B2BUnitTransactionPageResponse getTransactions(Long b2bUnitId,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          String search,
                                                          Pageable pageable) {
        B2BUnit b2bUnit = b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(b2bUnit.getId());

        LocalDate effectiveStartDate = startDate != null ? startDate : DEFAULT_START_DATE;
        LocalDate effectiveEndDate = endDate != null ? endDate : DEFAULT_END_DATE;
        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            throw new RuntimeException("startDate cannot be after endDate");
        }

        String normalizedSearch = normalizeNullable(search);
        B2BUnitTransaction.TransactionType transactionType = resolveTransactionType(normalizedSearch);

        PageRequest sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.ASC, "transactionDate").and(Sort.by(Sort.Direction.ASC, "id"))
        );

        Page<B2BUnitTransaction> pageResult = transactionRepository.searchLedger(
                b2bUnitId,
                effectiveStartDate,
                effectiveEndDate,
                normalizedSearch,
                transactionType,
                sortedPageable
        );

        B2BUnitTransactionPageResponse response = new B2BUnitTransactionPageResponse();
        response.setContent(pageResult.map(B2BUnitTransactionResponse::fromEntity).getContent());
        response.setPage(pageResult.getNumber());
        response.setSize(pageResult.getSize());
        response.setTotalElements(pageResult.getTotalElements());
        response.setTotalPages(pageResult.getTotalPages());
        return response;
    }

    public byte[] exportTransactionsExcel(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("Excel export is not implemented yet");
    }

    public byte[] exportTransactionsExcel() {
        throw new UnsupportedOperationException("Excel export is not implemented yet");
    }

    public byte[] exportTransactionsPdf(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("PDF export is not implemented yet");
    }

    public byte[] exportTransactionsPdf() {
        throw new UnsupportedOperationException("PDF export is not implemented yet");
    }

    public byte[] exportTransactionsCsv(Long b2bUnitId, LocalDate startDate, LocalDate endDate, String search) {
        throw new UnsupportedOperationException("CSV export is not implemented yet");
    }

    public byte[] exportTransactionsCsv() {
        throw new UnsupportedOperationException("CSV export is not implemented yet");
    }

    private B2BUnitTransaction.TransactionType resolveTransactionType(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String normalized = search.trim().toUpperCase();
        for (B2BUnitTransaction.TransactionType value : B2BUnitTransaction.TransactionType.values()) {
            if (value.name().equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    private void enforceObjectAccess(Long b2bUnitId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return;
        }

        if (currentUser.getRole() == User.Role.CARI_USER) {
            Long ownB2bUnitId = currentUser.getB2bUnit() != null ? currentUser.getB2bUnit().getId() : null;
            if (ownB2bUnitId == null || !ownB2bUnitId.equals(b2bUnitId)) {
                throw new AccessDeniedException("CARI user can only access own B2B unit");
            }
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName && !"anonymousUser".equals(principalName)) {
            username = principalName;
        }

        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
