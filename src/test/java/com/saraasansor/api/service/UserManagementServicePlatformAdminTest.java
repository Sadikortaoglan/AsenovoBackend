package com.saraasansor.api.service;

import com.saraasansor.api.dto.ManagedUserResponse;
import com.saraasansor.api.dto.PlatformTenantUserResetPasswordRequest;
import com.saraasansor.api.dto.TenantUserCreateRequest;
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
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServicePlatformAdminTest {

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
    void platformAdminCanListUsersOfSelectedTenantWithFilters() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        User tenantAdmin = new User();
        tenantAdmin.setId(100L);
        tenantAdmin.setUsername("tenant-admin");
        tenantAdmin.setRole(User.Role.STAFF_ADMIN);
        tenantAdmin.setEnabled(true);
        tenantAdmin.setActive(true);
        tenantAdmin.setLocked(false);

        PageRequest pageable = PageRequest.of(0, 20);
        when(userRepository.searchTenantUsers(eq("adm"), eq(User.Role.STAFF_ADMIN), eq(true), anyCollection(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(tenantAdmin), pageable, 1));

        Page<ManagedUserResponse> page = userManagementService.getTenantUsersForPlatform(10L, "adm", "TENANT_ADMIN", true, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getRole()).isEqualTo("TENANT_ADMIN");
        verify(auditLogger).log(eq("TENANT_USERS_LISTED_BY_PLATFORM"), eq("TENANT"), eq(10L), anyMap());
    }

    @Test
    void platformAdminCanReadTenantUserDetail() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        User user = new User();
        user.setId(22L);
        user.setUsername("staff.user");
        user.setRole(User.Role.STAFF_USER);
        user.setEnabled(true);
        user.setActive(true);

        when(userRepository.findById(22L)).thenReturn(Optional.of(user));

        ManagedUserResponse response = userManagementService.getTenantUserForPlatform(10L, 22L);

        assertThat(response.getId()).isEqualTo(22L);
        assertThat(response.getUsername()).isEqualTo("staff.user");
        assertThat(response.getRole()).isEqualTo("STAFF_USER");
        verify(auditLogger).log(eq("TENANT_USER_VIEWED_BY_PLATFORM"), eq("USER"), eq(22L), anyMap());
    }

    @Test
    void platformAdminCanCreateTenantAdmin() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(userRepository.existsByUsername("tenant.admin")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(301L);
            return saved;
        });

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("tenant.admin");
        request.setPassword("Password123");
        request.setRole(User.Role.TENANT_ADMIN);

        ManagedUserResponse response = userManagementService.createTenantUserForPlatform(10L, request);

        assertThat(response.getId()).isEqualTo(301L);
        assertThat(response.getRole()).isEqualTo("TENANT_ADMIN");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(User.Role.STAFF_ADMIN);
    }

    @Test
    void platformAdminCanResetTenantUserPasswordAndPasswordIsEncoded() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        User tenantAdmin = new User();
        tenantAdmin.setId(50L);
        tenantAdmin.setUsername("tenant-admin");
        tenantAdmin.setRole(User.Role.STAFF_ADMIN);
        tenantAdmin.setPasswordHash("OLD_HASH");
        tenantAdmin.setEnabled(true);
        tenantAdmin.setActive(true);
        tenantAdmin.setLocked(true);

        when(userRepository.findById(50L)).thenReturn(Optional.of(tenantAdmin));
        when(passwordEncoder.encode("NewPassword123")).thenReturn("NEW_HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformTenantUserResetPasswordRequest request = new PlatformTenantUserResetPasswordRequest();
        request.setNewPassword("NewPassword123");

        ManagedUserResponse response = userManagementService.resetTenantUserPasswordForPlatform(10L, 50L, request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(tenantAdmin.getPasswordHash()).isEqualTo("NEW_HASH");
        assertThat(tenantAdmin.getLocked()).isFalse();
        verify(passwordEncoder).encode("NewPassword123");
        verify(auditLogger).log(eq("TENANT_USER_PASSWORD_RESET_BY_PLATFORM"), eq("USER"), eq(50L), anyMap());
    }

    @Test
    void platformAdminResetPasswordCannotTargetPlatformUserInTenantFlow() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        User platformUser = new User();
        platformUser.setId(80L);
        platformUser.setUsername("platform");
        platformUser.setRole(User.Role.SYSTEM_ADMIN);
        when(userRepository.findById(80L)).thenReturn(Optional.of(platformUser));

        PlatformTenantUserResetPasswordRequest request = new PlatformTenantUserResetPasswordRequest();
        request.setNewPassword("NewPassword123");

        assertThatThrownBy(() -> userManagementService.resetTenantUserPasswordForPlatform(10L, 80L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tenant user not found");
    }

    @Test
    void nonPlatformRoleCannotUsePlatformTenantUserManagement() {
        AuthenticatedUserContext tenantAdmin = new AuthenticatedUserContext();
        tenantAdmin.setUserId(2L);
        tenantAdmin.setUsername("tenant-admin");
        tenantAdmin.setRole(User.Role.TENANT_ADMIN);
        tenantAdmin.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.TENANT);
        tenantAdmin.setTenantId(10L);
        when(authenticatedUserContextService.requireContext()).thenReturn(tenantAdmin);

        assertThatThrownBy(() -> userManagementService.getTenantUsersForPlatform(10L, null, null, null, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only PLATFORM_ADMIN can perform this action");

        verifyNoInteractions(tenantContextExecutionService);
    }

    @Test
    void platformAdminCreateCariUserShouldRequireLinkedB2bUnit() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextWrite(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });
        when(userRepository.existsByUsername("cari.platform")).thenReturn(false);

        TenantUserCreateRequest request = new TenantUserCreateRequest();
        request.setUsername("cari.platform");
        request.setPassword("Password123");
        request.setRole(User.Role.CARI_USER);

        assertThatThrownBy(() -> userManagementService.createTenantUserForPlatform(10L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("linkedB2bUnitId is required for CARI_USER");
    }

    @Test
    void platformAdminCanReadTenantB2BUnitLookupInExplicitTenantContext() {
        when(authenticatedUserContextService.requireContext()).thenReturn(platformAdminContext());
        when(tenantContextExecutionService.executeInTenantContextReadOnly(eq(10L), any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(1);
            return supplier.get();
        });

        B2BUnit unit = new B2BUnit();
        unit.setId(901L);
        unit.setName("Cari A");
        when(b2bUnitRepository.findActiveLookup(eq("cari"), any()))
                .thenReturn(List.of(unit));

        assertThat(userManagementService.getTenantB2BUnitLookupForPlatform(10L, "cari"))
                .extracting("id", "name")
                .containsExactly(org.assertj.core.groups.Tuple.tuple(901L, "Cari A"));
    }

    private AuthenticatedUserContext platformAdminContext() {
        AuthenticatedUserContext context = new AuthenticatedUserContext();
        context.setUserId(1L);
        context.setUsername("platform-admin");
        context.setRole(User.Role.PLATFORM_ADMIN);
        context.setAuthScopeType(AuthenticatedUserContext.AuthScopeType.PLATFORM);
        context.setTenantId(null);
        return context;
    }
}
