# JSON Ignore Unknown Properties Fix

## Sorun

Frontend'den gönderilen bazı field'lar (örneğin `description` Part model'inde) backend'de yok.
Jackson JSON parse error veriyor: `"Unrecognized field \"description\""`

## Çözüm

### 1. Global Jackson Configuration ✅
`JacksonConfig.java` oluşturuldu:
- `FAIL_ON_UNKNOWN_PROPERTIES = false` - Bilinmeyen field'ları ignore et
- Bu tüm model'ler için geçerli

### 2. Frontend Service Düzeltmesi ✅
`part.service.ts`:
- `description` field'ı `mapPartToBackend()` fonksiyonundan kaldırıldı
- Backend'de olmayan field'lar gönderilmiyor

## Sonuç

- ✅ Jackson global olarak unknown properties'leri ignore ediyor
- ✅ Frontend gereksiz field'ları göndermiyor
- ✅ Tüm endpoint'ler çalışmalı

## Not

Backend servisini restart etmek gerekiyor - JacksonConfig değişikliği için.

