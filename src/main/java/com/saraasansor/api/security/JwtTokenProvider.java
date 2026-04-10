package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenProvider {

    private static final String USER_ID = "userId";
    private static final String ROLE = "role";
    private static final String USER_TYPE = "userType";
    private static final String B2BUNIT_ID = "b2bUnitId";
    private static final String AUTH_SCOPE_TYPE = "authScopeType";
    private static final String TENANT_ID = "tenantId";
    private static final String TENANT_SCHEMA = "tenantSchema";
    @Value("${app.jwt.secret}")
    private String secret;
    
    @Value("${app.jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateAccessToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, userId);
        claims.put(ROLE, role);
        return createToken(claims, username, accessTokenExpiration);
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, user.getId());
        claims.put(ROLE, user.getCanonicalRole() != null ? user.getCanonicalRole().name() : null);
        claims.put(USER_TYPE, user.getUserType() != null ? user.getUserType().name() : null);
        claims.put(B2BUNIT_ID, user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);

        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant != null) {
            claims.put(AUTH_SCOPE_TYPE, "TENANT");
            claims.put(TENANT_ID, tenant.getId());
            claims.put(TENANT_SCHEMA, tenant.getSchemaName());
        } else {
            claims.put(AUTH_SCOPE_TYPE, "PLATFORM");
            claims.put(TENANT_ID, null);
            claims.put(TENANT_SCHEMA, null);
        }

        return createToken(claims, user.getUsername(), accessTokenExpiration);
    }
    
    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshTokenExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    public Long getUserIdFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object userId = claims.get(USER_ID);
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            return (Long) userId;
        });
    }
    
    public String getRoleFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(ROLE));
    }

    public String getUserTypeFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(USER_TYPE));
    }

    public String getAuthScopeTypeFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(AUTH_SCOPE_TYPE));
    }

    public Long getTenantIdFromToken(String token) {
        return getClaimFromToken(token, claims -> {
            Object tenantId = claims.get(TENANT_ID);
            if (tenantId == null) {
                return null;
            }
            if (tenantId instanceof Integer) {
                return ((Integer) tenantId).longValue();
            }
            return (Long) tenantId;
        });
    }

    public String getTenantSchemaFromToken(String token) {
        return getClaimFromToken(token, claims -> (String) claims.get(TENANT_SCHEMA));
    }
    
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    public Boolean validateToken(String token, String username) {
        final String tokenUsername = getUsernameFromToken(token);
        return (tokenUsername.equals(username) && !isTokenExpired(token));
    }
}
