# Swagger UI Erişim Rehberi

## ✅ Swagger UI URL'leri

Spring Boot 3 + SpringDoc ile:

### 1. Ana Swagger UI (Önerilen)
```
http://localhost:8081/swagger-ui/index.html
```

### 2. Alternatif URL'ler
```
http://localhost:8081/swagger-ui.html
http://localhost:8081/swagger-ui/
```

### 3. API Docs JSON
```
http://localhost:8081/api/api-docs
```

## 🔒 403 Hatası Alıyorsanız

SecurityConfig'de Swagger path'leri `permitAll()` ile işaretlenmiş olmalı.

Eğer hala 403 alıyorsanız:
```bash
cd backend
docker-compose restart app
```

## 📝 Not

Context path `/api` olduğu için Swagger UI context path'siz çalışır, ama API endpoint'leri `/api` prefix'i ile çalışır.

**Örnek:**
- Swagger UI: `http://localhost:8081/swagger-ui/index.html` ✅
- API Login: `http://localhost:8081/api/auth/login` ✅

