package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    @Query("SELECT t FROM Tenant t JOIN FETCH t.plan WHERE t.subdomain = :subdomain AND t.active = true")
    Optional<Tenant> findBySubdomainAndActiveIsTrue(@Param("subdomain") String subdomain);

    boolean existsBySubdomain(String subdomain);
}

