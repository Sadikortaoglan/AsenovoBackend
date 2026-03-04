package com.saraasansor.api.service;

import com.saraasansor.api.dto.CreateB2BUnitRequest;
import com.saraasansor.api.dto.UpdateB2BUnitRequest;
import com.saraasansor.api.model.B2BCurrency;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.B2BUnitGroup;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitGroupRepository;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Transactional
public class B2BUnitService {

    private final B2BUnitRepository b2bUnitRepository;
    private final B2BUnitGroupRepository b2bUnitGroupRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public B2BUnitService(B2BUnitRepository b2bUnitRepository,
                          B2BUnitGroupRepository b2bUnitGroupRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.b2bUnitRepository = b2bUnitRepository;
        this.b2bUnitGroupRepository = b2bUnitGroupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<B2BUnit> getB2BUnits(String query, Pageable pageable) {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException("CARI users cannot access B2B unit list");
        }

        String normalizedQuery = normalizeNullable(query);
        return b2bUnitRepository.search(normalizedQuery, pageable);
    }

    @Transactional(readOnly = true)
    public B2BUnit getB2BUnitById(Long id) {
        B2BUnit unit = b2bUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(unit.getId());
        return unit;
    }

    @Transactional(readOnly = true)
    public B2BUnit getMyB2BUnit() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new RuntimeException("User not authenticated");
        }
        if (currentUser.getRole() != User.Role.CARI_USER) {
            throw new AccessDeniedException("Only CARI users can access own B2B unit");
        }
        if (currentUser.getB2bUnit() == null) {
            throw new RuntimeException("No B2B unit linked to current user");
        }
        return getB2BUnitById(currentUser.getB2bUnit().getId());
    }

    public B2BUnit createB2BUnit(CreateB2BUnitRequest request) {
        enforceCreateAccess();

        String normalizedPortalUsername = normalizeNullable(request.getPortalUsername());
        validatePortalUsernameDuplicate(normalizedPortalUsername, null);

        B2BUnit unit = new B2BUnit();
        applyRequestToUnit(unit, request);
        unit.setPortalUsername(normalizedPortalUsername);

        String rawPassword = request.getPortalPassword();
        String encodedPassword = null;
        if (StringUtils.hasText(rawPassword)) {
            encodedPassword = passwordEncoder.encode(rawPassword.trim());
            unit.setPortalPasswordHash(encodedPassword);
        }

        B2BUnit saved = b2bUnitRepository.save(unit);
        synchronizeCariUser(saved, rawPassword, encodedPassword);
        return saved;
    }

    public B2BUnit updateB2BUnit(Long id, UpdateB2BUnitRequest request) {
        B2BUnit unit = b2bUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(unit.getId());

        String normalizedPortalUsername = request.getPortalUsername() != null
                ? normalizeNullable(request.getPortalUsername())
                : unit.getPortalUsername();
        validatePortalUsernameDuplicate(normalizedPortalUsername, id);

        applyRequestToUnit(unit, request);
        unit.setPortalUsername(normalizedPortalUsername);

        String rawPassword = request.getPortalPassword();
        String encodedPassword = null;
        if (StringUtils.hasText(rawPassword)) {
            encodedPassword = passwordEncoder.encode(rawPassword.trim());
            unit.setPortalPasswordHash(encodedPassword);
        }

        B2BUnit saved = b2bUnitRepository.save(unit);
        synchronizeCariUser(saved, rawPassword, encodedPassword);
        return saved;
    }

    public void deleteB2BUnit(Long id) {
        B2BUnit unit = b2bUnitRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        enforceObjectAccess(unit.getId());
        unit.setActive(false);
        b2bUnitRepository.save(unit);
        userRepository.findFirstByB2bUnitIdAndActiveTrue(id).ifPresent(this::deactivateUser);
    }

    private void applyRequestToUnit(B2BUnit unit, CreateB2BUnitRequest request) {
        unit.setName(normalizeRequired(request.getName()));
        unit.setTaxNumber(normalizeNullable(request.getTaxNumber()));
        unit.setTaxOffice(normalizeNullable(request.getTaxOffice()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setCurrency(request.getCurrency() != null ? request.getCurrency() : B2BCurrency.TRY);
        unit.setRiskLimit(request.getRiskLimit() != null ? request.getRiskLimit() : BigDecimal.ZERO);
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setDescription(normalizeNullable(request.getDescription()));
        unit.setGroup(resolveGroup(request.getGroupId()));
    }

    private void applyRequestToUnit(B2BUnit unit, UpdateB2BUnitRequest request) {
        unit.setName(normalizeRequired(request.getName()));
        unit.setTaxNumber(normalizeNullable(request.getTaxNumber()));
        unit.setTaxOffice(normalizeNullable(request.getTaxOffice()));
        unit.setPhone(normalizeNullable(request.getPhone()));
        unit.setEmail(normalizeNullable(request.getEmail()));
        unit.setCurrency(request.getCurrency() != null ? request.getCurrency() : B2BCurrency.TRY);
        unit.setRiskLimit(request.getRiskLimit() != null ? request.getRiskLimit() : BigDecimal.ZERO);
        unit.setAddress(normalizeNullable(request.getAddress()));
        unit.setDescription(normalizeNullable(request.getDescription()));
        unit.setGroup(resolveGroup(request.getGroupId()));
    }

    private B2BUnitGroup resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return b2bUnitGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("B2B unit group not found"));
    }

    private void validatePortalUsernameDuplicate(String portalUsername, Long currentId) {
        if (!StringUtils.hasText(portalUsername)) {
            return;
        }

        boolean exists = currentId == null
                ? b2bUnitRepository.existsByPortalUsernameAndActiveTrue(portalUsername)
                : b2bUnitRepository.existsByPortalUsernameAndActiveTrueAndIdNot(portalUsername, currentId);

        if (exists) {
            throw new RuntimeException("Portal username already exists");
        }
    }

    private void synchronizeCariUser(B2BUnit unit, String rawPassword, String encodedPassword) {
        Optional<User> existingCariUserOpt = userRepository.findFirstByB2bUnitIdAndActiveTrue(unit.getId());
        String portalUsername = normalizeNullable(unit.getPortalUsername());
        boolean passwordProvided = StringUtils.hasText(rawPassword);
        Optional<User> usernameOwnerOpt = StringUtils.hasText(portalUsername)
                ? userRepository.findByUsername(portalUsername)
                : Optional.empty();

        if (!StringUtils.hasText(portalUsername)) {
            if (existingCariUserOpt.isPresent()) {
                deactivateUser(existingCariUserOpt.get());
                unit.setPortalPasswordHash(null);
                b2bUnitRepository.save(unit);
            }
            return;
        }

        if (existingCariUserOpt.isPresent()) {
            User existingCariUser = existingCariUserOpt.get();
            if (usernameOwnerOpt.isPresent() && !usernameOwnerOpt.get().getId().equals(existingCariUser.getId())) {
                throw new RuntimeException("Portal username already exists");
            }

            existingCariUser.setUsername(portalUsername);
            existingCariUser.setRole(User.Role.CARI_USER);
            existingCariUser.setUserType(User.UserType.CARI);
            existingCariUser.setB2bUnit(unit);
            existingCariUser.setEnabled(true);
            existingCariUser.setActive(true);
            existingCariUser.setLocked(false);

            if (passwordProvided) {
                String passwordHash = encodedPassword != null
                        ? encodedPassword
                        : passwordEncoder.encode(rawPassword.trim());
                existingCariUser.setPasswordHash(passwordHash);
                unit.setPortalPasswordHash(passwordHash);
                b2bUnitRepository.save(unit);
            }

            userRepository.save(existingCariUser);
            return;
        }

        if (usernameOwnerOpt.isPresent()) {
            throw new RuntimeException("Portal username already exists");
        }

        if (!passwordProvided && !StringUtils.hasText(unit.getPortalPasswordHash())) {
            throw new RuntimeException("Portal password is required when creating a CARI user account");
        }

        String passwordHash = unit.getPortalPasswordHash();
        if (passwordProvided) {
            passwordHash = encodedPassword != null
                    ? encodedPassword
                    : passwordEncoder.encode(rawPassword.trim());
            unit.setPortalPasswordHash(passwordHash);
            b2bUnitRepository.save(unit);
        }

        User user = new User();
        user.setUsername(portalUsername);
        user.setPasswordHash(passwordHash);
        user.setRole(User.Role.CARI_USER);
        user.setUserType(User.UserType.CARI);
        user.setActive(true);
        user.setEnabled(true);
        user.setLocked(false);
        user.setB2bUnit(unit);
        userRepository.save(user);
    }

    private void deactivateUser(User user) {
        user.setActive(false);
        user.setEnabled(false);
        userRepository.save(user);
    }

    private void enforceCreateAccess() {
        User currentUser = getCurrentUser();
        if (currentUser != null && currentUser.getRole() == User.Role.CARI_USER) {
            throw new AccessDeniedException("CARI users cannot create B2B units");
        }
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
