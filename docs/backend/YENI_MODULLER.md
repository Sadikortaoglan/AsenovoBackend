# Yeni Added ERP Modules

## 📋 Summary

Backend'e 4 yeni modül eklendi:

1. **Fault (Fault)** - Fault Tracking modülü
2. **Inspection (Inspection)** - Inspection Record modülü
3. **Maintenance Filtering** - Existing Maintenance modülüne Advanced Filtering
4. **PaymentReceipt (Receipt Fişleri)** - Receipt Tracking modülü

---

## 1) Fault (Fault) Modülü

### Entity
- **Tablo**: `faults`
- **Alanlar**:
  - id, elevatorId, arizaKonu, gorusulenKisi, binaYetkiliMesaji, aciklama
  - Status: ACIK (default), TAMAMLANDI
  - createdAt

### Endpoints

#### POST /api/faults
- Yeni Fault kaydı Creates
- Request Body: FaultDto (elevatorId, arizaKonu, gorusulenKisi, binaYetkiliMesaji, aciklama)
- Audit Log: ARIZA_OLUSTURULDU

#### GET /api/faults
- Tüm Fault kayıtlarını Lists
- Query Param: `status` (ACIK veya TAMAMLANDI) - opsiyonel
- Örnek: `GET /api/faults?status=ACIK`

#### GET /api/faults/{id}
- ID'ye göre Fault Details

#### PUT /api/faults/{id}/status
- Fault durumunu Updates
- Query Param: `status` (ACIK veya TAMAMLANDI)
- Audit Log: ARIZA_DURUM_GUNCELLENDI

---

## 2) Inspection (Inspection) Modülü

### Entity
- **Tablo**: `inspections`
- **Alanlar**:
  - id, elevatorId, Date, sonuc, aciklama, createdAt

### Endpoints

#### POST /api/inspections
- Yeni Inspection kaydı Creates
- Request Body: InspectionDto (elevatorId, Date, sonuc, aciklama)
- Audit Log: DENETIM_OLUSTURULDU

#### GET /api/inspections
- Tüm Inspection kayıtlarını Lists

#### GET /api/inspections/{id}
- ID'ye göre Inspection Details

#### GET /api/inspections/elevator/{elevatorId}
- Belirli asansörün Inspection kayıtlarını Lists (Date sıralı DESC)

---

## 3) Maintenance Filtering (Geliştirmeler)

### Yeni Endpoint'ler

#### GET /api/maintenances (Güncellendi)
- **Yeni Query Parametreler**:
  - `paid` (Boolean) - Ödendi/ödenmedi filtresi
    - `paid=true`: Sadece ödenenler
    - `paid=false`: Sadece ödenmeyenler
    - Parametre yok: Tümü
  - `dateFrom` (Date) - Başlangıç tarihi (ISO format: YYYY-MM-DD)
  - `dateTo` (Date) - Expiry tarihi (ISO format: YYYY-MM-DD)

**Örnekler**:
- `GET /api/maintenances?paid=true` - Sadece ödenenler
- `GET /api/maintenances?dateFrom=2026-01-01&dateTo=2026-01-31` - Ocak 2026
- `GET /api/maintenances?paid=false&dateFrom=2026-01-01` - Ödenmeyen ve Ocak'tan sonra

#### GET /api/maintenances/summary
- **Aylık Maintenance özeti**
- Query Param: `month` (String, format: YYYY-MM) - opsiyonel, varsayılan: bu ay
- Response: MaintenanceSummaryDto
  - totalCount: Toplam Maintenance sayısı
  - paidCount: Ödenen Maintenance sayısı
  - unpaidCount: Ödenmeyen Maintenance sayısı
  - totalAmount: Toplam tutar
  - paidAmount: Ödenen tutar
  - unpaidAmount: Ödenmeyen tutar

**Örnek**:
- `GET /api/maintenances/summary?month=2026-01` - Ocak 2026 özeti

---

## 4) PaymentReceipt (Receipt Fişleri) Modülü

### Entity
- **Tablo**: `payment_receipts`
- **Alanlar**:
  - id, maintenanceId, amount, payerName, date, note, createdAt

### İş Kuralı
- Receipt fişi oluşturulduğunda, ilgili maintenance Automatic olarak `odendi=true` olarak işaretlenir
- Eğer maintenance'ın `odemeTarihi` yoksa, Receipt fişinin tarihi set edilir

### Endpoints

#### POST /api/payments
- Yeni Receipt fişi Creates
- Request Body: PaymentReceiptDto (maintenanceId, amount, payerName, date, note)
- Audit Log: TAHSILAT_FISI_OLUSTURULDU
- **Automatic**: İlgili maintenance `odendi=true` olarak işaretlenir

#### GET /api/payments
- Tüm Receipt fişlerini Lists
- Query Parametreler:
  - `dateFrom` (Date) - Başlangıç tarihi
  - `dateTo` (Date) - Expiry tarihi
- Örnek: `GET /api/payments?dateFrom=2026-01-01&dateTo=2026-01-31`

#### GET /api/payments/{id}
- ID'ye göre Receipt fişi Details

---

## 📊 Database Değişiklikleri

### Yeni Tablolar (V3 Migration)

1. **faults**
   - Foreign Key: elevator_id → elevators(id) CASCADE DELETE
   - Index: elevator_id, Status, created_at

2. **inspections**
   - Foreign Key: elevator_id → elevators(id) CASCADE DELETE
   - Index: elevator_id, Date

3. **payment_receipts**
   - Foreign Key: maintenance_id → maintenances(id) CASCADE DELETE
   - Index: maintenance_id, date

### Migration Dosyası
- `V3__add_fault_inspection_payment_tables.sql`

---

## 🔍 Audit Log Kayıtları

Yeni modüller aşağıdaki audit log kayıtlarını Creates:

- **ARIZA_OLUSTURULDU** - Yeni Fault kaydı oluşturulduğunda
- **ARIZA_DURUM_GUNCELLENDI** - Fault durumu değiştirildiğinde
- **DENETIM_OLUSTURULDU** - Yeni Inspection kaydı oluşturulduğunda
- **TAHSILAT_FISI_OLUSTURULDU** - Yeni Receipt fişi oluşturulduğunda

---

## 📝 Validation

Tüm DTO'lara validation annotation'ları eklendi:

- **FaultDto**: elevatorId, arizaKonu, gorusulenKisi - NotBlank/NotNull
- **InspectionDto**: elevatorId, Date, sonuc - NotBlank/NotNull
- **PaymentReceiptDto**: maintenanceId, amount (Positive), payerName - NotBlank/NotNull

---

## 🎯 Endpoint Özeti

### Yeni Endpoint'ler (Toplam 10)

**Fault (4)**:
- POST /api/faults
- GET /api/faults
- GET /api/faults/{id}
- PUT /api/faults/{id}/status

**Inspection (4)**:
- POST /api/inspections
- GET /api/inspections
- GET /api/inspections/{id}
- GET /api/inspections/elevator/{elevatorId}

**Payment (3)**:
- POST /api/payments
- GET /api/payments
- GET /api/payments/{id}

**Maintenance (Güncellendi, 1 yeni)**:
- GET /api/maintenances (Filtering eklendi)
- GET /api/maintenances/summary (YENİ)

---

## ✅ Test Senaryoları

1. **Fault Oluşturma**:
   ```bash
   POST /api/faults
   {
     "elevatorId": 1,
     "arizaKonu": "Motor çalışmıyor",
     "gorusulenKisi": "Ahmet Yılmaz",
     "binaYetkiliMesaji": "Acil müdahale gerekiyor",
     "aciklama": "Elevator 2. katta takıldı"
   }
   ```

2. **Fault Durumu Güncelleme**:
   ```bash
   PUT /api/faults/1/status?status=TAMAMLANDI
   ```

3. **Inspection Oluşturma**:
   ```bash
   POST /api/inspections
   {
     "elevatorId": 1,
     "Date": "2026-01-10",
     "sonuc": "BAŞARILI",
     "aciklama": "Tüm kontroller yapıldı"
   }
   ```

4. **Receipt Fişi Oluşturma**:
   ```bash
   POST /api/payments
   {
     "maintenanceId": 1,
     "amount": 500.0,
     "payerName": "Building Yönetimi",
     "date": "2026-01-10",
     "note": "Nakit Payment"
   }
   ```

5. **Maintenance Filtering**:
   ```bash
   GET /api/maintenances?paid=false&dateFrom=2026-01-01
   ```

6. **Aylık Summary**:
   ```bash
   GET /api/maintenances/summary?month=2026-01
   ```

---

## 🔄 Migration Çalıştırma

Migration Automatic olarak uygulanacaktır. Manuel Control için:

```bash
cd backend
docker-compose restart app
```

Migration başarılı olursa yeni tablolar oluşturulmuş olacaktır.

---

**Date**: 2026-01-10
**Versiyon**: 1.1.0 (Backend'e Added modüller)

