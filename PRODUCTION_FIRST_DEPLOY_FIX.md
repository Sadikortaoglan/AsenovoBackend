# Production First Deploy Fix - Missing Table [buildings]

## 🔴 HATA
```
Schema-validation: missing table [buildings]
```

## 🔍 SORUN
Database boş (yeni volume). Flyway migration'ları çalışmamış veya Hibernate validate modu migration'lardan önce çalışıyor.

## ✅ HIZLI ÇÖZÜM (İlk Deploy İçin)

### 1. Database Durumunu Kontrol Et

```bash
# PostgreSQL container'a bağlan
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}

# Tabloları kontrol et
\dt

# Flyway history kontrol et
SELECT * FROM flyway_schema_history;
```

### 2. Geçici Çözüm: ddl-auto: update (Sadece İlk Deploy)

**application-prod.yml'de geçici olarak:**
```yaml
jpa:
  hibernate:
    ddl-auto: update  # TEMPORARY: First deploy only
```

**Sonra:**
```bash
# Container'ı durdur
docker compose -f docker-compose.prod.yml down

# Rebuild
docker compose -f docker-compose.prod.yml build

# Başlat
docker compose -f docker-compose.prod.yml up -d

# Logları kontrol et
docker compose -f docker-compose.prod.yml logs -f app
```

### 3. Schema Oluştuktan Sonra validate'e Geri Dön

**application-prod.yml'de:**
```yaml
jpa:
  hibernate:
    ddl-auto: validate  # Tekrar validate
```

**Sonra rebuild:**
```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

## 🔍 ALTERNATİF: Flyway Migration'larını Manuel Kontrol

Eğer `ddl-auto: update` istemiyorsan:

```bash
# 1. Database'e bağlan
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}

# 2. Flyway history kontrol et
SELECT * FROM flyway_schema_history;

# 3. Eğer boşsa, migration dosyalarını container'a kopyala ve çalıştır
# (Ama bu karmaşık, ddl-auto: update daha kolay)
```

## ⚠️ ÖNEMLİ

1. **`ddl-auto: update` sadece ilk deploy için!**
2. Schema oluştuktan sonra **mutlaka `validate`'e geri dön**
3. Production'da `update` modu güvenlik riski oluşturabilir

## 📋 KONTROL KOMUTLARI

```bash
# Tabloları kontrol et
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "\dt"

# Flyway history
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "SELECT * FROM flyway_schema_history;"
```
