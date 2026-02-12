# CORS Setup Guide

## Environment Variable: `CORS_ALLOWED_ORIGINS`

`CORS_ALLOWED_ORIGINS` environment variable Docker Compose tarafından okunur ve Spring Boot uygulamasına aktarılır.

## Nereye Konulacak?

### Production (Docker Compose)

**1. `.env` dosyası oluştur** (backend klasöründe):

```bash
cd /path/to/backend
nano .env
```

**2. `.env` dosyasına ekle:**

```bash
# Database
POSTGRES_DB=sara_asansor
POSTGRES_USER=sara_asansor
POSTGRES_PASSWORD=your_password

# JWT
JWT_SECRET=your-256-bit-secret-key

# CORS - Production origins (comma-separated)
CORS_ALLOWED_ORIGINS=http://51.21.3.85,http://51.21.3.85:80
```

**3. Docker Compose çalıştır:**

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

Docker Compose otomatik olarak `.env` dosyasını okur ve `${CORS_ALLOWED_ORIGINS}` değerini container'a aktarır.

### Alternative: Direkt docker-compose.prod.yml

`.env` dosyası yerine direkt `docker-compose.prod.yml`'de de tanımlanabilir:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  CORS_ALLOWED_ORIGINS: "http://51.21.3.85,http://51.21.3.85:80"
  # ... diğer environment variables
```

**NOT:** Bu yöntem güvenlik açısından önerilmez (şifreler dosyada görünür).

### Development (Local)

Local development için `.env` dosyasına ekle:

```bash
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

Veya environment variable olarak:

```bash
export CORS_ALLOWED_ORIGINS=http://localhost:5173
mvn spring-boot:run
```

## Format

- **Comma-separated**: Birden fazla origin için virgülle ayırın
- **Pattern support**: `http://51.21.3.85:*` şeklinde wildcard kullanılabilir
- **Examples**:
  ```
  # Single origin
  CORS_ALLOWED_ORIGINS=http://51.21.3.85
  
  # Multiple origins
  CORS_ALLOWED_ORIGINS=http://51.21.3.85,http://51.21.3.85:80,https://example.com
  
  # With wildcard
  CORS_ALLOWED_ORIGINS=http://51.21.3.85:*
  ```

## Backend Kodu

Backend'de `SecurityConfig.java` içinde şu şekilde okunuyor:

```java
String corsOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS");
if (corsOrigins != null && !corsOrigins.trim().isEmpty()) {
    String[] origins = corsOrigins.split(",");
    for (String origin : origins) {
        String trimmed = origin.trim();
        if (!trimmed.isEmpty()) {
            allowedOriginPatterns.add(trimmed);
        }
    }
}
```

## Test

Environment variable'ın doğru yüklendiğini kontrol et:

```bash
# Docker container içinde
docker exec -it sara-asansor-api-prod env | grep CORS

# Çıktı:
# CORS_ALLOWED_ORIGINS=http://51.21.3.85,http://51.21.3.85:80
```

## Önemli Notlar

1. ✅ `.env` dosyasını `.gitignore`'a ekle (şifreler içerir)
2. ✅ Production'da `.env` dosyasını güvenli bir şekilde deploy et
3. ✅ `.env.example` dosyası oluştur (template olarak)
4. ✅ Docker Compose otomatik olarak `.env` dosyasını okur

## Docker Compose .env Dosyası Okuma

Docker Compose, `docker-compose.prod.yml` dosyasıyla aynı dizinde bulunan `.env` dosyasını otomatik okur.

**Örnek yapı:**
```
backend/
├── .env                    ← Buraya koy
├── docker-compose.prod.yml ← Bu dosya .env'i okur
└── ...
```

**Deployment:**
```bash
# 1. .env dosyasını oluştur
nano .env

# 2. CORS_ALLOWED_ORIGINS ekle
CORS_ALLOWED_ORIGINS=http://51.21.3.85,http://51.21.3.85:80

# 3. Deploy
docker-compose -f docker-compose.prod.yml up -d --build
```
