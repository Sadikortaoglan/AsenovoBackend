# Pull Sonrası Kurulum Checklist (Hybrid Tenancy)

Bu dosya, `hybrid-tenancy` branch'ini çeken geliştirici için minimum adımları içerir.

## 1) Kodu al

```bash
git fetch --all
git checkout hybrid-tenancy
git pull
```

## 2) Local env ayarları

Proje kökünde `.env.local` (veya IntelliJ Env Vars) içinde en az şu değişkenler olmalı:

```env
SPRING_PROFILES_ACTIVE=dev
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/sara_asansor
SPRING_DATASOURCE_USERNAME=sara_asansor
SPRING_DATASOURCE_PASSWORD=sara_asansor
JWT_SECRET=change-this-min-32-char-secret-key
QR_SECRET_KEY=change-this-min-32-char-secret-key
CORS_ALLOWED_ORIGINS=http://*.sara.local:5173,http://localhost:5173,http://127.0.0.1:5173
```

## 3) Hosts kayıtları

`/etc/hosts` dosyasına ekle:

```txt
127.0.0.1 default.sara.local
127.0.0.1 acme.sara.local
```

## 4) PostgreSQL çalıştır

```bash
docker compose up -d postgres
```

## 5) Uygulamayı çalıştır

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

Beklenen: Flyway migration'lar geçmeli ve uygulama `/api` context path ile açılmalı.

## 6) Tenant kayıtlarını doğrula (DB)

```sql
select id,name,subdomain,tenancy_mode,schema_name,active from tenants order by id;
select schema_name from information_schema.schemata where schema_name in ('public','tenant_acme');
```

Beklenen: `default` ve `acme` tenantları, `public` ve `tenant_acme` şemaları.

## 7) CORS preflight testi

```bash
curl -i -X OPTIONS "http://default.sara.local:8080/api/auth/login" \
  -H "Origin: http://acme.sara.local:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```

Beklenen:
- HTTP `200` veya `204`
- `Access-Control-Allow-Origin`
- `Access-Control-Allow-Headers`
- `Access-Control-Allow-Methods`

## 8) FE ekibi notu

- API base URL host-aware olmalı (proxy varsa `/api`, proxy yoksa `http://<tenant>.sara.local:8080/api`).
- Token tenant bazlı tutulmalı (`auth_token_<tenant>`, `refresh_token_<tenant>`).
- Tenant host değişince eski tokenlar temizlenmeli.

