# Production Deployment - Database Migration Rehberi

## Soru: Docker Compose Down/Build yapınca migration'lar çalışır mı?

### ✅ EVET, Migration'lar Çalışır!

**Nasıl çalışır:**
1. `docker compose down` → Container'ları durdurur, **AMA database volume'u korunur**
2. `docker compose build` → Yeni kod ile image'ı build eder
3. `docker compose up` → Container'ları başlatır
4. **Spring Boot başlarken Flyway otomatik migration'ları çalıştırır**

### 🔍 Migration Çalışma Mantığı

Flyway `flyway_schema_history` tablosunda hangi migration'ların çalıştığını tutar:

```sql
SELECT * FROM flyway_schema_history;
```

**Durum 1: Database Volume Korunursa (Normal Deploy)**
- ✅ Sadece **yeni migration'lar** çalışır (V1-V9 arası çalışmamış olanlar)
- ✅ Mevcut veriler korunur
- ✅ Hızlı deploy

**Durum 2: Database Volume Silinirse (⚠️ DİKKAT!)**
- ⚠️ **TÜM migration'lar baştan çalışır** (V1'den V9'a kadar)
- ⚠️ **TÜM VERİLER SİLİNİR!**
- ⚠️ Production'da ASLA yapma!

### 📋 Production Deploy Adımları

#### Güvenli Deploy (Önerilen)

```bash
# 1. Mevcut container'ları durdur (volume korunur)
docker compose -f docker-compose.prod.yml down

# 2. Yeni kod ile image'ı build et
docker compose -f docker-compose.prod.yml build

# 3. Container'ları başlat (Flyway migration'ları otomatik çalışır)
docker compose -f docker-compose.prod.yml up -d

# 4. Logları kontrol et (migration'ların çalıştığını gör)
docker compose -f docker-compose.prod.yml logs -f app
```

#### Migration'ların Çalıştığını Kontrol Et

```bash
# App loglarında Flyway migration mesajlarını gör:
docker compose -f docker-compose.prod.yml logs app | grep -i flyway

# Veya database'e bağlanıp kontrol et:
docker exec -it sara-asansor-postgres-prod psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### ⚠️ ÖNEMLİ UYARILAR

1. **Volume Silme (ASLA YAPMA!):**
   ```bash
   # ❌ YANLIŞ - Tüm verileri siler!
   docker compose -f docker-compose.prod.yml down -v
   ```

2. **Migration Kontrolü:**
   - Production'da migration'lar **sadece bir kez** çalışır
   - Flyway `flyway_schema_history` tablosunda takip eder
   - Aynı migration iki kez çalışmaz

3. **Yeni Migration Varsa:**
   - Yeni migration dosyaları (V10, V11, vs.) eklediysen
   - Sadece yeni olanlar çalışır
   - Mevcut veriler korunur

### 🔄 Migration Dosyaları (Mevcut)

```
V1__schema.sql                    ✅ Schema oluşturma
V2__base_seed.sql                 ✅ Base seed (production-safe)
V3__dev_seed.sql                  ❌ Dev only (production'da çalışmaz)
V4__add_audit_fields_to_maintenance_plans.sql
V5__add_lifecycle_fields_to_maintenance_plans.sql
V6__fix_cancellation_logic.sql
V7__add_active_to_maintenance_sections.sql
V8__add_order_columns_to_collections.sql
V9__add_admin_role_and_maintenance_start_audit.sql
```

### ✅ Sonuç

**Evet, migration'lar otomatik çalışır!**

- `docker compose down` → Volume korunur ✅
- `docker compose build` → Yeni kod build edilir ✅
- `docker compose up` → Spring Boot başlarken Flyway migration'ları çalışır ✅
- Sadece yeni/çalışmamış migration'lar çalışır ✅
- Mevcut veriler korunur ✅

**Tek yapman gereken:**
```bash
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml build
docker compose -f docker-compose.prod.yml up -d
```

Migration'lar otomatik çalışacak! 🚀
