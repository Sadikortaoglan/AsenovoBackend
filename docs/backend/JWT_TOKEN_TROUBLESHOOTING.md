# JWT Token Sorun Giderme Guide

## 🔍 Sorun Analizi

Postman'de JWT Token başarıyla dönüyor ancak Desktop/Web uygulamasından Token alınamıyor.

## ✅ Backend Kontrolleri

### 1. AuthController /auth/login Endpoint

**Endpoint**: `POST /api/auth/login`

**Beklenen Request Body Format**:
```json
{
  "username": "patron",
  "password": "password"
}
```

**Beklenen Headers**:
- `Content-Type: application/json` (ZORUNLU)

**Response JSON Structure**:
```json
{
  "success": true,
  "message": "Login başarılı",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  },
  "errors": null
}
```

**HTTP Status Codes**:
- `200 OK`: Başarılı Login
- `400 Bad Request`: Validasyon hatası veya Identity Validation hatası

**Validation Rules**:
- `username`: NotBlank (boş olamaz)
- `password`: NotBlank (boş olamaz)

---

### 2. CORS Configuration ✅

**SecurityConfig.java**:
```java
configuration.setAllowedOrigins(List.of("*"));  // Tüm origin'lere izin verilmiş
configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));  // Tüm header'lara izin verilmiş
configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
```

**✅ Validation**:
- ✅ Tüm origin'lere izin verilmiş (`*`)
- ✅ Authorization header exposed
- ✅ Content-Type header allowed
- ✅ POST ve OPTIONS metodları allowed

**CORS Preflight (OPTIONS) İsteği**:
- Desktop/Web uygulaması POST isteğinden önce OPTIONS isteği göndermeli
- Backend OPTIONS isteğine doğru CORS header'ları ile yanıt veriyor

---

### 3. Content-Type Handling ✅

**Gereksinim**: `application/json`

**AuthController**:
```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request)
```

- `@RequestBody` annotation'ı JSON formatında body bekliyor
- `Content-Type: application/json` header'ı ZORUNLU

---

### 4. SecurityConfig ✅

**CSRF**: ✅ Disabled
```java
.csrf(csrf -> csrf.disable())
```

**Auth Endpoint İzni**: ✅ PermitAll
```java
.requestMatchers("/auth/**").permitAll()
```

**Session Management**: ✅ Stateless
```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

**✅ Validation**:
- ✅ CSRF devre dışı (REST API için uygun)
- ✅ `/auth/**` endpoint'leri authentication gerektirmiyor
- ✅ Stateless session (JWT için uygun)

---

### 5. Error Handling

**GlobalExceptionHandler** Existing ve şu durumları handle ediyor:

1. **Validation Hataları** (`400 Bad Request`):
```json
{
  "success": false,
  "message": "Validasyon hatası",
  "data": null,
  "errors": {
    "username": "User adı boş olamaz",
    "password": "Şifre boş olamaz"
  }
}
```

2. **Authentication Hataları** (`400 Bad Request`):
```json
{
  "success": false,
  "message": "Login başarısız: Bad credentials",
  "data": null,
  "errors": null
}
```

3. **Runtime Exceptions** (`400 Bad Request`):
```json
{
  "success": false,
  "message": "Error mesajı",
  "data": null,
  "errors": null
}
```

---

## 🖥️ Desktop/Web Uygulaması Gereksinimleri

### Request Format

**URL**: `http://localhost:8081/api/auth/login`

**HTTP Method**: `POST`

**Headers**:
```
Content-Type: application/json
```

**Body** (JSON):
```json
{
  "username": "patron",
  "password": "password"
}
```

### Örnek İstekler

#### JavaScript/Fetch API:
```javascript
fetch('http://localhost:8081/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: 'patron',
    password: 'password'
  })
})
.then(response => response.json())
.then(data => {
  if (data.success) {
    const Token = data.data.accessToken;
    // Token'ı sakla
  } else {
    console.error('Login failed:', data.message);
  }
})
.catch(error => {
  console.error('Error:', error);
});
```

#### Java HttpClient:
```java
HttpClient client = HttpClient.newHttpClient();
String requestBody = "{"
    + "\"username\":\"patron\","
    + "\"password\":\"password\""
    + "}";

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:8081/api/auth/login"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    .build();

HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());

if (response.statusCode() == 200) {
    // Parse JSON response
    // Extract data.accessToken
}
```

#### cURL (Referans):
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"patron","password":"password"}'
```

### Response Parsing

**Başarılı Response**:
```json
{
  "success": true,
  "message": "Login başarılı",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "patron",
    "role": "PATRON"
  },
  "errors": null
}
```

**Token'ı Alma**:
- `response.data.accessToken` (JavaScript)
- `response.getData().getAccessToken()` (Java)
- `response["data"]["accessToken"]` (Python)

---

## 🔧 Yaygın Sorunlar ve Çözümleri

### Sorun 1: CORS Hatası

**Belirtiler**:
- Browser console'da CORS hatası
- `Access-Control-Allow-Origin` header'ı yok

**Çözüm**: 
✅ CORS zaten `*` ile açık. Eğer hala sorun varsa:
- Browser cache'i temizleyin
- OPTIONS preflight isteğinin başarılı olduğundan emin olun

### Sorun 2: Content-Type Hatası

**Belirtiler**:
- `415 Unsupported Media Type`
- `Content-Type` header'ı eksik veya yanlış

**Çözüm**:
- Header'ı şu şekilde ayarlayın: `Content-Type: application/json`
- Body'yi JSON formatında gönderin

### Sorun 3: Validation Hatası

**Belirtiler**:
- `400 Bad Request`
- Response'da `errors` objesi var

**Çözüm**:
- `username` ve `password` alanlarının dolu olduğundan emin olun
- Boş string göndermeyin

### Sorun 4: Authentication Hatası

**Belirtiler**:
- `400 Bad Request`
- Message: "Login başarısız: Bad credentials"

**Çözüm**:
- User adı ve şifrenin doğru olduğundan emin olun
- Varsayılan: `username: "patron"`, `password: "password"`

### Sorun 5: Network Hatası

**Belirtiler**:
- Request hiç gitmiyor
- Connection refused

**Çözüm**:
- Backend'in çalıştığından emin olun: `http://localhost:8081/api/auth/login`
- Port'un doğru olduğundan emin olun (8081)
- Context path'in doğru olduğundan emin olun (`/api`)

### Sorun 6: Response Parse Hatası

**Belirtiler**:
- Request başarılı ama Token parse edilemiyor

**Çözüm**:
- Response JSON formatını Control edin
- `data.success` kontrolü yapın
- `data.data.accessToken` yolunu doğru kullanın

---

## 📋 Test Checklist

Desktop/Web uygulamanızın login isteğini Test etmek için:

- [ ] **URL**: `http://localhost:8081/api/auth/login` (context path `/api` dahil)
- [ ] **Method**: `POST`
- [ ] **Header**: `Content-Type: application/json`
- [ ] **Body**: Valid JSON (`{"username":"...","password":"..."}`)
- [ ] **OPTIONS Preflight**: Browser Automatic göndermeli
- [ ] **Response Status**: `200 OK`
- [ ] **Response Body**: JSON parse edilebilir
- [ ] **Success Check**: `response.success === true`
- [ ] **Token Extraction**: `response.data.accessToken` Existing ve null değil

---

## 🔍 Debug Adımları

### 1. Backend Loglarını Control

```bash
cd backend
docker-compose logs app | grep -i "login\|auth\|cors\|error"
```

**Başarılı Login**:
```
Securing POST /auth/login
Secured POST /auth/login
Authenticated user
```

**Error Durumu**:
- CORS hatası: `Access-Control-Allow-Origin` hatası
- Validation hatası: `MethodArgumentNotValidException`
- Auth hatası: `BadCredentialsException`

### 2. Network Tab'ını Control (Browser)

**Request Headers**:
```
POST /api/auth/login HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Origin: http://localhost:3000 (veya uygulama URL'i)
```

**Response Headers**:
```
HTTP/1.1 200 OK
Content-Type: application/json
Access-Control-Allow-Origin: *
```

### 3. Response Body'yi Control

**Başarılı**:
- `success: true`
- `data.accessToken` Existing
- `data.refreshToken` Existing

**Error**:
- `success: false`
- `message` Error açıklaması içeriyor
- `errors` validation hataları içerebilir

---

## ✅ Summary: Desktop/Web App Gereksinimleri

### Mutlaka Gerekenler:

1. **URL**: `http://localhost:8081/api/auth/login`
2. **Method**: `POST`
3. **Header**: `Content-Type: application/json`
4. **Body**: `{"username":"patron","password":"password"}` (JSON formatında)
5. **Response Parse**: `response.data.accessToken` yolunu kullan

### İsteğe Bağlı (Browser için):

- OPTIONS preflight isteği (browser Automatic gönderir)
- Origin header (CORS için)

### Response Handling:

```javascript
// JavaScript örneği
const response = await fetch(url, options);
const data = await response.json();

if (data.success && data.data && data.data.accessToken) {
  const Token = data.data.accessToken;
  // Token'ı sakla (localStorage, sessionStorage, vb.)
} else {
  // Error durumunu handle et
  console.error('Login failed:', data.message, data.errors);
}
```

---

## 📞 Ek Notes

- **Context Path**: Tüm endpoint'ler `/api` ile başlar
- **Port**: Backend `8080` portunda çalışıyor, Docker ile `8081`'e map edilmiş
- **Token Duration**: Access Token 1 saat, Refresh Token 7 gün
- **CORS**: Tüm origin'lere açık (`*`), production'da kısıtlanmalı

---

## ⚠️ IMPORTANT Not: JWT Filter Güncellemesi

**JwtAuthenticationFilter** güncellendi:
- OPTIONS istekleri (CORS preflight) artık JWT validation'ından geçmiyor
- `/auth/**` endpoint'lerinde Token yoksa Error vermiyor
- Invalid Token durumunda Exception handle ediliyor

Bu sayede Desktop/Web uygulamalarından gelen login istekleri sorunsuz çalışmalı.

---

**Son Güncelleme**: 2026-01-10

