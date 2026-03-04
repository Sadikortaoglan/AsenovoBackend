package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
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

    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getActive()) && Boolean.TRUE.equals(user.getEnabled()),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !Boolean.TRUE.equals(user.getLocked()),
                getAuthorities(user.getRole())
        );
    }
    
    private Collection<? extends GrantedAuthority> getAuthorities(User.Role role) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));

        if (role == User.Role.SYSTEM_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(ROLE_STAFF_ADMIN));
            authorities.add(new SimpleGrantedAuthority(ROLE_STAFF_USER));
            authorities.add(new SimpleGrantedAuthority(ROLE_CARI_USER));
        } else if (role == User.Role.STAFF_ADMIN) {
            authorities.add(new SimpleGrantedAuthority(ROLE_STAFF_USER));
            authorities.add(new SimpleGrantedAuthority(ROLE_CARI_USER));
        } else if (role == User.Role.STAFF_USER) {
            authorities.add(new SimpleGrantedAuthority(ROLE_CARI_USER));
        }

        return authorities;
    }
}
