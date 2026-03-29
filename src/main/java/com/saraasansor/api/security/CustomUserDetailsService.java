package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final String ROLE_PREFIX = "ROLE_";
    private final String ROLE_STAFF_ADMIN = "ROLE_STAFF_ADMIN";
    private final String ROLE_STAFF_USER = "ROLE_STAFF_USER";
    private final String ROLE_CARI_USER = "ROLE_CARI_USER";
    private static final String ROLE_PLATFORM_ADMIN = "ROLE_PLATFORM_ADMIN";
    private static final String ROLE_TENANT_ADMIN = "ROLE_TENANT_ADMIN";
    private static final String ROLE_SYSTEM_ADMIN = "ROLE_SYSTEM_ADMIN";
    private final String ROLE_ADMIN = "ROLE_ADMIN";
    private final String ROLE_TECHNICIAN = "ROLE_TECHNICIAN";

    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        User.Role canonicalRole = user.getCanonicalRole();
        if (canonicalRole == null) {
            throw new UsernameNotFoundException("User role is missing");
        }

        boolean tenantRequest = TenantContext.hasTenant();
        if (tenantRequest && canonicalRole.isPlatformAdmin()) {
            throw new UsernameNotFoundException("Platform users cannot authenticate in tenant scope");
        }

        if (!tenantRequest && !canonicalRole.isPlatformAdmin()) {
            throw new UsernameNotFoundException("Tenant users must authenticate from tenant scope");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getActive()) && Boolean.TRUE.equals(user.getEnabled()),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !Boolean.TRUE.equals(user.getLocked()),
                getAuthorities(canonicalRole)
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User.Role canonicalRole) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + canonicalRole.name()));

        if (canonicalRole == User.Role.PLATFORM_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(ROLE_PLATFORM_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_SYSTEM_ADMIN)); // compatibility
        } else if (canonicalRole == User.Role.TENANT_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(ROLE_TENANT_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_STAFF_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_TECHNICIAN));// compatibility
        } else if(canonicalRole == User.Role.STAFF_USER){
            authorities.add(new SimpleGrantedAuthority(ROLE_TECHNICIAN));
        }

        return authorities;
    }
}
