package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantContextExecutionServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private TenantContextExecutionService service;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any());
        doNothing().when(transactionManager).rollback(any());
        service = new TenantContextExecutionService(tenantRepository, transactionManager);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void resolveTenantContextShouldReadFromPublicScopeAndRestorePreviousTenantContext() {
        TenantDescriptor previousTenant = new TenantDescriptor(
                99L,
                "Previous",
                "previous",
                Tenant.TenancyMode.SHARED_SCHEMA,
                "tenant_previous",
                null,
                null,
                null,
                null,
                null,
                null,
                Tenant.TenantStatus.ACTIVE,
                null,
                null
        );
        TenantContext.setCurrentTenant(previousTenant);

        Tenant tenant = createTenant(3L, "tenant_three");
        when(tenantRepository.findByIdWithPlan(3L)).thenAnswer(invocation -> {
            assertThat(TenantContext.getCurrentTenant()).isNull();
            return Optional.of(tenant);
        });

        TenantDescriptor resolved = service.resolveTenantContext(3L);

        assertThat(resolved.getId()).isEqualTo(3L);
        assertThat(resolved.getSchemaName()).isEqualTo("tenant_three");
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(previousTenant);
    }

    @Test
    void executeInTenantContextWriteShouldApplyTenantContextBeforeTransactionStart() {
        Tenant tenant = createTenant(3L, "tenant_three");
        when(tenantRepository.findByIdWithPlan(3L)).thenReturn(Optional.of(tenant));
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(definition.isReadOnly()).isFalse();
            assertThat(TenantContext.getCurrentTenant()).isNotNull();
            assertThat(TenantContext.getCurrentTenant().getSchemaName()).isEqualTo("tenant_three");
            return new SimpleTransactionStatus();
        });

        Integer result = service.executeInTenantContextWrite(3L, () -> {
            assertThat(TenantContext.getCurrentTenant()).isNotNull();
            assertThat(TenantContext.getCurrentTenant().getSchemaName()).isEqualTo("tenant_three");
            return 42;
        });

        assertThat(result).isEqualTo(42);
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void executeInTenantContextReadOnlyShouldOpenReadOnlyTransactionInTenantSchema() {
        Tenant tenant = createTenant(4L, "tenant_four");
        when(tenantRepository.findByIdWithPlan(4L)).thenReturn(Optional.of(tenant));
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            TransactionDefinition definition = invocation.getArgument(0);
            assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            assertThat(definition.isReadOnly()).isTrue();
            assertThat(TenantContext.getCurrentTenant()).isNotNull();
            assertThat(TenantContext.getCurrentTenant().getSchemaName()).isEqualTo("tenant_four");
            return new SimpleTransactionStatus();
        });

        String value = service.executeInTenantContextReadOnly(4L, () -> {
            assertThat(TenantContext.getCurrentTenant()).isNotNull();
            assertThat(TenantContext.getCurrentTenant().getSchemaName()).isEqualTo("tenant_four");
            return "ok";
        });

        assertThat(value).isEqualTo("ok");
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    private Tenant createTenant(Long id, String schemaName) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName("Tenant " + id);
        tenant.setCompanyName("Tenant " + id);
        tenant.setSubdomain("tenant" + id);
        tenant.setTenancyMode(Tenant.TenancyMode.SHARED_SCHEMA);
        tenant.setSchemaName(schemaName);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setPlanType(Tenant.PlanType.PROFESSIONAL);
        return tenant;
    }
}
