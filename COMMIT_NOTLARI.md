# Commit Notları - Backend Güncellemeleri

**Tarih:** 2026-02-10  
**Commit:** Technician Auto Assignment, Photo Validation, Elevator Form Validation

---

## 🚨 ÖNEMLİ: DEPLOY ÖNCESİ DİKKAT EDİLMESİ GEREKENLER

### ⚠️ DATABASE DEĞİŞİKLİKLERİ VAR!

Bu commit'te **YENİ BİR MIGRATION** çalışacak:
- **V12__elevator_form_validation_updates.sql**

### 📋 Deploy Adımları

1. **Database Backup Alın**
   ```bash
   # Production database backup
   docker exec sara-asansor-postgres-prod pg_dump -U sara_asansor sara_asansor > backup_$(date +%Y%m%d_%H%M%S).sql
   ```

2. **Migration Kontrolü**
   - Migration otomatik çalışacak (Flyway)
   - V12 migration şunları yapacak:
     - ORANGE enum değeri ekleyecek
     - `manager_tc_identity_no` ve `manager_phone` kolonlarını NOT NULL yapacak
     - Mevcut NULL kayıtları backfill edecek (default değerlerle)
     - Database constraint'leri ekleyecek

3. **Mevcut Veriler İçin**
   - Eğer `manager_tc_identity_no` veya `manager_phone` NULL ise:
     - `manager_tc_identity_no` → '00000000000' olarak set edilecek
     - `manager_phone` → '0000000000' olarak set edilecek
   - **ÖNEMLİ:** Bu default değerleri sonradan düzeltmeniz gerekebilir!

4. **Uygulama Başlatma**
   - Migration başarılı olursa uygulama normal başlayacak
   - Migration başarısız olursa uygulama başlamayacak (logları kontrol edin)

---

## 📝 Bu Commit'te Yapılan Değişiklikler

### 1. Technician Otomatik Atama ✅

**Ne Değişti:**
- Maintenance oluşturulurken technician artık **otomatik** olarak authenticated user'a atanıyor
- Request body'den `technicianUserId` kabul edilmiyor (ignored)
- Technician güncelleme endpoint'inde değiştirilemiyor

**Etki:**
- ✅ Mevcut maintenance'lar etkilenmez
- ✅ Yeni maintenance'lar otomatik technician alır
- ⚠️ Frontend'den `technicianUserId` gönderilmemeli (zaten ignore ediliyor)

**Database Değişikliği:** YOK

---

### 2. Fotoğraf Validasyonu (Minimum 4 Fotoğraf) ✅

**Ne Değişti:**
- Yeni endpoint: `POST /api/maintenances/{id}/photos`
- Maintenance için **en az 4 fotoğraf** zorunlu
- 4'ten az fotoğraf gönderilirse hata döner

**Etki:**
- ✅ Mevcut maintenance'lar etkilenmez
- ✅ Yeni fotoğraf yükleme için 4 fotoğraf zorunlu
- ⚠️ Frontend'den minimum 4 fotoğraf gönderilmeli

**Database Değişikliği:** YOK (sadece yeni endpoint)

---

### 3. Elevator Form Validation Güncellemeleri ✅

**Ne Değişti:**

#### a) Label Type
- ORANGE eklendi: GREEN, YELLOW, RED, ORANGE, BLUE
- Label type artık **ZORUNLU** (required)

#### b) Manager Bilgileri Zorunlu
- `managerTcIdentityNo`: Zorunlu, tam 11 rakam
- `managerPhone`: Zorunlu, 10-11 rakam (Türkiye formatı)

#### c) End Date Logic
- İki seçenek:
  - **Option A:** End date explicit olarak gönderilir
  - **Option B:** End date gönderilmez, `labelDate + duration` ile hesaplanır

#### d) Validation
- Tüm zorunlu alanlar için validation eklendi
- Format validation (TC kimlik, telefon)
- Business rule: `endDate > labelDate`

**Etki:**
- ⚠️ **YENİ ELEVATOR OLUŞTURMA:** Tüm zorunlu alanlar dolu olmalı
- ⚠️ **ELEVATOR GÜNCELLEME:** Tüm zorunlu alanlar dolu olmalı
- ⚠️ **MEVCUT ELEVATOR'LAR:** Manager bilgileri NULL ise default değerlerle doldurulacak

**Database Değişikliği:** ✅ VAR (V12 migration)

---

## 🗄️ DATABASE MIGRATION DETAYLARI (V12)

### Yapılacaklar:

1. **ORANGE Enum Eklenecek**
   - `label_type` enum'ına ORANGE değeri eklenecek
   - Mevcut kayıtlar etkilenmez

2. **Manager Fields NOT NULL Yapılacak**
   - `manager_tc_identity_no`: NOT NULL, length=11
   - `manager_phone`: NOT NULL
   - **Backfill:** NULL kayıtlar default değerlerle doldurulacak
     - `manager_tc_identity_no` → '00000000000'
     - `manager_phone` → '0000000000'

3. **Database Constraints Eklenecek**
   - `elevators_expiry_after_label_date`: `CHECK (expiry_date > label_date)`
   - `elevators_manager_tc_format`: `CHECK (manager_tc_identity_no ~ '^[0-9]{11}$')`
   - `elevators_manager_phone_format`: `CHECK (manager_phone ~ '^[0-9]{10,11}$')`

4. **Mevcut Kayıtlar İçin Backfill**
   - `label_type` NULL ise → 'BLUE'
   - `label_date` NULL ise → `inspection_date`
   - `expiry_date` NULL ise → `label_date + duration` (label_type'a göre)

### ⚠️ DİKKAT:

- **Mevcut elevator kayıtlarında** `manager_tc_identity_no` veya `manager_phone` NULL ise:
  - Default değerlerle doldurulacak ('00000000000', '0000000000')
  - **Bu kayıtları sonradan düzeltmeniz gerekebilir!**

- **Migration başarısız olursa:**
  - Uygulama başlamaz
  - Logları kontrol edin
  - Gerekirse migration'ı manuel çalıştırın

---

## 🔄 Frontend Güncellemeleri Gerekli

### 1. Maintenance Oluşturma
- ❌ `technicianUserId` göndermeyin (artık kabul edilmiyor)
- ✅ Technician otomatik atanacak (logged-in user)

### 2. Maintenance Fotoğraf Yükleme
- ✅ Yeni endpoint: `POST /api/maintenances/{id}/photos`
- ✅ Minimum 4 fotoğraf gönderilmeli
- ✅ `multipart/form-data` formatında

### 3. Elevator Form
- ✅ `labelType` zorunlu (GREEN, YELLOW, RED, ORANGE, BLUE)
- ✅ `labelDate` zorunlu
- ✅ `expiryDate` zorunlu (veya boş bırakılırsa otomatik hesaplanır)
- ✅ `managerTcIdentityNo` zorunlu (11 rakam)
- ✅ `managerPhone` zorunlu (10-11 rakam)

---

## 📊 Migration Sonrası Kontrol

Deploy sonrası şunları kontrol edin:

```sql
-- Manager fields NULL kontrolü (olmamalı)
SELECT id, manager_tc_identity_no, manager_phone 
FROM elevators 
WHERE manager_tc_identity_no IS NULL OR manager_phone IS NULL;

-- ORANGE enum kontrolü
SELECT DISTINCT label_type FROM elevators;

-- Constraints kontrolü
SELECT constraint_name, constraint_type 
FROM information_schema.table_constraints 
WHERE table_name = 'elevators' 
AND constraint_name LIKE '%manager%' OR constraint_name LIKE '%expiry%';
```

---

## 🚀 Deploy Komutları

### Production Deploy

```bash
# 1. Backup al
docker exec sara-asansor-postgres-prod pg_dump -U sara_asansor sara_asansor > backup_$(date +%Y%m%d_%H%M%S).sql

# 2. Code pull
cd /opt/SarAsansorBackend/backend
git pull origin main

# 3. Build
mvn clean package -DskipTests

# 4. Restart (migration otomatik çalışacak)
docker compose -f docker-compose.prod.yml restart app

# 5. Logları kontrol et
docker compose -f docker-compose.prod.yml logs -f app | grep -i "migration\|error\|exception"
```

---

## ✅ Başarı Kriterleri

Deploy başarılı sayılır eğer:

1. ✅ Migration V12 başarıyla çalıştı
2. ✅ Uygulama başladı (port 8080'de çalışıyor)
3. ✅ Health check: `GET /api/health` → 200 OK
4. ✅ Yeni elevator oluşturma çalışıyor (validation'lar aktif)
5. ✅ Maintenance oluşturma çalışıyor (technician otomatik atanıyor)
6. ✅ Fotoğraf yükleme çalışıyor (minimum 4 fotoğraf kontrolü)

---

## 🐛 Sorun Giderme

### Migration Başarısız Olursa

```bash
# Migration durumunu kontrol et
mvn flyway:info -Dflyway.url=jdbc:postgresql://localhost:5433/sara_asansor -Dflyway.user=sara_asansor -Dflyway.password=sara_asansor

# Manuel migration çalıştır
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5433/sara_asansor -Dflyway.user=sara_asansor -Dflyway.password=sara_asansor
```

### Manager Fields NULL Hatası

Eğer migration sonrası hala NULL kayıtlar varsa:

```sql
-- Manuel düzelt
UPDATE elevators 
SET manager_tc_identity_no = '00000000000', 
    manager_phone = '0000000000'
WHERE manager_tc_identity_no IS NULL OR manager_phone IS NULL;
```

---

## 📞 Destek

Sorun olursa:
1. Logları kontrol edin: `docker compose logs app`
2. Database migration durumunu kontrol edin
3. Health endpoint'i test edin: `curl http://localhost:8080/api/health`

---

**ÖNEMLİ:** Bu commit'te database değişikliği var. Mutlaka backup alın ve migration'ı test edin!
