# JSON Parse Error Fix - Unknown Properties

## Sorun

Frontend'den gönderilen bazı field'lar (örneğin Part'ta `description`) backend model'inde yok.
Jackson JSON parse error veriyor: `"Unrecognized field \"description\""`

Postman'de çalışıyor çünkü gereksiz field'lar gönderilmiyor.

## Çözümler

### 1. Backend - JacksonConfig ✅
`JacksonConfig.java` güncellendi:
```java
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
```

Bu ayar tüm model'ler için geçerli - bilinmeyen field'lar ignore edilir.

### 2. Backend - Part Model ✅
`Part.java`:
- `@JsonIgnoreProperties(ignoreUnknown = true)` eklendi (ekstra güvenlik)

### 3. Frontend - Part Service ✅
`part.service.ts`:
- `mapPartToBackend()` fonksiyonundan `description` field'ı kaldırıldı
- Backend'de olmayan field'lar artık gönderilmiyor

## Sonuç

✅ Jackson global olarak unknown properties'leri ignore ediyor
✅ Frontend gereksiz field'ları göndermiyor
✅ Part model'ine annotation eklendi
✅ Tüm endpoint'ler çalışmalı

## Not

**Backend servisini restart etmek gerekiyor!**
- JacksonConfig değişikliği için
- Part.java annotation değişikliği için

Restart sonrası frontend'den Part create çalışmalı.

