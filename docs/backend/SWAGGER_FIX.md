# Swagger Erişim Bilgisi

## Swagger UI URL

Spring Boot 3 + SpringDoc ile context path `/api` kullanıldığında:

**Swagger UI**: `http://localhost:8081/api/swagger-ui/index.html`

Veya direkt:
- `http://localhost:8081/swagger-ui/index.html` (context path olmadan)

**API Docs JSON**: `http://localhost:8081/api/api-docs`

## Erişim Sorunu Varsa

Eğer 403 hatası alıyorsanız:
1. SecurityConfig.java'da Swagger path'leri permitAll() ile işaretlenmiş olmalı
2. Container'ı yeniden başlatın: `docker-compose restart app`

## Test

```bash
# Swagger UI
curl http://localhost:8081/swagger-ui/index.html

# API Docs
curl http://localhost:8081/api/api-docs
```

