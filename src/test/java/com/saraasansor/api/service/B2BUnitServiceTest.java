package com.saraasansor.api.service;

import com.saraasansor.api.dto.B2BUnitDetailResponse;
import com.saraasansor.api.dto.UpdateB2BUnitRequest;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitGroupRepository;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class B2BUnitServiceTest {

    @Mock
    private B2BUnitRepository b2bUnitRepository;

    @Mock
    private B2BUnitGroupRepository b2bUnitGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private B2BUnitService b2bUnitService;

    @BeforeEach
    void setUp() {
        b2bUnitService = new B2BUnitService(
                b2bUnitRepository,
                b2bUnitGroupRepository,
                userRepository,
                passwordEncoder
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateShouldPreservePasswordHashWhenPasswordIsBlank() {
        B2BUnit existing = new B2BUnit();
        existing.setId(10L);
        existing.setName("Old Name");
        existing.setPortalUsername("cari-user");
        existing.setPortalPasswordHash("existing-hash");

        UpdateB2BUnitRequest request = new UpdateB2BUnitRequest();
        request.setName("Updated Name");
        request.setRiskLimit(BigDecimal.TEN);
        request.setPortalPassword("   ");

        when(b2bUnitRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(existing));
        when(b2bUnitRepository.save(any(B2BUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findFirstByB2bUnitIdAndActiveTrue(10L)).thenReturn(Optional.empty());
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.empty());

        B2BUnit updated = b2bUnitService.updateB2BUnit(10L, request);

        assertThat(updated.getPortalPasswordHash()).isEqualTo("existing-hash");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateShouldHashAndReplacePasswordWhenPasswordIsProvided() {
        B2BUnit existing = new B2BUnit();
        existing.setId(20L);
        existing.setName("Old Name");
        existing.setPortalUsername("cari-user");
        existing.setPortalPasswordHash("existing-hash");

        User cariUser = new User();
        cariUser.setId(200L);
        cariUser.setUsername("cari-user");
        cariUser.setRole(User.Role.CARI_USER);
        cariUser.setUserType(User.UserType.CARI);
        cariUser.setB2bUnit(existing);
        cariUser.setPasswordHash("existing-hash");
        cariUser.setActive(true);
        cariUser.setEnabled(true);

        UpdateB2BUnitRequest request = new UpdateB2BUnitRequest();
        request.setName("Updated Name");
        request.setRiskLimit(BigDecimal.ONE);
        request.setPortalPassword("new-secret");

        when(b2bUnitRepository.findByIdAndActiveTrue(20L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("new-secret")).thenReturn("hashed-secret");
        when(b2bUnitRepository.save(any(B2BUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findFirstByB2bUnitIdAndActiveTrue(20L)).thenReturn(Optional.of(cariUser));
        when(userRepository.findByUsername("cari-user")).thenReturn(Optional.of(cariUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        B2BUnit updated = b2bUnitService.updateB2BUnit(20L, request);

        assertThat(updated.getPortalPasswordHash()).isEqualTo("hashed-secret");
        verify(passwordEncoder, times(1)).encode(eq("new-secret"));
    }

    @Test
    void cariUserShouldNotAccessAnotherB2BUnit() {
        B2BUnit targetUnit = new B2BUnit();
        targetUnit.setId(300L);
        targetUnit.setName("Another Unit");

        authenticateAs("cari-username");
        User currentUser = new User();
        currentUser.setUsername("cari-username");
        currentUser.setRole(User.Role.CARI_USER);
        currentUser.setUserType(User.UserType.CARI);
        B2BUnit ownUnit = new B2BUnit();
        ownUnit.setId(301L);
        currentUser.setB2bUnit(ownUnit);

        when(userRepository.findByUsername("cari-username")).thenReturn(Optional.of(currentUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(300L)).thenReturn(Optional.of(targetUnit));

        assertThatThrownBy(() -> b2bUnitService.getB2BUnitById(300L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit");
    }

    @Test
    void staffUserShouldAccessB2BUnit() {
        B2BUnit targetUnit = new B2BUnit();
        targetUnit.setId(400L);
        targetUnit.setName("Target Unit");

        authenticateAs("staff-user");
        User staffUser = new User();
        staffUser.setUsername("staff-user");
        staffUser.setRole(User.Role.STAFF_USER);
        staffUser.setUserType(User.UserType.STAFF);

        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(400L)).thenReturn(Optional.of(targetUnit));

        B2BUnit found = b2bUnitService.getB2BUnitById(400L);

        assertThat(found.getId()).isEqualTo(400L);
    }

    @Test
    void detailShouldReturnBaseDataWithMenuAndZeroSummary() {
        B2BUnit targetUnit = new B2BUnit();
        targetUnit.setId(410L);
        targetUnit.setName("Target Unit");
        targetUnit.setEmail("cari@example.com");
        targetUnit.setPhone("+905551112233");
        targetUnit.setTaxNumber("1234567890");
        targetUnit.setTaxOffice("Kadikoy");
        targetUnit.setAddress("Istanbul");
        targetUnit.setPortalUsername("cari-410");
        targetUnit.setActive(true);

        authenticateAs("staff-user");
        User staffUser = new User();
        staffUser.setUsername("staff-user");
        staffUser.setRole(User.Role.STAFF_USER);
        staffUser.setUserType(User.UserType.STAFF);

        when(userRepository.findByUsername("staff-user")).thenReturn(Optional.of(staffUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(410L)).thenReturn(Optional.of(targetUnit));

        B2BUnitDetailResponse detail = b2bUnitService.getB2BUnitDetail(410L);

        assertThat(detail.getId()).isEqualTo(410L);
        assertThat(detail.getCode()).isEqualTo("cari-410");
        assertThat(detail.getStatus()).isEqualTo("ACTIVE");
        assertThat(detail.getMenus()).hasSize(6);
        assertThat(detail.getMenus())
                .extracting(B2BUnitDetailResponse.MenuItem::getKey)
                .containsExactly("filter", "invoice", "account-transactions", "collection", "payment", "reporting");
        assertThat(detail.getSummary()).isNotNull();
        assertThat(detail.getSummary().getTotalIncome()).isEqualByComparingTo("0");
        assertThat(detail.getSummary().getTotalExpense()).isEqualByComparingTo("0");
        assertThat(detail.getSummary().getTotalBalance()).isEqualByComparingTo("0");
    }

    @Test
    void detailShouldThrowNotFoundWhenB2BUnitDoesNotExist() {
        when(b2bUnitRepository.findByIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> b2bUnitService.getB2BUnitDetail(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("B2B unit not found");
    }

    @Test
    void detailShouldForbidCariUserForAnotherB2BUnit() {
        B2BUnit targetUnit = new B2BUnit();
        targetUnit.setId(420L);
        targetUnit.setName("Another Unit");

        authenticateAs("cari-username");
        User currentUser = new User();
        currentUser.setUsername("cari-username");
        currentUser.setRole(User.Role.CARI_USER);
        currentUser.setUserType(User.UserType.CARI);
        B2BUnit ownUnit = new B2BUnit();
        ownUnit.setId(421L);
        currentUser.setB2bUnit(ownUnit);

        when(userRepository.findByUsername("cari-username")).thenReturn(Optional.of(currentUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(420L)).thenReturn(Optional.of(targetUnit));

        assertThatThrownBy(() -> b2bUnitService.getB2BUnitDetail(420L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("only access own B2B unit");
    }

    @Test
    void systemAdminShouldAccessAndDeleteAnyB2BUnit() {
        B2BUnit targetUnit = new B2BUnit();
        targetUnit.setId(500L);
        targetUnit.setName("Target Unit");
        targetUnit.setActive(true);

        authenticateAs("sys-admin");
        User adminUser = new User();
        adminUser.setUsername("sys-admin");
        adminUser.setRole(User.Role.SYSTEM_ADMIN);
        adminUser.setUserType(User.UserType.SYSTEM_ADMIN);

        when(userRepository.findByUsername("sys-admin")).thenReturn(Optional.of(adminUser));
        when(b2bUnitRepository.findByIdAndActiveTrue(500L)).thenReturn(Optional.of(targetUnit));
        when(b2bUnitRepository.save(any(B2BUnit.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findFirstByB2bUnitIdAndActiveTrue(500L)).thenReturn(Optional.empty());

        b2bUnitService.deleteB2BUnit(500L);

        assertThat(targetUnit.getActive()).isFalse();
    }

    private void authenticateAs(String username) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(
                        username,
                        "x",
                        Collections.emptyList()
                );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
