# Production Migration Fix - Missing Table [buildings]

## 🔴 HATA
```
Schema-validation: missing table [buildings]
```

## 🔍 SORUN ANALİZİ

Flyway migration'ları çalışmamış görünüyor. Database boş (yeni volume) veya migration'lar başarısız olmuş.

## ✅ ÇÖZÜM ADIMLARI

### 1. Database'e Bağlan ve Kontrol Et

```bash
# PostgreSQL container'a bağlan
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}

# Tabloları kontrol et
\dt

# Flyway migration history'yi kontrol et
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### 2. Eğer flyway_schema_history Tablosu Yoksa

Database tamamen boş demektir. Migration'lar çalışmamış.

**Çözüm A: ddl-auto'yu geçici olarak update yap (Hızlı Fix)**

```bash
# Container'ı durdur
docker compose -f docker-compose.prod.yml down

# application-prod.yml'de geçici olarak:
# ddl-auto: validate → ddl-auto: update

# Tekrar başlat
docker compose -f docker-compose.prod.yml up -d
```

**Çözüm B: Flyway migration'larını manuel çalıştır (Önerilen)**

```bash
# Database'e bağlan
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}

# V1__schema.sql'i manuel çalıştır
\i /path/to/V1__schema.sql

# Veya migration dosyalarını container'a kopyala ve çalıştır
```

### 3. Eğer flyway_schema_history Varsa Ama Tablolar Yoksa

Migration'lar başarısız olmuş olabilir.

```bash
# Flyway history'yi kontrol et
SELECT * FROM flyway_schema_history;

# Eğer migration'lar başarısızsa, repair yap
# Spring Boot loglarında Flyway hatalarını ara
docker compose -f docker-compose.prod.yml logs app | grep -i flyway
```

### 4. Geçici Çözüm: ddl-auto: update (Sadece İlk Deploy İçin)

**⚠️ DİKKAT: Sadece ilk deploy için! Sonra validate'e geri dön!**

`application-prod.yml`:
```yaml
jpa:
  hibernate:
    ddl-auto: update  # Geçici olarak update
```

Sonra:
```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Schema oluştuktan sonra tekrar `validate`'e dön:
```yaml
jpa:
  hibernate:
    ddl-auto: validate  # Tekrar validate
```

## 🔍 KONTROL KOMUTLARI

```bash
# 1. Container loglarını kontrol et
docker compose -f docker-compose.prod.yml logs app | grep -i flyway

# 2. Database'de tabloları kontrol et
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "\dt"

# 3. Flyway history kontrol et
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "SELECT * FROM flyway_schema_history;"
```

## 📋 MUHTEMEL NEDENLER

1. **Database volume yeni oluşturulmuş** (boş database)
2. **Flyway migration'ları çalışmamış** (başlangıç sırası problemi)
3. **Migration dosyaları container'a kopyalanmamış** (Dockerfile problemi)
4. **Flyway disabled** (application-prod.yml'de enabled: false)

## ✅ ÖNERİLEN ÇÖZÜM

1. Database'e bağlan ve durumu kontrol et
2. Eğer boşsa, geçici olarak `ddl-auto: update` yap
3. Schema oluştuktan sonra `validate`'e geri dön
4. Sonraki deploy'larda Flyway migration'ları çalışacak
