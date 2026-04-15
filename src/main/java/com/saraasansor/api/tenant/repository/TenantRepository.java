package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("SELECT t FROM Tenant t JOIN FETCH t.plan WHERE t.subdomain = :subdomain AND t.active = true")
    Optional<Tenant> findBySubdomainAndActiveIsTrue(@Param("subdomain") String subdomain);

    @Query("SELECT t FROM Tenant t JOIN FETCH t.plan WHERE t.subdomain = :subdomain")
    Optional<Tenant> findBySubdomain(@Param("subdomain") String subdomain);

    @Query("SELECT t FROM Tenant t JOIN FETCH t.plan WHERE t.subdomain = :subdomain")
    Optional<Tenant> findBySubdomainWithPlan(@Param("subdomain") String subdomain);

    @Query("SELECT t FROM Tenant t JOIN FETCH t.plan WHERE t.id = :id")
    Optional<Tenant> findByIdWithPlan(@Param("id") Long id);

    List<Tenant> findByActiveTrueAndStatusOrderByIdAsc(Tenant.TenantStatus status);

    Optional<Tenant> findFirstByOrderByIdAsc();

    @Query("""
            SELECT t FROM Tenant t
            WHERE
            (
                :queryPattern IS NULL
                OR LOWER(COALESCE(t.companyName, t.name)) LIKE :queryPattern
                OR LOWER(t.subdomain) LIKE :queryPattern
            )
            AND (:status IS NULL OR t.status = :status)
            """)
    Page<Tenant> search(@Param("queryPattern") String queryPattern,
                        @Param("status") Tenant.TenantStatus status,
                        Pageable pageable);

    boolean existsBySubdomain(String subdomain);

    boolean existsBySchemaName(String schemaName);
}
