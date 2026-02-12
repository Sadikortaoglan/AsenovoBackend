# Swagger UI Usage Guide

## 🔗 Swagger UI Access

**URL**: `http://localhost:8081/swagger-ui/index.html`

## 🔐 Authentication (IMPORTANT!)

Swagger'dan API endpoint'lerini Test etmek için **JWT Token** eklemeniz gerekiyor:

### Adım 1: Login ile Token Get

1. Swagger UI'da **Authentication > POST /auth/login** endpoint'ini Find
2. "Try it out" butonuna Click
3. Request body'yi Fill:
```json
{
  "username": "patron",
  "password": "password"
}
```
4. "Execute" butonuna Click
5. Response'dan `accessToken` değerini Copy

### Adım 2: Token'ı Swagger'a Add

1. Swagger UI sayfasının **sağ üst köşesinde** 🔒 **"Authorize"** butonunu Find
2. "Authorize" butonuna Click
3. Opened popup'ta **"bearerAuth"** bölümüne Token'ı Paste
4. Token formatı: Sadece Token değerini Paste (Bearer yazmayın)
   - Örnek: `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUEFUUk9OIiwidXNlcklkIjoxLCJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgwNzYzMzksImV4cCI6MTc2ODA3OTkzOX0...`
5. "Authorize" butonuna Click
6. "Close" ile popup'ı Close

### Adım 3: Artık Tüm Endpoint'leri Test You can

Token eklendikten sonra tüm authenticated endpoint'ler Will work.

## ⚠️ IMPORTANT Notes

- Token Duration **1 saat**dir
- Token Duration When expired yeni bir Token You need to get gerekecek
- Token'ı Refresh için `/auth/refresh` endpoint'ini You can use

## 📝 Test Scenario

1. **Login** → Token al
2. **Authorize** → Token'ı ekle
3. **GET /elevators** → Asansörleri listele
4. **GET /dashboard/summary** → Dashboard özeti al
5. **POST /maintenances** → Yeni Maintenance kaydı oluştur

## 🐛 403 Hatası If you get

1. Token'ın ekli olduğundan emin olun (sağ üstte 🔒 ikonu görünmeli)
2. Token'ın geçerli olduğundan emin olun (1 saatten eski değil)
3. Login yapıp yeni Token Get
4. Yeni Token'ı Authorize'a Add

---

**Not**: Dashboard'dan Swagger'ı açtığınızda, Token Automatic olarak eklenecek şekilde yapılandırılabilir (frontend geliştirme aşamasında).

