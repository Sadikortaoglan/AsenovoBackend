# Frontend 403 Error Fix - CORS Configuration

## Problem
Frontend uygulamasından tüm API endpoint'lerine istek atıldığında **403 Forbidden - "Full authentication is required"** hatası alınıyor. Ancak Postman'den aynı istekler başarılı.

## Root Cause
CORS (Cross-Origin Resource Sharing) konfigürasyonu eksik veya yanlış. Frontend'den gelen request'lerde:
- Preflight OPTIONS request'leri başarısız oluyor
- Authorization header'ı düzgün işlenmiyor
- Origin header'ı doğru handle edilmiyor

## Solution Applied

### 1. SecurityConfig.java - CORS Configuration Updated

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:5173",  // Vite default
        "http://localhost:3000",  // React default
        "http://localhost:5174",  // Vite alternative
        "http://127.0.0.1:5173",
        "http://127.0.0.1:3000",
        "http://localhost:8080"   // Desktop app
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization", 
        "Content-Type", 
        "Accept", 
        "X-Requested-With", 
        "Origin", 
        "Access-Control-Request-Method", 
        "Access-Control-Request-Headers"
    ));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### Changes Made:
1. ✅ **AllowedHeaders**: `["*"]` yerine spesifik header'lar listelendi (Authorization, Content-Type, etc.)
2. ✅ **ExposedHeaders**: Response header'ları eklendi
3. ✅ **AllowedOrigins**: Desktop app için localhost:8080 eklendi

## Frontend Request Format

### Correct Request Headers

**GET Request:**
```
GET http://localhost:8081/api/elevators
Origin: http://localhost:5173
Accept: application/json
Authorization: Bearer <token>
Content-Type: application/json (optional for GET)
```

**POST Request:**
```
POST http://localhost:8081/api/maintenances
Origin: http://localhost:5173
Content-Type: application/json
Accept: application/json
Authorization: Bearer <token>
```

### Preflight OPTIONS Request

Browser automatically sends OPTIONS request before actual request:

```
OPTIONS http://localhost:8081/api/elevators
Origin: http://localhost:5173
Access-Control-Request-Method: GET
Access-Control-Request-Headers: Authorization,Content-Type
```

**Expected Response:**
```
HTTP/1.1 200 OK
Access-Control-Allow-Origin: http://localhost:5173
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, Accept, X-Requested-With, Origin, Access-Control-Request-Method, Access-Control-Request-Headers
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

## Testing

### Test OPTIONS (Preflight)
```bash
curl -v -X OPTIONS http://localhost:8081/api/elevators \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization,Content-Type"
```

Expected: HTTP 200 with CORS headers

### Test GET with Token
```bash
TOKEN="your_token_here"
curl -v http://localhost:8081/api/elevators \
  -H "Origin: http://localhost:5173" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer $TOKEN"
```

Expected: HTTP 200 with data

## Common Frontend Issues

### Issue 1: Missing Authorization Header
**Error**: 403 Forbidden  
**Fix**: Ensure Authorization header is sent:
```javascript
headers: {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json',
  'Accept': 'application/json'
}
```

### Issue 2: CORS Preflight Fails
**Error**: CORS policy error in browser console  
**Fix**: Backend now handles OPTIONS requests correctly

### Issue 3: Credentials Not Sent
**Error**: Cookie/token not sent  
**Fix**: Ensure `credentials: 'include'` in fetch or `withCredentials: true` in axios

## Frontend Code Examples

### Fetch API
```javascript
const token = localStorage.getItem('token');

fetch('http://localhost:8081/api/elevators', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Accept': 'application/json',
    'Content-Type': 'application/json'
  },
  credentials: 'include'
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

### Axios
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  withCredentials: true
});

// Add token to all requests
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Use it
api.get('/elevators')
  .then(response => console.log(response.data))
  .catch(error => console.error('Error:', error));
```

### Java HttpClient (Desktop App)
```java
HttpClient client = HttpClient.newHttpClient();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8081/api/elevators"))
    .header("Accept", "application/json")
    .header("Authorization", "Bearer " + token)
    .header("Content-Type", "application/json")
    .GET()
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());
```

## Verification Checklist

- [ ] OPTIONS preflight returns 200
- [ ] CORS headers present in OPTIONS response
- [ ] GET/POST requests include Authorization header
- [ ] Origin header matches allowed origins
- [ ] Token is valid and not expired
- [ ] Content-Type header is `application/json` for POST/PUT
- [ ] Accept header is `application/json`

## Restart Backend

After making changes to SecurityConfig:
```bash
cd backend
pkill -f "spring-boot:run"
mvn spring-boot:run
```

## Status

✅ CORS configuration updated  
✅ Authorization header allowed  
✅ OPTIONS requests handled  
✅ Credentials enabled  
✅ Frontend origins whitelisted

