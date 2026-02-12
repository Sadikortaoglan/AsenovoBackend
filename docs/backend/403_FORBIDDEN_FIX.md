# 403 Forbidden Fix - Frontend Access Issue Resolution

## Problem
Frontend requests from `localhost:5173` or `localhost:3000` were receiving 403 Forbidden errors while Postman requests worked correctly.

## Root Cause
The CORS configuration was using a wildcard (`*`) for allowed origins, which doesn't work properly with credentials in modern browsers. Additionally, credentials were not explicitly enabled.

## Solution Applied

### 1. CORS Configuration Fixed (`SecurityConfig.java`)

**Before:**
```java
configuration.setAllowedOrigins(List.of("*"));  // Doesn't work with credentials
```

**After:**
```java
configuration.setAllowedOrigins(Arrays.asList(
    "http://localhost:5173",  // Vite default
    "http://localhost:3000",  // React default
    "http://localhost:5174",  // Vite alternative
    "http://127.0.0.1:5173",
    "http://127.0.0.1:3000"
));
configuration.setAllowCredentials(true);  // Explicitly enable credentials
configuration.setMaxAge(3600L);  // Cache preflight for 1 hour
```

### 2. JWT Filter
- Kept simple and clean
- Properly extracts Bearer token from Authorization header
- Sets authentication in SecurityContext correctly

### 3. Role Mapping Verification
✅ **Verified Correct:**
- `CustomUserDetailsService` returns `"ROLE_PATRON"` or `"ROLE_PERSONEL"`
- `SecurityConfig.hasRole("PATRON")` automatically prefixes with `"ROLE_"`
- Role mapping is **CORRECT** and works as expected

## Endpoint Security Configuration

All endpoints are properly configured:

| Endpoint Pattern | Security | Access |
|-----------------|----------|--------|
| `/auth/**` | `permitAll()` | Public (no auth required) |
| `/swagger-ui/**`, `/api-docs/**` | `permitAll()` | Public (no auth required) |
| `/users/**` | `hasRole("PATRON")` | PATRON role only |
| All other endpoints (`/elevators`, `/maintenances`, `/parts`, `/faults`, `/inspections`, `/payments`, `/warnings`, `/dashboard`) | `authenticated()` | Any authenticated user |

## Testing Checklist

✅ CORS preflight (OPTIONS) requests work
✅ Authorization header is accepted
✅ JWT tokens are validated correctly
✅ Role-based access works (PATRON for /users, authenticated for others)
✅ All endpoints are accessible with valid JWT token
✅ Frontend can send requests from `localhost:5173` or `localhost:3000`

## Frontend Requirements

The frontend must send requests with:

1. **Authorization Header**: `Authorization: Bearer <accessToken>`
2. **Content-Type**: `Content-Type: application/json` (for POST/PUT/PATCH requests)
3. **Origin**: Automatically set by browser (must match allowed origins)

### Example Request
```javascript
fetch('http://localhost:8080/api/elevators', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
})
```

## Token Acquisition

1. Login: `POST /api/auth/login`
   ```json
   {
     "username": "patron",
     "password": "password"
   }
   ```

2. Response contains `accessToken`:
   ```json
   {
     "success": true,
     "data": {
       "accessToken": "eyJhbGci...",
       "refreshToken": "eyJhbGci...",
       "role": "PATRON"
     }
   }
   ```

3. Use `accessToken` in Authorization header for all authenticated requests

## Verification

All endpoints are now configured to:
1. ✅ Accept CORS requests from frontend development origins
2. ✅ Process JWT tokens correctly from Authorization header
3. ✅ Apply role-based authorization properly
4. ✅ Handle preflight OPTIONS requests
5. ✅ Support all HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
6. ✅ Work consistently for both Postman and frontend requests

## Notes

- The CORS configuration allows credentials, which is necessary for proper authentication
- Specific origins are required when `allowCredentials(true)` is set (cannot use wildcard `*`)
- All common frontend development ports are included
- Preflight requests are cached for 1 hour to improve performance

