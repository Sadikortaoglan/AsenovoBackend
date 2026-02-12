# Postman Collection Kurulum Guide

## 📦 Dosyalar

Bu klasörde iki Postman dosyası bulunmaktadır:

1. **SaraAsansor_API.postman_collection.json** - Tüm API endpoint'lerini içeren collection
2. **SaraAsansor_API.postman_environment.json** - Environment değişkenleri (baseUrl, tokens, vs.)

## 🚀 Kurulum Adımları

### 1. Postman'de Collection'ı İçe Aktar

1. Postman'i açın
2. **File > Import** menüsüne gidin
3. **SaraAsansor_API.postman_collection.json** dosyasını seçin
4. **Import** butonuna Click

### 2. Environment'ı İçe Aktar

1. Postman'de **Environments** sekmesine gidin (sağ üst köşe)
2. **Import** butonuna Click
3. **SaraAsansor_API.postman_environment.json** dosyasını seçin
4. Environment'ı seçili hale getirin (dropdown'dan)

### 3. İlk Test - Login

1. Collection'da **Authentication > Login** request'ini açın
2. **Send** butonuna Click
3. Başarılı olursa, `accessToken` ve `refreshToken` Automatic olarak environment'a kaydedilir
4. Artık diğer request'ler Will work (Token Automatic eklenir)

## 📋 Environment Değişkenleri

| Değişken | Description | Örnek Değer |
|----------|----------|-------------|
| `baseUrl` | API base URL | `http://localhost:8081/api` |
| `username` | User adı | `patron` |
| `password` | Şifre | `password` |
| `accessToken` | JWT Access Token (Automatic) | (Login sonrası doldurulur) |
| `refreshToken` | JWT Refresh Token (Automatic) | (Login sonrası doldurulur) |
| `elevatorId` | Test için Elevator ID | `1` |
| `maintenanceId` | Test için Maintenance ID | `1` |
| `partId` | Test için Part ID | `1` |

## 🔐 Swagger'da Authorization

Swagger UI'da endpoint'leri Test etmek için:

1. **Login yapın**: `POST /auth/login` endpoint'ini kullanarak Token Get
2. **Authorize butonuna Click**: Swagger UI sayfasının sağ üst köşesindeki 🔒 butonuna Click
3. **Token'ı Add**: `bearerAuth` alanına sadece Token değerini Paste (Bearer yazmayın)
4. **Artık Test You can**: Tüm endpoint'ler Will work

> ⚠️ **403 Hatası**: Token eklenmemişse veya Duration dolmuşsa 403 hatası alırsınız.

## 🧪 Test Senaryoları

### Senaryo 1: Tam Akış Testi

1. **Login** - Token al
2. **Get All Elevators** - Asansörleri listele
3. **Create Elevator** - Yeni Elevator ekle
4. **Get Elevator by ID** - Added asansörü Control et
5. **Create Maintenance** - Bu Elevator için Maintenance kaydı oluştur
6. **Get Dashboard Summary** - Summary istatistikleri görüntüle

### Senaryo 2: Warning System Testi

1. **Login** - Token al
2. **Get Expired Elevators** - Duration dolmuş asansörleri listele
3. **Get Warning Elevators** - Yakında dolacak asansörleri listele
4. **Get Elevator Status** - Belirli bir asansörün durumunu Control et

### Senaryo 3: Maintenance ve Payment Testi

1. **Login** - Token al
2. **Create Maintenance** - Maintenance kaydı oluştur
3. **Get Maintenances by Elevator ID** - Asansöre ait bakımları listele
4. **Mark Maintenance as Paid** - Payment olarak işaretle
5. **Get Dashboard Summary** - Gelir/borç istatistiklerini Control et

## 🔧 Özelleştirme

### Farklı Bir Port Kullanıyorsanız

Environment'da `baseUrl` değerini güncelleyin:
- Local: `http://localhost:8081/api`
- Development: `http://dev.example.com/api`
- Production: `https://api.example.com/api`

### Farklı User ile Test

1. Environment'da `username` ve `password` değerlerini güncelleyin
2. **Login** request'ini tekrar çalıştırın

### Token Yenileme

Eğer `accessToken` Duration dolduysa:
1. **Authentication > Refresh Token** request'ini çalıştırın
2. Yeni Token Automatic olarak environment'a kaydedilir

## 📝 Notes

- Tüm authenticated endpoint'ler `Authorization: Bearer {{accessToken}}` header'ı kullanır
- Login sonrası Token'lar Automatic olarak environment'a kaydedilir
- Environment değişkenlerini request'lerde `{{variableName}}` şeklinde You can use
- Collection'da her request için Description (description) mevcuttur

## 🐛 Sorun Giderme

### Token Hatası If you get

1. **Login** request'ini tekrar çalıştırın
2. Environment'da `accessToken` değerinin dolu olduğundan emin olun

### Connection Refused Hatası

1. Backend'in çalıştığından emin olun: `docker-compose ps`
2. Environment'daki `baseUrl` değerini Control edin
3. Port'un açık olduğunu Control edin: `curl http://localhost:8081/api/auth/login`

### 401 Unauthorized Hatası

1. Token'ın geçerli olduğundan emin olun
2. **Login** request'ini tekrar çalıştırın
3. User adı/şifrenin doğru olduğunu Control edin

## ✅ Test Control Listesi

- [ ] Collection ve Environment içe aktarıldı
- [ ] Environment seçili
- [ ] Login başarılı ve Token alındı
- [ ] Get All Elevators çalışıyor
- [ ] Create Elevator çalışıyor
- [ ] Dashboard Summary çalışıyor
- [ ] Warnings endpoint'i çalışıyor

---

**Hazırlayan**: Sara Elevator Backend Team  
**Date**: 2024

