# Swagger 403 Hatası Çözümü

## ❌ Sorun

Swagger UI'dan endpoint'lere istek yaparken **403 Forbidden** hatası alıyorsunuz.

## ✅ Çözüm

**Swagger'da JWT token'ı eklemeniz gerekiyor!**

### Adım Adım Çözüm

#### 1. Swagger UI'ı Açın
```
http://localhost:8081/swagger-ui/index.html
```
veya
```
http://localhost:8081/swagger-ui.html
```

#### 2. Login Yaparak Token Alın

1. Swagger UI'da **`POST /auth/login`** endpoint'ini bulun
2. **"Try it out"** butonuna tıklayın
3. Request body'ye şunları yazın:
```json
{
  "username": "patron",
  "password": "password"
}
```
4. **"Execute"** butonuna tıklayın
5. Response'dan **`accessToken`** değerini kopyalayın

#### 3. Token'ı Swagger'a Ekleyin (ÖNEMLİ!)

1. Swagger UI sayfasının **sağ üst köşesinde** 🔒 **"Authorize"** butonunu bulun
2. **"Authorize"** butonuna tıklayın
3. Açılan popup'ta **"bearerAuth"** bölümüne:
   - **Sadece token değerini** yapıştırın (Bearer yazmayın!)
   - Örnek: `eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiUEFUUk9OIiwidXNlcklkIjoxLCJzdWIiOiJwYXRyb24iLCJpYXQiOjE3NjgwNzYzMzksImV4cCI6MTc2ODA3OTkzOX0...`
4. **"Authorize"** butonuna tıklayın
5. **"Close"** ile popup'ı kapatın

#### 4. Artık Test Edebilirsiniz!

Token eklendikten sonra tüm endpoint'ler çalışacaktır. 🔒 ikonu görünmelidir.

## 📝 Örnek Test

1. ✅ Login → Token al
2. ✅ Authorize → Token ekle
3. ✅ GET /elevators → Çalışır (403 hatası yok)
4. ✅ GET /maintenances → Çalışır (403 hatası yok)
5. ✅ GET /dashboard/summary → Çalışır (403 hatası yok)

## ⚠️ Token Süresi Dolduğunda

Token **1 saat** geçerlidir. Süre dolduğunda:
1. Yeni login yapın
2. Yeni token'ı alın
3. Authorize'a yeni token'ı ekleyin

## 🔍 Swagger UI'da Token Kontrolü

- Token eklendiyse: Sağ üstte 🔒 **"Authorize"** butonunda sayı görünür
- Token eklenmediyse: 403 hatası alırsınız

---

**ÖNEMLİ**: Swagger'dan endpoint test etmek için MUTLAKA token eklemelisiniz!

