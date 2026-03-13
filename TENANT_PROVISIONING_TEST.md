Evet. Aşağıdaki senaryo setiyle hem manuel hem otomasyon testini net yapabilirsin.

1) Ön koşullar
   Uygulamayı migration’larla ayağa kaldır.
   V28 ve V29 çalışmış olmalı.
   ROLE_SYSTEM_ADMIN token hazır olmalı.
   Worker açık olmalı (app.tenancy.provisioning.worker.enabled=true).
2) En kritik uçtan uca senaryo (happy path)
   POST /api/system-admin/tenants çağır.
   Beklenen: 202 Accepted, tenant.status=PENDING, schemaName=null, job.status=PENDING.
   Hemen DB’de kontrol et: schema henüz oluşmamış olmalı.
   5-10 sn bekle (worker poll).
   GET /api/system-admin/tenant-jobs/{jobId} çağır.
   Beklenen: status=COMPLETED.
   GET /api/system-admin/tenants/{tenantId} çağır.
   Beklenen: status=ACTIVE, schemaName=tenant_<id>.
3) DB doğrulama sorguları
   select id, subdomain, status, schema_name, license_start_date, license_end_date
   from tenants
   order by id desc;

select id, tenant_id, job_type, status, retry_count, error_message, requested_at, started_at, finished_at
from tenant_provisioning_jobs
order by id desc;

select id, tenant_id, job_id, action, message, created_at
from tenant_provisioning_audit_logs
order by id desc;
Schema var mı:

select schema_name
from information_schema.schemata
where schema_name = 'tenant_<tenantId>';
4) Negatif/failure senaryoları
   create çağrısında sadece initialAdminUsername gönder, initialAdminPassword gönderme.
   Beklenen: job FAILED, tenant PROVISIONING_FAILED, audit log’da failure mesajı.
   GET /api/system-admin/tenants?query=... çağrısında artık lower(bytea) hatası olmamalı.
   Beklenen: liste normal dönmeli.
5) Güvenlik senaryoları
   STAFF_ADMIN token ile GET /api/system-admin/tenants.
   Beklenen: 403.
   SYSTEM_ADMIN token ile aynı çağrı.
   Beklenen: 200.
6) Runtime tenant resolver/lisans senaryoları
   Yeni tenant PENDING iken tenant-plane endpoint çağır (Host: <subdomain>.asenovo.local).
   Beklenen: TENANT_PENDING.
   Tenant’ı suspend et (POST /api/system-admin/tenants/{id}/suspend), job tamamlanınca tenant-plane endpoint çağır.
   Beklenen: TENANT_SUSPENDED.
   Lisans sonu geçmiş tenant için tenant-plane endpoint çağır.
   Beklenen: TENANT_EXPIRED.
   Unknown subdomain çağrısı.
   Beklenen: TENANT_NOT_FOUND.
7) Lifecycle senaryoları
   POST /api/system-admin/tenants/{id}/extend-license ile ileri tarih ver.
   Beklenen: job COMPLETED, tenant lisans tarihi güncel.
   POST /api/system-admin/tenants/{id}/activate.
   Beklenen: job COMPLETED, tenant ACTIVE.
   Delete akışını (soft) job ile test et (DELETE_TENANT job).
   Beklenen: tenant DELETED, schema fiziksel olarak drop edilmez.
8) cURL hızlı test örnekleri
   TOKEN="<SYSTEM_ADMIN_JWT>"

curl -X POST "http://localhost:8080/api/system-admin/tenants" \
-H "Authorization: Bearer $TOKEN" \
-H "Content-Type: application/json" \
-d '{
"companyName":"Yeni Cari",
"subdomain":"yeni",
"planType":"BASIC",
"licenseStartDate":"2026-03-13",
"licenseEndDate":"2026-04-13",
"initialAdminUsername":"yeni_admin",
"initialAdminPassword":"Password123!"
}'
curl "http://localhost:8080/api/system-admin/tenants" \
-H "Authorization: Bearer $TOKEN"
curl "http://localhost:8080/api/b2bunits" \
-H "Host: yeni.asenovo.local" \
-H "Authorization: Bearer $TOKEN"
9) Otomatik test çalıştırma (önerilen)
   Sadece bu alanı koştur:

./mvnw -Dtest=TenantManagementServiceTest,TenantProvisioningServiceTest,TenantProvisioningWorkerTest,TenantResolverFilterValidationTest,SystemAdminTenantControllerSecurityTest test
İstersen bir sonraki adımda bunları CI için tek bir “tenant-control-plane regression checklist” formatına da çevireyim.