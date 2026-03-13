package com.saraasansor.api.repository;

import com.saraasansor.api.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);
    Optional<User> findFirstByB2bUnitIdAndActiveTrue(Long b2bUnitId);
    List<User> findByRoleInOrderByIdAsc(Collection<User.Role> roles);
    List<User> findByRoleNotInOrderByIdAsc(Collection<User.Role> roles);
    @Query("""
            SELECT u
            FROM User u
            WHERE u.role NOT IN :excludedRoles
              AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
              AND (:roleFilter IS NULL OR u.role = :roleFilter)
              AND (:enabledFilter IS NULL OR u.enabled = :enabledFilter)
            """)
    Page<User> searchTenantUsers(@Param("query") String query,
                                 @Param("roleFilter") User.Role roleFilter,
                                 @Param("enabledFilter") Boolean enabledFilter,
                                 @Param("excludedRoles") Collection<User.Role> excludedRoles,
                                 Pageable pageable);
    Optional<User> findByIdAndRoleIn(Long id, Collection<User.Role> roles);
    long countByRoleInAndActiveTrue(Collection<User.Role> roles);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.active = true")
    long countActiveUsersByRole(@Param("role") User.Role role);
}
