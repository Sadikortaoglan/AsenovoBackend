package com.saraasansor.api.service;

import com.saraasansor.api.dto.ManagedUserResponse;
import com.saraasansor.api.dto.SelfPasswordChangeRequest;
import com.saraasansor.api.dto.TenantUserCreateRequest;
import com.saraasansor.api.dto.TenantUserUpdateRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.security.AuthenticatedUserContext;
import com.saraasansor.api.security.AuthenticatedUserContextService;
import com.saraasansor.api.security.UserAuthorizationPolicyService;
import com.saraasansor.api.tenant.service.TenantContextExecutionService;
import com.saraasansor.api.util.AuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTenantAdminTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantContextExecutionService tenantContextExecutionService;

    @Mock
    private AuthenticatedUserContextService authenticatedUserContextService;

    @Mock
    private AuditLogger auditLogger;

    private UserManagementService userManagementService;

    @BeforeEach
    void setUp() {
        userManagementService = new UserManagementService(
                userRepository,
                b2bUnitRepository,
                passwordEncoder,
                tenantContextExecutionService,
                authenticatedUserContextService,
                new UserAuthorizationPolicyService(),
                auditLogger
        );
    }

    @Test
    void tenantAdminCanCreateStaffUser() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());
        when(userRepository.existsByUsername("staff.user")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(15L);
            return user;
        });

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("staff.user");
        request.setPassword("Password123");
        request.setRole(User.Role.STAFF_USER);
        request.setEnabled(true);

        ManagedUserResponse response = userManagementService.createTenantUserForTenantAdmin(request);

        assertThat(response.getId()).isEqualTo(15L);
        assertThat(response.getRole()).isEqualTo("STAFF_USER");
        assertThat(response.getEnabled()).isTrue();
        assertThat(response.getLinkedB2bUnitId()).isNull();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(User.Role.STAFF_USER);
        assertThat(captor.getValue().getUserType()).isEqualTo(User.UserType.STAFF);
        assertThat(captor.getValue().getB2bUnit()).isNull();
    }

    @Test
    void tenantAdminCanCreateCariUserWithLinkedB2bUnit() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());
        when(userRepository.existsByUsername("cari.user")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("ENCODED");

        B2BUnit b2bUnit = new B2BUnit();
        b2bUnit.setId(99L);
        b2bUnit.setName("Cari A");
        when(b2bUnitRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.of(b2bUnit));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(25L);
            return user;
        });

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("cari.user");
        request.setPassword("Password123");
        request.setRole(User.Role.CARI_USER);
        request.setLinkedB2bUnitId(99L);

        ManagedUserResponse response = userManagementService.createTenantUserForTenantAdmin(request);

        assertThat(response.getRole()).isEqualTo("CARI_USER");
        assertThat(response.getLinkedB2bUnitId()).isEqualTo(99L);
        assertThat(response.getLinkedB2bUnitName()).isEqualTo("Cari A");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUserType()).isEqualTo(User.UserType.CARI);
        assertThat(captor.getValue().getB2bUnit()).isNotNull();
        assertThat(captor.getValue().getB2bUnit().getId()).isEqualTo(99L);
    }

    @Test
    void tenantAdminCannotCreatePlatformAdmin() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("platform.user");
        request.setPassword("Password123");
        request.setRole(User.Role.PLATFORM_ADMIN);

        assertThatThrownBy(() -> userManagementService.createTenantUserForTenantAdmin(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("TENANT_ADMIN can only manage STAFF_USER and CARI_USER");
    }

    @Test
    void tenantAdminCannotCreateTenantAdmin() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("tenant.admin");
        request.setPassword("Password123");
        request.setRole(User.Role.TENANT_ADMIN);

        assertThatThrownBy(() -> userManagementService.createTenantUserForTenantAdmin(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("TENANT_ADMIN can only manage STAFF_USER and CARI_USER");
    }

    @Test
    void staffUserCannotAccessTenantUserCrud() {
        when(authenticatedUserContextService.requireContext()).thenReturn(staffUserContext());

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("staff2");
        request.setPassword("Password123");
        request.setRole(User.Role.STAFF_USER);

        assertThatThrownBy(() -> userManagementService.createTenantUserForTenantAdmin(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only TENANT_ADMIN or tenant-scoped PLATFORM_ADMIN can perform this action");
    }

    @Test
    void cariUserCannotAccessTenantUserCrud() {
        when(authenticatedUserContextService.requireContext()).thenReturn(cariUserContext());

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("staff3");
        request.setPassword("Password123");
        request.setRole(User.Role.STAFF_USER);

        assertThatThrownBy(() -> userManagementService.createTenantUserForTenantAdmin(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only TENANT_ADMIN or tenant-scoped PLATFORM_ADMIN can perform this action");
    }

    @Test
    void updateShouldPreservePasswordWhenBlank() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(40L);
        existing.setUsername("staff.user");
        existing.setRole(User.Role.STAFF_USER);
        existing.setPasswordHash("OLD_HASH");
        existing.setActive(true);
        existing.setEnabled(true);
        existing.setLocked(false);

        when(userRepository.findById(40L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantUserUpdateRequest request = new TenantUserUpdateRequest();
        request.setPassword("   ");

        ManagedUserResponse response = userManagementService.updateTenantUserForTenantAdmin(40L, request);

        assertThat(response.getId()).isEqualTo(40L);
        assertThat(existing.getPasswordHash()).isEqualTo("OLD_HASH");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateShouldEncodePasswordWhenChanged() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(41L);
        existing.setUsername("staff.user");
        existing.setRole(User.Role.STAFF_USER);
        existing.setPasswordHash("OLD_HASH");
        existing.setActive(true);
        existing.setEnabled(true);
        existing.setLocked(false);

        when(userRepository.findById(41L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("NEW_HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantUserUpdateRequest request = new TenantUserUpdateRequest();
        request.setPassword("NewPassword123");

        ManagedUserResponse response = userManagementService.updateTenantUserForTenantAdmin(41L, request);

        assertThat(response.getId()).isEqualTo(41L);
        assertThat(existing.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(passwordEncoder).encode("NewPassword123");
    }

    @Test
    void updateShouldRejectLinkedB2bUnitForStaffUser() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(42L);
        existing.setUsername("staff.user");
        existing.setRole(User.Role.STAFF_USER);
        existing.setActive(true);
        existing.setEnabled(true);
        existing.setLocked(false);

        when(userRepository.findById(42L)).thenReturn(Optional.of(existing));

        TenantUserUpdateRequest request = new TenantUserUpdateRequest();
        request.setLinkedB2bUnitId(100L);

        assertThatThrownBy(() -> userManagementService.updateTenantUserForTenantAdmin(42L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("linkedB2bUnitId can only be set for CARI_USER");
    }

    @Test
    void disableAndEnableShouldUpdateActiveAndEnabledFlags() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(43L);
        existing.setUsername("staff.user");
        existing.setRole(User.Role.STAFF_USER);
        existing.setActive(true);
        existing.setEnabled(true);
        existing.setLocked(false);

        when(userRepository.findById(43L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ManagedUserResponse disabled = userManagementService.setTenantUserEnabledForTenantAdmin(43L, false);
        assertThat(disabled.getActive()).isFalse();
        assertThat(disabled.getEnabled()).isFalse();

        ManagedUserResponse enabled = userManagementService.setTenantUserEnabledForTenantAdmin(43L, true);
        assertThat(enabled.getActive()).isTrue();
        assertThat(enabled.getEnabled()).isTrue();
    }

    @Test
    void createCariUserShouldRequireLinkedB2bUnit() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());
        when(userRepository.existsByUsername("cari.user")).thenReturn(false);

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("cari.user");
        request.setPassword("Password123");
        request.setRole(User.Role.CARI_USER);

        assertThatThrownBy(() -> userManagementService.createTenantUserForTenantAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("linkedB2bUnitId is required for CARI_USER");
    }

    @Test
    void tenantAdminCannotManagePlatformUsersInTenantCrud() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(44L);
        existing.setUsername("platform");
        existing.setRole(User.Role.SYSTEM_ADMIN);

        when(userRepository.findById(44L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userManagementService.getTenantUserForTenantAdmin(44L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant user not found");
    }

    @Test
    void tenantAdminListShouldSupportPaginationSearchRoleAndEnabledFilter() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User tenantAdmin = new User();
        tenantAdmin.setId(50L);
        tenantAdmin.setUsername("admin");
        tenantAdmin.setRole(User.Role.STAFF_ADMIN);
        tenantAdmin.setActive(true);
        tenantAdmin.setEnabled(true);

        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.searchTenantUsers(eq("adm"), eq(User.Role.STAFF_ADMIN), eq(true), anyCollection(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(tenantAdmin), pageable, 1));

        Page<ManagedUserResponse> result = userManagementService.getTenantUsersForTenantAdmin("adm", "TENANT_ADMIN", true, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRole()).isEqualTo("TENANT_ADMIN");
        verify(userRepository).searchTenantUsers(eq("adm"), eq(User.Role.STAFF_ADMIN), eq(true), anyCollection(), eq(pageable));
    }

    @Test
    void tenantAdminUpdateWithBlankPasswordShouldNotCallPasswordEncoder() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdminContext());

        User existing = new User();
        existing.setId(45L);
        existing.setUsername("cari.user");
        existing.setRole(User.Role.CARI_USER);
        existing.setPasswordHash("OLD_HASH");
        B2BUnit b2BUnit = new B2BUnit();
        b2BUnit.setId(11L);
        b2BUnit.setName("Cari X");
        existing.setB2bUnit(b2BUnit);

        when(userRepository.findById(45L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantUserUpdateRequest request = new TenantUserUpdateRequest();
        request.setPassword("");

        userManagementService.updateTenantUserForTenantAdmin(45L, request);

        verify(passwordEncoder, never()).encode(any());
        assertThat(existing.getPasswordHash()).isEqualTo("OLD_HASH");
    }

    @Test
    void tenantScopedPlatformAdminCanCreateTenantAdminInsideOwnTenant() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantScopedPlatformAdminContext());
        when(userRepository.existsByUsername("tenant.admin")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(91L);
            return user;
        });

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("tenant.admin");
        request.setPassword("Password123");
        request.setRole(User.Role.TENANT_ADMIN);

        ManagedUserResponse response = userManagementService.createTenantUserForTenantAdmin(request);

        assertThat(response.getId()).isEqualTo(91L);
        assertThat(response.getRole()).isEqualTo("TENANT_ADMIN");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(User.Role.STAFF_ADMIN);
    }

    @Test
    void tenantScopedPlatformAdminCanChangeOwnPassword() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantScopedPlatformAdminContext());

        User existing = new User();
        existing.setId(101L);
        existing.setUsername("platform.local");
        existing.setRole(User.Role.SYSTEM_ADMIN);
        existing.setPasswordHash("OLD_HASH");
        existing.setActive(true);
        existing.setEnabled(true);
        existing.setLocked(true);
        when(userRepository.findByUsername("platform.local")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("OldPass123", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123")).thenReturn("NEW_HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SelfPasswordChangeRequest request = new SelfPasswordChangeRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("NewPass123");

        userManagementService.changeOwnPasswordForTenantPlatformAdmin(request);

        assertThat(existing.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(existing.getLocked()).isFalse();
    }

    @Test
    void tenantScopedPlatformAdminChangeOwnPasswordShouldValidateCurrentPassword() {
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantScopedPlatformAdminContext());

        User existing = new User();
        existing.setId(102L);
        existing.setUsername("platform.local");
        existing.setRole(User.Role.SYSTEM_ADMIN);
        existing.setPasswordHash("OLD_HASH");
        when(userRepository.findByUsername("platform.local")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("WrongPass", "OLD_HASH")).thenReturn(false);

        SelfPasswordChangeRequest request = new SelfPasswordChangeRequest();
        request.setCurrentPassword("WrongPass");
        request.setNewPassword("NewPass123");

        assertThatThrownBy(() -> userManagementService.changeOwnPasswordForTenantPlatformAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    private AuthenticatedUserContext tenantAdminContext() {
        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUserId(1L);
        context.setUsername("tenant-admin");
        context.setRole(User.Role.TENANT_ADMIN);
        context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
        context.setTenantId(10L);
        context.setTenantSchema("tenant_10");
        return context;
    }

    private AuthenticatedUserContext staffUserContext() {
        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUserId(2L);
        context.setUsername("staff-user");
        context.setRole(User.Role.STAFF_USER);
        context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
        context.setTenantId(10L);
        context.setTenantSchema("tenant_10");
        return context;
    }

    private AuthenticatedUserContext cariUserContext() {
        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUserId(3L);
        context.setUsername("cari-user");
        context.setRole(User.Role.CARI_USER);
        context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
        context.setTenantId(10L);
        context.setTenantSchema("tenant_10");
        context.setLinkedB2bUnitId(77L);
        return context;
    }

    private AuthenticatedUserContext tenantScopedPlatformAdminContext() {
        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUserId(4L);
        context.setUsername("platform.local");
        context.setRole(User.Role.PLATFORM_ADMIN);
        context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
        context.setTenantId(10L);
        context.setTenantSchema("tenant_10");
        return context;
    }
}
