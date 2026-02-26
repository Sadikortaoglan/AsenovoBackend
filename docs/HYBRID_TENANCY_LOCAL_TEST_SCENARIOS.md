# Hybrid Tenancy Local Test Senaryolari

Bu dokuman, local ortamda tek sunucuda iki farkli tenant/subdomain davranisini test etmek icin hazirlandi.

## 1) On Kosullar
- Uygulama Spring Boot olarak localde calisiyor (`http://localhost:8080/api`).
- PostgreSQL calisiyor.
- `V10` ve `V11` migration'lari uygulanmis durumda.

## 2) Tenant Kayitlarini Hazirlama
Asagidaki SQL ile bir `SHARED_SCHEMA` ve bir `DEDICATED_DB` tenant olustur.

```sql
-- PLANLAR (yoksa olustur)
INSERT INTO plans (code, plan_type, max_users, max_assets, api_rate_limit_per_minute, max_storage_mb, priority_support, active)
VALUES ('PRO-DEFAULT', 'PRO'::plan_type, 25, 250, 1000, 1024, false, true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO plans (code, plan_type, max_users, max_assets, api_rate_limit_per_minute, max_storage_mb, priority_support, active)
VALUES ('ENTERPRISE-DEFAULT', 'ENTERPRISE'::plan_type, 500, 50000, 5000, 10240, true, true)
ON CONFLICT (code) DO NOTHING;

-- SHARED_SCHEMA tenant
INSERT INTO tenants (name, subdomain, tenancy_mode, schema_name, redis_namespace, plan_id, active)
VALUES (
  'Acme Shared',
  'acme',
  'SHARED_SCHEMA',
  'tenant_acme',
  'tenant:acme',
  (SELECT id FROM plans WHERE code='PRO-DEFAULT' LIMIT 1),
  true
)
ON CONFLICT (subdomain) DO NOTHING;

-- DEDICATED_DB tenant
INSERT INTO tenants (name, subdomain, tenancy_mode, db_host, db_name, db_username, db_password, redis_namespace, plan_id, active)
VALUES (
  'Enterprise X',
  'enterprise',
  'DEDICATED_DB',
  'localhost:5433',
  'sara_enterprise',
  'sara_asansor',
  'sara_asansor',
  'tenant:enterprise',
  (SELECT id FROM plans WHERE code='ENTERPRISE-DEFAULT' LIMIT 1),
  true
)
ON CONFLICT (subdomain) DO NOTHING;
```

> Not: Dedicated DB icin `sara_enterprise` veritabaninin erisilebilir oldugundan emin ol.

## 3) Subdomain Simulasyonu

### Yontem A - Hosts dosyasi olmadan (onerilen)
`Host` header kullan:

```bash
curl -i http://localhost:8080/api/health -H "Host: default.localtest.me"
curl -i http://localhost:8080/api/health -H "Host: acme.localtest.me"
curl -i http://localhost:8080/api/health -H "Host: enterprise.localtest.me"
```

### Yontem B - Tarayicidan test
`/etc/hosts`:

```txt
127.0.0.1 default.sara.local
127.0.0.1 acme.sara.local
127.0.0.1 enterprise.sara.local
```

Sonra:
- `http://default.sara.local:8080/api/...`
- `http://acme.sara.local:8080/api/...`
- `http://enterprise.sara.local:8080/api/...`

## 4) Test Senaryolari

### Senaryo 1 - Fallback davranisi (single-tenant)
- Istek: `Host: localhost`
- Beklenen: uygulama default/public semada calisir, 200 doner.

### Senaryo 2 - Bilinmeyen tenant
- Istek: `Host: unknown.localtest.me`
- Beklenen: `TENANT_NOT_FOUND` + 4xx.

### Senaryo 3 - Shared schema tenant migration
- Tenant: `acme` (`schema_name=tenant_acme`)
- Beklenen:
  - startup'ta `tenant_acme` schema olusur,
  - Flyway migration bu schema icin calisir,
  - `Host: acme...` istekleri bu schemaya yonlenir.

### Senaryo 4 - Dedicated DB tenant migration
- Tenant: `enterprise` (`tenancy_mode=DEDICATED_DB`)
- Beklenen:
  - startup'ta dedicated DB icin Flyway migration calisir,
  - `Host: enterprise...` istekleri dedicated DB'ye gider.

### Senaryo 5 - Izolasyon
- `acme` altinda olusan veri `default` veya `enterprise` altinda gorunmemeli.
- Beklenen: cross-tenant leak yok.

### Senaryo 6 - ThreadLocal temizligi
- Sirali istek at:
  1. `Host: acme...`
  2. `Host: default...`
- Beklenen: ikinci istekte ilk tenant context'i tasinmaz.

## 5) Ornek Curl Akisi

```bash
# 1) login (default)
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H "Host: default.localtest.me" \
  -H "Content-Type: application/json" \
  -d '{"username":"patron","password":"password"}'

# 2) ayni endpointi iki farkli tenantta cagir
curl -i http://localhost:8080/api/elevators -H "Host: acme.localtest.me" -H "Authorization: Bearer <TOKEN>"
curl -i http://localhost:8080/api/elevators -H "Host: enterprise.localtest.me" -H "Authorization: Bearer <TOKEN>"
```

## 6) Yararlı Config Flag'leri
- `TENANCY_MIGRATION_ENABLED=true|false`
- `TENANCY_MIGRATION_INCLUDE_SHARED_SCHEMAS=true|false`
- `TENANCY_MIGRATION_INCLUDE_DEDICATED_DBS=true|false`
- `TENANCY_DEFAULT_SHARED_SCHEMA=public`

Bu flag'lerle localde migration kapsamını kontrollu sekilde acip kapatabilirsin.
