# Sara Elevator Backend API - Teknik Rapor

## 1) Technology Stack

### Java Version
- Java 17 (LTS)
- Maven Compiler Source/Target: 17

### Spring Boot Version
- Spring Boot 3.2.0
- Spring Framework 6.x

### Database
- PostgreSQL (production)
- Hibernate JPA (ORM)
- Flyway (Database Migration Tool)
- Connection: jdbc:postgresql://localhost:5433/sara_asansor
- Default User: sara_asansor / sara_asansor

### Authentication & Authorization
- JWT (JSON Web Token) - Access Token + Refresh Token
- Access Token Duration: 1 saat (3600000 ms)
- Refresh Token Duration: 7 gün (604800000 ms)
- BCrypt Password Encoding
- Spring Security 6.x
- Stateless authentication (session yok)

### Build Tool
- Maven
- Packaging: JAR

### PDF & Dosya Depolama Araçları
- Apache PDFBox 2.0.29 (PDF üretimi için)
- AWS S3 SDK 2.21.0 (S3-compatible storage için)
- File Storage Tipi: Local (development) veya S3 (production)
- Local storage dizini: ./uploads
- Max file size: 10MB

### Diğer Teknolojiler
- Lombok (code generation)
- Jackson (JSON processing)
- SpringDoc OpenAPI 2.3.0 (Swagger UI)
- Validation API (Jakarta Validation)

### API Documentation
- Swagger UI: http://localhost:8081/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8081/api/api-docs

---

## 2) Architecture

### Package Structure

```
com.saraasansor.api/
├── config/              # Konfigürasyon sınıfları
│   ├── GlobalExceptionHandler.java
│   ├── JacksonConfig.java
│   └── OpenApiConfig.java
├── controller/          # REST API Controllers
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── ElevatorController.java
│   ├── MaintenanceController.java
│   ├── PartController.java
│   └── WarningController.java
├── dto/                 # Data Transfer Objects
│   ├── ApiResponse.java
│   ├── DashboardSummaryDto.java
│   ├── ElevatorDto.java
│   ├── ElevatorStatusDto.java
│   ├── MaintenanceDto.java
│   └── auth/
│       ├── LoginRequest.java
│       ├── LoginResponse.java
│       ├── RefreshTokenRequest.java
│       └── RegisterRequest.java
├── model/               # JPA Entity Models
│   ├── AuditLog.java
│   ├── Elevator.java
│   ├── FileAttachment.java
│   ├── Maintenance.java
│   ├── Offer.java
│   ├── OfferItem.java
│   ├── Part.java
│   └── User.java
├── repository/          # Spring Data JPA Repositories
│   ├── AuditLogRepository.java
│   ├── ElevatorRepository.java
│   ├── FileAttachmentRepository.java
│   ├── MaintenanceRepository.java
│   ├── OfferItemRepository.java
│   ├── OfferRepository.java
│   ├── PartRepository.java
│   └── UserRepository.java
├── security/            # Security & Authentication
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── SecurityConfig.java
├── service/             # Business Logic Services
│   ├── AuthService.java
│   ├── DashboardService.java
│   ├── ElevatorService.java
│   ├── MaintenanceService.java
│   └── PartService.java
└── util/                # Utility Classes
    └── AuditLogger.java
```

### Layer Mimarisi

**3-Tier Architecture:**

1. **Controller Layer** (REST API)
   - HTTP Requests Handles
   - Request validation
   - Response Formatting
   - HTTP status code yönetimi

2. **Service Layer** (Business Logic)
   - Tüm iş Logic burada
   - Transaction yönetimi
   - Repository katmanına Access
   - DTO dönüşümleri

3. **Repository Layer** (Data Access)
   - Spring Data JPA Repositories
   - Database Operations
   - Custom query'ler

### İş Logic Organization

- Tüm iş Logic Service katmanında
- Controllers sadece HTTP Requests handle eder
- DTOs veri transferi için kullanılır
- Entity'ler Database tablolarını Represents eder
- Validation DTO Level (@Valid)
- Exception handling GlobalExceptionHandler ile

---

## 3) Domain Models

### User (User)
- **id**: Long (PK, auto increment)
- **username**: String (unique, not null)
- **passwordHash**: String (BCrypt hash, not null)
- **role**: Enum (PATRON, PERSONEL) - not null
- **active**: Boolean (default: true) - not null
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- Maintenance -> Technician (ManyToOne, optional)
- FileAttachment -> uploadedBy (ManyToOne, optional)
- AuditLog -> user (ManyToOne, optional)

### Elevator (Elevator)
- **id**: Long (PK, auto increment)
- **kimlikNo**: String (unique, not null)
- **binaAdi**: String (not null)
- **Address**: String (not null)
- **asansorNo**: String (optional)
- **durakSayisi**: Integer (optional)
- **kapasite**: Integer (optional)
- **hiz**: Double (optional)
- **teknikNotlar**: String (optional)
- **tahrikTipi**: String (optional)
- **makineMarka**: String (optional)
- **kapiTipi**: String (optional)
- **montajYili**: Integer (optional)
- **seriNo**: String (optional)
- **kumanda**: String (optional)
- **halat**: String (optional)
- **modernizasyon**: String (optional)
- **maviEtiketTarihi**: LocalDate (not null)
- **bitisTarihi**: LocalDate (not null) - hesaplanan: maviEtiketTarihi + 12 ay
- **createdAt**: LocalDateTime (auto, not updatable)
- **updatedAt**: LocalDateTime (auto, güncellenir)

**İlişkiler:**
- Maintenance -> elevator (OneToMany)
- Offer -> elevator (ManyToOne, optional)

### Maintenance (Maintenance)
- **id**: Long (PK, auto increment)
- **elevator**: Elevator (ManyToOne, not null, CASCADE DELETE)
- **Date**: LocalDate (not null)
- **aciklama**: String (TEXT, optional)
- **Technician**: User (ManyToOne, optional)
- **ucret**: Double (optional)
- **odendi**: Boolean (default: false, not null)
- **odemeTarihi**: LocalDate (optional)
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- Elevator -> maintenances (OneToMany)

### Part (Part/Stock)
- **id**: Long (PK, auto increment)
- **ad**: String (not null)
- **birimFiyat**: Double (not null)
- **Stock**: Integer (default: 0, not null)
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- OfferItem -> part (ManyToOne)

### Offer (Offer)
- **id**: Long (PK, auto increment)
- **elevator**: Elevator (ManyToOne, optional)
- **Date**: LocalDate (not null)
- **kdvOran**: Double (default: 20.0, not null)
- **indirimTutar**: Double (default: 0.0)
- **araToplam**: Double (default: 0.0, not null)
- **genelToplam**: Double (default: 0.0, not null)
- **Status**: Enum (BEKLIYOR, KABUL, RED) - default: BEKLIYOR
- **items**: List<OfferItem> (OneToMany, CASCADE ALL)
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- OfferItem -> offer (ManyToOne)
- Elevator -> offers (OneToMany)

### OfferItem (Offer Kalemi)
- **id**: Long (PK, auto increment)
- **offer**: Offer (ManyToOne, not null, CASCADE DELETE)
- **part**: Part (ManyToOne, not null)
- **adet**: Integer (not null)
- **birimFiyat**: Double (not null)
- **satirToplam**: Double (not null) - hesaplanan: adet * birimFiyat

**İlişkiler:**
- Offer -> items (OneToMany)
- Part -> offerItems (OneToMany)

### FileAttachment (Dosya Eki)
- **id**: Long (PK, auto increment)
- **entityType**: Enum (ELEVATOR, MAINTENANCE, OFFER) - not null
- **entityId**: Long (not null)
- **fileName**: String (not null)
- **contentType**: String (not null)
- **size**: Long (not null) - byte cinsinden
- **storageKey**: String (not null) - dosya storage key
- **url**: String (optional) - dosya URL'i
- **uploadedBy**: User (ManyToOne, optional)
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- User -> fileAttachments (OneToMany)

### AuditLog (Inspection Kaydı)
- **id**: Long (PK, auto increment)
- **user**: User (ManyToOne, optional)
- **action**: String (not null) - Operation adı
- **entityType**: String (optional) - entity tipi
- **entityId**: Long (optional) - entity ID
- **metadataJson**: String (TEXT, optional) - JSON metadata
- **createdAt**: LocalDateTime (auto, not updatable)

**İlişkiler:**
- User -> auditLogs (OneToMany)

---

## 4) API Endpoints

### Base URL
```
http://localhost:8081/api
```

### Authentication Endpoints

#### POST /auth/login
- **Amaç**: User girişi
- **Request Body**: LoginRequest (username, password)
- **Response**: LoginResponse (accessToken, refreshToken, userId, username, role)
- **Authentication**: Gerekmez (permitAll)

#### POST /auth/register
- **Amaç**: Yeni User kaydı ve Automatic login
- **Request Body**: RegisterRequest (username, password, role)
- **Response**: LoginResponse (accessToken, refreshToken, userId, username, role)
- **Authentication**: Gerekmez (permitAll)
- **Not**: Automatic olarak Login yapar

#### POST /auth/refresh
- **Amaç**: Refresh Token ile yeni access Token alma
- **Request Body**: RefreshTokenRequest (refreshToken)
- **Response**: LoginResponse (yeni accessToken, aynı refreshToken)
- **Authentication**: Gerekmez (permitAll)

#### POST /auth/logout
- **Amaç**: User çıkışı (client-side Token silme)
- **Response**: ApiResponse<Void>
- **Authentication**: Gerekli

### Elevator Endpoints

#### GET /elevators
- **Amaç**: Tüm asansörleri listele
- **Response**: ApiResponse<List<ElevatorDto>>
- **Authentication**: Gerekli

#### GET /elevators/{id}
- **Amaç**: ID'ye göre Elevator Details
- **Path Variable**: id (Long)
- **Response**: ApiResponse<ElevatorDto>
- **Authentication**: Gerekli

#### POST /elevators
- **Amaç**: Yeni Elevator ekle
- **Request Body**: ElevatorDto (validated)
- **Response**: ApiResponse<ElevatorDto>
- **Authentication**: Gerekli
- **İş Kuralı**: bitisTarihi = maviEtiketTarihi + 12 ay Automatic hesaplanır

#### PUT /elevators/{id}
- **Amaç**: Elevator güncelle
- **Path Variable**: id (Long)
- **Request Body**: ElevatorDto (validated)
- **Response**: ApiResponse<ElevatorDto>
- **Authentication**: Gerekli
- **İş Kuralı**: bitisTarihi = maviEtiketTarihi + 12 ay Automatic hesaplanır

#### DELETE /elevators/{id}
- **Amaç**: Elevator sil
- **Path Variable**: id (Long)
- **Response**: ApiResponse<Void>
- **Authentication**: Gerekli
- **CASCADE**: İlgili maintenances de silinir

#### GET /elevators/{id}/status
- **Amaç**: Elevator durumu kontrolü (OK, WARNING, EXPIRED)
- **Path Variable**: id (Long)
- **Response**: ApiResponse<ElevatorStatusDto>
- **Authentication**: Gerekli
- **İş Kuralı**: 
  - EXPIRED: bitisTarihi < bugün
  - WARNING: 30 gün veya daha az kaldı
  - OK: diğer durumlar

### Maintenance Endpoints

#### GET /maintenances
- **Amaç**: Tüm Maintenance kayıtlarını listele
- **Response**: ApiResponse<List<MaintenanceDto>>
- **Authentication**: Gerekli

#### GET /maintenances/{id}
- **Amaç**: ID'ye göre Maintenance kaydı Details
- **Path Variable**: id (Long)
- **Response**: ApiResponse<MaintenanceDto>
- **Authentication**: Gerekli

#### GET /maintenances/elevator/{elevatorId}
- **Amaç**: Belirli asansörün Maintenance kayıtlarını listele (Date sıralı DESC)
- **Path Variable**: elevatorId (Long)
- **Response**: ApiResponse<List<MaintenanceDto>>
- **Authentication**: Gerekli

#### POST /maintenances
- **Amaç**: Yeni Maintenance kaydı oluştur
- **Request Body**: MaintenanceDto (validated)
- **Response**: ApiResponse<MaintenanceDto>
- **Authentication**: Gerekli

#### PUT /maintenances/{id}
- **Amaç**: Maintenance kaydı güncelle
- **Path Variable**: id (Long)
- **Request Body**: MaintenanceDto (validated)
- **Response**: ApiResponse<MaintenanceDto>
- **Authentication**: Gerekli

#### DELETE /maintenances/{id}
- **Amaç**: Maintenance kaydı sil
- **Path Variable**: id (Long)
- **Response**: ApiResponse<Void>
- **Authentication**: Gerekli

#### POST /maintenances/{id}/mark-paid
- **Amaç**: Maintenance kaydını ödendi/ödenmedi olarak işaretle
- **Path Variable**: id (Long)
- **Query Parameter**: paid (boolean, default: true)
- **Response**: ApiResponse<MaintenanceDto>
- **Authentication**: Gerekli
- **İş Kuralı**: paid=true ise odemeTarihi = bugün

### Part Endpoints

#### GET /parts
- **Amaç**: Tüm Part/Stock listesi
- **Response**: ApiResponse<List<Part>>
- **Authentication**: Gerekli

#### GET /parts/{id}
- **Amaç**: ID'ye göre Part Details
- **Path Variable**: id (Long)
- **Response**: ApiResponse<Part>
- **Authentication**: Gerekli

#### POST /parts
- **Amaç**: Yeni Part ekle
- **Request Body**: Part (validated)
- **Response**: ApiResponse<Part>
- **Authentication**: Gerekli

#### PUT /parts/{id}
- **Amaç**: Part güncelle
- **Path Variable**: id (Long)
- **Request Body**: Part (validated)
- **Response**: ApiResponse<Part>
- **Authentication**: Gerekli

#### DELETE /parts/{id}
- **Amaç**: Part sil
- **Path Variable**: id (Long)
- **Response**: ApiResponse<Void>
- **Authentication**: Gerekli

### Offer Endpoints

**Not**: Offer endpoint'leri şu anda controller'da tanımlı değil, model hazır.

### Warning Endpoints

#### GET /warnings
- **Amaç**: Warning veren asansörleri listele
- **Query Parameter**: type (String, optional) - "EXPIRED" veya "WARNING"
  - type=EXPIRED: Duration dolmuş asansörler
  - type=WARNING: 30 gün içinde dolacak asansörler
  - type yok: Her ikisi
- **Response**: ApiResponse<List<ElevatorDto>>
- **Authentication**: Gerekli

### Dashboard Endpoints

#### GET /dashboard/summary
- **Amaç**: Dashboard Summary istatistikleri
- **Response**: ApiResponse<DashboardSummaryDto>
  - totalElevators: Long
  - totalMaintenances: Long
  - totalIncome: Double (ödendi=true olan maintenances'ların toplam ücreti)
  - totalDebt: Double (ödendi=false olan maintenances'ların toplam ücreti)
  - expiredCount: Long (Duration dolmuş Elevator sayısı)
  - warningCount: Long (30 gün içinde dolacak Elevator sayısı)
- **Authentication**: Gerekli

### User Endpoints

**Not**: User endpoint'leri şu anda controller'da tanımlı değil. SecurityConfig'de PATRON rolü gerektirir.

---

## 5) Business Rules

### Periodic Control Logic

- **Blue Label Tarihi**: Asansörün son Periodic Control tarihi
- **Expiry Tarihi**: Blue Label Tarihi + 12 ay (Automatic hesaplanır)
- Elevator oluşturulurken veya güncellenirken bitisTarihi Automatic hesaplanır
- MaviEtiketTarihi değiştiğinde audit log kaydı tutulur

### Warning System

- **EXPIRED**: Expiry tarihi bugünden önce olan asansörler
- **WARNING**: 30 gün veya daha az süre kalan asansörler
- **OK**: Diğer tüm durumlar
- GET /elevators/{id}/status endpoint'i ile Control edilir
- GET /warnings endpoint'i ile listelenir

### Payment Logic

- Maintenance kaydında `odendi` boolean alanı var (default: false)
- `odendi` = true ise `odemeTarihi` Automatic olarak bugün olarak set edilir
- `odendi` = false yapılırsa `odemeTarihi` null yapılır
- POST /maintenances/{id}/mark-paid endpoint'i ile Payment işaretlenir
- Dashboard'da totalIncome (ödendi=true) ve totalDebt (ödendi=false) hesaplanır

### Stock Rules

- Part entity'sinde `Stock` alanı integer (default: 0)
- Stock negatif olabilir (overflow kontrolü yok)
- Stock bilgisi OfferItem'larda kullanılır ama Automatic düşme yok

### Offer Calculation Rules

- OfferItem: satirToplam = adet * birimFiyat
- Offer: araToplam = tüm item'ların satirToplam toplamı
- Offer: genelToplam = (araToplam - indirimTutar) * (1 + kdvOran/100)
- Default KDV: %20
- Status: BEKLIYOR (default), KABUL, RED

**Not**: Offer Calculation Logic service katmanında henüz implement edilmemiş olabilir.

### PDF Üretim Logic

- Apache PDFBox kullanılıyor
- **Not**: PDF üretimi için service/endpoint henüz tanımlı değil görünüyor

### Audit Log Logic

- AuditLogger utility class'ı var
- IMPORTANT işlemler (ör. Periodic Date güncelleme) audit log'a kaydedilir
- Metadata JSON formatında saklanır
- User bilgisi varsa kaydedilir (SecurityContext'ten)

---

## 6) Security & Roles

### Login Çalışma Logic

1. User POST /auth/login ile username/password gönderir
2. AuthService login() metodu:
   - Spring Security AuthenticationManager ile authenticate eder
   - UserDetailsService kullanıcıyı yükler
   - BCrypt ile şifre doğrulanır
3. Başarılı olursa:
   - JwtTokenProvider ile accessToken ve refreshToken oluşturulur
   - Access Token: 1 saat geçerli, içinde userId, username, role var
   - Refresh Token: 7 gün geçerli, içinde username var
   - LoginResponse döner (accessToken, refreshToken, userId, username, role)

### Register Çalışma Logic

1. User POST /auth/register ile username/password/role gönderir
2. AuthService register() metodu:
   - Username unique kontrolü yapar
   - BCrypt ile şifre hash'lenir
   - User oluşturulur (role: PATRON veya PERSONEL)
   - User kaydedilir
3. Automatic login yapılır (Token'lar oluşturulur)
4. LoginResponse döner

### Token Yenileme

1. POST /auth/refresh ile refreshToken gönderilir
2. JwtTokenProvider Token'ı validate eder
3. Username'den User bulunur
4. Yeni accessToken oluşturulur (aynı refreshToken döner)

### Role Bazlı Yetkilendirme

- **PATRON**: Tüm endpoint'lere erişebilir (users/** hariç, users/** sadece PATRON)
- **PERSONEL**: Çoğu endpoint'e erişebilir, users/** endpoint'lerine erişemez

**SecurityConfig Rules:**
- /auth/** → permitAll() (herkese açık)
- /swagger-ui/** → permitAll() (Swagger erişimi)
- /users/** → hasRole("PATRON") (sadece PATRON)
- Diğer tüm endpoint'ler → authenticated() (Token gerekli)

### JWT Token Structure

- **Header**: Authorization: Bearer {Token}
- **Access Token İçeriği**:
  - sub: username
  - userId: Long
  - role: String (PATRON/PERSONEL)
  - iat: issued at timestamp
  - exp: expiration timestamp (1 saat)
- **Refresh Token İçeriği**:
  - sub: username
  - iat: issued at timestamp
  - exp: expiration timestamp (7 gün)

### Token Validation

- Her authenticated request'te JwtAuthenticationFilter çalışır
- Authorization header'dan Bearer Token çıkarılır
- Token validate edilir
- Username'den UserDetails yüklenir
- SecurityContext'e authentication set edilir

---

## 7) File & PDF Handling

### Dosya Yükleme/İndirme

**Model:**
- FileAttachment entity'si var
- entityType: ELEVATOR, MAINTENANCE, OFFER
- entityId: ilgili entity'nin ID'si
- storageKey: dosya storage key (local path veya S3 key)
- url: dosya Access URL'i

**Storage Structure:**
- Development: Local storage (./uploads dizini)
- Production: S3-compatible storage (AWS S3 veya MinIO gibi)
- Storage type application.yml'de belirlenir (FILE_STORAGE_TYPE)

**Config:**
- Max file size: 10MB
- Multipart enabled
- Max request size: 10MB

**Not**: File upload/download endpoint'leri şu anda controller'da tanımlı değil görünüyor.

### PDF Üretimi

- Apache PDFBox 2.0.29 dependency var
- **Not**: PDF üretimi için service/endpoint henüz tanımlı değil görünüyor
- Frontend'den PDF oluşturulması için endpoint'ler eklenmeli

---

## 8) Database

### Tablo Özeti

1. **users** - Kullanıcılar
   - Primary Key: id
   - Unique: username
   - Index: yok

2. **elevators** - Asansörler
   - Primary Key: id
   - Unique: kimlik_no
   - Index: kimlik_no, bitis_tarihi

3. **maintenances** - Bakımlar
   - Primary Key: id
   - Foreign Key: elevator_id → elevators(id) CASCADE DELETE
   - Foreign Key: teknisyen_user_id → users(id) SET NULL
   - Index: elevator_id, Date

4. **parts** - Parçalar/Stock
   - Primary Key: id
   - Index: yok

5. **offers** - Teklifler
   - Primary Key: id
   - Foreign Key: elevator_id → elevators(id) SET NULL
   - Index: yok

6. **offer_items** - Offer Kalemleri
   - Primary Key: id
   - Foreign Key: offer_id → offers(id) CASCADE DELETE
   - Foreign Key: part_id → parts(id)
   - Index: offer_id, part_id

7. **file_attachments** - Dosya Ekleri
   - Primary Key: id
   - Foreign Key: uploaded_by_user_id → users(id) SET NULL
   - Index: (entity_type, entity_id)

8. **audit_logs** - Inspection Kayıtları
   - Primary Key: id
   - Foreign Key: user_id → users(id) SET NULL
   - Index: user_id, (entity_type, entity_id), created_at

### Flyway Migrations

**V1__init_schema.sql**
- Tüm tabloları Creates
- Foreign key constraint'leri tanımlar
- Index'leri Creates
- CHECK constraint'leri ekler (role, Status, entity_type)

**V2__seed_data.sql**
- Development için seed data
- Default PATRON User (username: patron, password: password)
- Örnek asansörler
- Örnek parçalar

**Migration Stratejisi:**
- Baseline on migrate: true
- Locations: classpath:db/migration
- Hibernate ddl-auto: validate (schema değişikliğine izin vermez, sadece validate eder)

---

## 9) Ready-for-Frontend Notes

### Authentication Operations

**Login:**
1. POST /api/auth/login endpoint'ine istek gönder
2. Request body: `{"username": "patron", "password": "password"}`
3. Response'dan `accessToken` ve `refreshToken` al
4. Token'ları localStorage/sessionStorage'da sakla
5. Her request'te Authorization header'ına ekle: `Authorization: Bearer {accessToken}`

**Register:**
1. POST /api/auth/register endpoint'ine istek gönder
2. Request body: `{"username": "user", "password": "pass", "role": "PERSONEL"}`
3. Automatic login yapılır, Token'ları al

**Token Yenileme:**
1. Access Token Duration When expired POST /api/auth/refresh gönder
2. Request body: `{"refreshToken": "{refreshToken}"}`
3. Yeni accessToken al

**Logout:**
1. POST /api/auth/logout (opsiyonel)
2. Client-side Token'ları sil

### Elevator Verilerini Getirme

**Tüm Asansörler:**
- GET /api/elevators
- Authorization header gerekli
- Response: `{"success": true, "data": [...], "message": null, "errors": null}`

**Elevator Details:**
- GET /api/elevators/{id}
- Authorization header gerekli

**Elevator Durumu:**
- GET /api/elevators/{id}/status
- Response: `{"status": "OK|WARNING|EXPIRED", "daysLeft": 181, "kimlikNo": "...", "binaAdi": "...", "bitisTarihi": "2026-07-10"}`

### Uyarıları Gösterme

**Uyarılar:**
- GET /api/warnings?type=EXPIRED (Duration dolmuş)
- GET /api/warnings?type=WARNING (30 gün içinde dolacak)
- GET /api/warnings (her ikisi)
- Response: ElevatorDto listesi

**Dashboard'da Show:**
- GET /api/dashboard/summary
- Response: `{"totalElevators": 5, "totalMaintenances": 10, "totalIncome": 5000.0, "totalDebt": 2000.0, "expiredCount": 1, "warningCount": 2}`

### Maintenance Kaydı Oluşturma

**Yeni Maintenance:**
- POST /api/maintenances
- Request body:
```json
{
  "elevatorId": 1,
  "Date": "2026-01-10",
  "aciklama": "Periodic Maintenance",
  "teknisyenUserId": 2,
  "ucret": 500.0,
  "odendi": false
}
```
- Authorization header gerekli

**Maintenance Listesi:**
- GET /api/maintenances
- GET /api/maintenances/elevator/{elevatorId} (asansöre göre)

### Payment İşaretleme

**Payment Olarak İşaretle:**
- POST /api/maintenances/{id}/mark-paid?paid=true
- Authorization header gerekli
- Response: Güncellenmiş MaintenanceDto (odemeTarihi Automatic bugün olur)

**Payment İşaretini Kaldır:**
- POST /api/maintenances/{id}/mark-paid?paid=false
- odemeTarihi null olur

### PDF Üretimi

**Not**: PDF üretimi için endpoint henüz yok. Backend'e eklenmeli:
- GET /api/elevators/{id}/pdf (Elevator raporu)
- GET /api/maintenances/{id}/pdf (Maintenance raporu)
- GET /api/offers/{id}/pdf (Offer PDF'i)

Frontend'den backend'e istek atılacak, backend PDF üretip dönecek veya dosya olarak indirilecek.

### Response Format

**Başarılı Response:**
```json
{
  "success": true,
  "message": "Operation başarılı",
  "data": {...},
  "errors": null
}
```

**Error Response:**
```json
{
  "success": false,
  "message": "Error mesajı",
  "data": null,
  "errors": {...}
}
```

### Error Handling

- HTTP 400: Bad Request (validation hatası, iş kuralı hatası)
- HTTP 401: Unauthorized (Token yok veya geçersiz)
- HTTP 403: Forbidden (yetki yok)
- HTTP 404: Not Found (Record bulunamadı)
- HTTP 500: Internal Server Error (sunucu hatası)

### CORS

- Tüm origin'lere izin verilmiş (*)
- Tüm HTTP metodlarına izin verilmiş
- Authorization header exposed

### IMPORTANT Notes

1. **Context Path**: `/api` - Tüm endpoint'ler `/api` ile başlar
2. **Port**: 8080 (container'da), 8081 (host'ta map edilmiş)
3. **Base URL**: `http://localhost:8081/api`
4. **Token Duration**: Access Token 1 saat, refresh Token 7 gün
5. **Token Format**: `Authorization: Bearer {Token}` header'ında
6. **Date Format**: ISO 8601 (YYYY-MM-DD)
7. **DateTime Format**: ISO 8601 (YYYY-MM-DDTHH:mm:ss)

---

**Rapor Tarihi**: 2026-01-10
**Backend Version**: 1.0.0
**Spring Boot Version**: 3.2.0
**Java Version**: 17

