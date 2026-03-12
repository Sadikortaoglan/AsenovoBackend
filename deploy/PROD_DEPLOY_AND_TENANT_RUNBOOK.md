# Production Deploy ve Tenant Provision Runbook

## A) Backend deploy (EC2 + Docker Compose)

```bash
ssh -i saraasansor.pem ec2-user@51.21.3.85
cd ~/sara-backend
cp deploy/env/backend.prod.env.example .env.prod
# .env.prod içindeki REPLACE_* değerlerini gerçek değerlerle doldur
./deploy/scripts/deploy_backend_prod.sh ~/sara-backend http://127.0.0.1:8080/api/health
```

Alternatif manuel:

```bash
cd ~/sara-backend
git pull
docker compose --env-file .env.prod -f docker-compose.prod.yml down --remove-orphans
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f app
```

## B) Frontend deploy (local build -> EC2 nginx)

```bash
cd SarAsansorDesktop
npm run build
scp -i saraasansor.pem -r dist ec2-user@51.21.3.85:/home/ec2-user/
ssh -i saraasansor.pem ec2-user@51.21.3.85
sudo rm -rf /usr/share/nginx/html/*
sudo mv /home/ec2-user/dist/* /usr/share/nginx/html/
sudo chmod -R 755 /usr/share/nginx/html
sudo systemctl restart nginx
```

Frontend prod env:

```env
VITE_API_BASE_URL=https://api.asenovo.com/api
```

## B2) Sıfırdan canlı DB reset (TÜM VERİ SİLİNİR)

Canlı DB tamamen sıfırlanacaksa:

```bash
ssh -i saraasansor.pem ec2-user@51.21.3.85
cd ~/sara-backend
chmod +x deploy/scripts/reset_prod_database.sh
./deploy/scripts/reset_prod_database.sh ~/sara-backend http://127.0.0.1:8080/api/health
```

Beklenen env:

```env
POSTGRES_DB=asenovo
POSTGRES_USER=asenovo
POSTGRES_PASSWORD=asenovo767
```

Not:
- Bu işlem PostgreSQL volume'ünü siler.
- Uygulama tekrar kalkarken Flyway migration'ları baştan çalışır.
- Bu repodaki mevcut migration seti fresh DB üzerinde başlangıç verilerini yeniden oluşturur.

## C) Tenant açma (sara/test)

Not: `PGPASSWORD` export edilmeden script çalışmaz.

```bash
cd ~/sara-backend
export PGPASSWORD='DB_PASSWORD'

# sara tenant
./deploy/scripts/provision_shared_tenant.sh \
  --db-host DB_HOST \
  --db-port 5432 \
  --db-name asenovo \
  --db-user asenovo \
  --tenant-subdomain sara \
  --tenant-name 'Sara Tenant' \
  --tenant-schema tenant_sara

# test tenant
./deploy/scripts/provision_shared_tenant.sh \
  --db-host DB_HOST \
  --db-port 5432 \
  --db-name asenovo \
  --db-user asenovo \
  --tenant-subdomain test \
  --tenant-name 'Test Tenant' \
  --tenant-schema tenant_test
```

## D) Doğrulama

```bash
curl -i https://api.asenovo.com/api/health
```

```sql
select id,name,subdomain,tenancy_mode,schema_name,active from tenants order by id;
select schema_name from information_schema.schemata where schema_name in ('public','tenant_sara','tenant_test');
```

## E) Güvenlik kontrolü

- SG: `8080` public açık olmamalı.
- SG: `5432` public açık olmamalı.
- Public açık portlar: `80/443` ve kısıtlı `22`.
- Backend CORS:

```env
CORS_ALLOWED_ORIGINS=https://*.asenovo.com
```
