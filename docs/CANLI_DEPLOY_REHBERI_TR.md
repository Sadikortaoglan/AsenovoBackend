# Asenovo Canlı Ortam Deploy Rehberi (EC2 + Nginx + Docker)

Bu doküman, aşağıdaki mimari için hazırlanmıştır:
- Backend: Spring Boot (Docker, `localhost:8080`)
- Veritabanı: PostgreSQL (Docker)
- Frontend: React (Vite), Docker'da değil
- Nginx: Host makinede, `80/443`
- Domain: `asenovo.com`
- API path: `/api`

---

## 1) Hedef Mimari (Doğru Kurulum)

- `asenovo.com` -> Frontend (Nginx static)
- `www.asenovo.com` -> Frontend
- `api.asenovo.com` -> Nginx üzerinden backend proxy
- Backend container portu dış dünyaya açık **olmamalı** (`127.0.0.1:8080:8080`)
- PostgreSQL dış dünyaya açık **olmamalı**

> Beyaz ekran sorunu çoğunlukla: yanlış Vite `base`, yanlış Nginx `try_files`, eksik static dosya kopyalama veya yanlış API env nedeniyle oluşur.

---

## 2) EC2 Dizin Yapısı

```text
/opt/asenovo/backend/                    # backend repo
/opt/asenovo/backend/.env               # canlı backend env
/opt/asenovo/backend/docker-compose.prod.yml

/var/www/asenovo/releases/              # frontend release klasörleri
/var/www/asenovo/releases/2026.../
/var/www/asenovo/current -> /var/www/asenovo/releases/2026...  (symlink)
```

---

## 3) Nginx Konfigürasyonu

Dosya: `/etc/nginx/conf.d/asenovo.conf`

```nginx
upstream asenovo_backend {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name asenovo.com www.asenovo.com;

    root /var/www/asenovo/current;
    index index.html;

    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header X-XSS-Protection "1; mode=block" always;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss image/svg+xml;
    gzip_min_length 1024;

    location /api/ {
        proxy_pass http://asenovo_backend;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;

        proxy_connect_timeout 30s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /assets/ {
        try_files $uri =404;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    location / {
        try_files $uri $uri/ /index.html;
    }

    location ~ /\.(?!well-known).* {
        deny all;
    }
}
```

Konfig test + reload:

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 4) Vite Canlı Ayarı

### `vite.config.ts`

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: 'dist',
    sourcemap: false
  }
})
```

### `.env.production`

```env
VITE_API_BASE_URL=/api
```

> Nginx `/api`'yi backend'e proxy ettiği için frontend tarafında en sağlıklısı budur.

---

## 5) Spring Boot Canlı Ayarı

Canlı env (`/opt/asenovo/backend/.env`) örnek:

```env
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://REPLACE_DB_HOST:5432/sara
SPRING_DATASOURCE_USERNAME=sara
SPRING_DATASOURCE_PASSWORD=REPLACE_STRONG_DB_PASSWORD
JWT_SECRET=REPLACE_MIN_32_CHAR_SECRET
QR_SECRET_KEY=REPLACE_MIN_32_CHAR_SECRET
CORS_ALLOWED_ORIGINS=https://asenovo.com,https://*.asenovo.com
TENANCY_MIGRATION_ENABLED=true
TENANCY_MIGRATION_INCLUDE_SHARED_SCHEMAS=true
TENANCY_MIGRATION_INCLUDE_DEDICATED_DBS=true
```

`application-prod.yml` için öneri:

```yaml
server:
  forward-headers-strategy: framework
```

> Backend context-path `/api` ise Nginx config bu haliyle uyumludur.

---

## 6) Docker Compose Notu (Kritik)

Canlıda backend port bind **localhost olmalı**:

```yaml
services:
  app:
    ports:
      - "127.0.0.1:8080:8080"
```

Postgres public açılmamalı. Mümkünse `ports` kaldırın; gerekiyorsa sadece localhost bind edin.

---

## 7) Backend Deploy Adımları (EC2)

```bash
chmod 400 saraasansor.pem
ssh -i saraasansor.pem ec2-user@51.21.3.85

cd /opt/asenovo/backend
git pull origin main
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f app
```

Health kontrol:

```bash
curl -i http://127.0.0.1:8080/api/health
curl -i http://api.asenovo.com/api/health
```

---

## 8) Frontend Deploy Adımları

### Local build

```bash
cd /path/to/frontend
npm ci
npm run build
```

### Dist dosyalarını EC2'ye kopyala

```bash
scp -i saraasansor.pem -r dist ec2-user@51.21.3.85:/tmp/asenovo-dist
```

### EC2'de release al ve Nginx'i yenile

```bash
ssh -i saraasansor.pem ec2-user@51.21.3.85
TS=$(date +%Y%m%d%H%M%S)
sudo mkdir -p /var/www/asenovo/releases/$TS
sudo cp -r /tmp/asenovo-dist/* /var/www/asenovo/releases/$TS/
sudo ln -sfn /var/www/asenovo/releases/$TS /var/www/asenovo/current
sudo chown -R nginx:nginx /var/www/asenovo
sudo find /var/www/asenovo -type d -exec chmod 755 {} \;
sudo find /var/www/asenovo -type f -exec chmod 644 {} \;
sudo nginx -t
sudo systemctl reload nginx
```

---

## 9) Multi-Tenant İlk Kurulum (sara + test)

Önce backup al:

```bash
docker exec -i sara-asansor-postgres-prod pg_dump -U sara -d sara > pre_hybrid_$(date +%F_%H%M).sql
```

Tenant açmak için (script ile):

```bash
cd /opt/asenovo/backend
export PGPASSWORD='DB_PASSWORD'

./deploy/scripts/provision_shared_tenant.sh \
  --db-host DB_HOST \
  --db-port 5432 \
  --db-name sara \
  --db-user sara \
  --tenant-subdomain sara \
  --tenant-name 'Sara Tenant' \
  --tenant-schema tenant_sara

./deploy/scripts/provision_shared_tenant.sh \
  --db-host DB_HOST \
  --db-port 5432 \
  --db-name sara \
  --db-user sara \
  --tenant-subdomain test \
  --tenant-name 'Test Tenant' \
  --tenant-schema tenant_test
```

Kontrol:

```sql
select id,name,subdomain,tenancy_mode,schema_name,active from tenants order by id;
select schema_name from information_schema.schemata where schema_name in ('public','tenant_sara','tenant_test');
```

---

## 10) HTTPS (Opsiyonel ama önerilir)

```bash
sudo dnf install -y certbot python3-certbot-nginx
sudo certbot --nginx -d asenovo.com -d www.asenovo.com
sudo certbot renew --dry-run
```

Wildcard (`*.asenovo.com`) için ACM + ALB/CloudFront daha uygundur. EC2 üstü certbot ile wildcard almak için DNS challenge gerekir.

---

## 11) SG (Security Group) Kuralları

- Açık olmalı:
  - `80/tcp` -> `0.0.0.0/0`
  - `443/tcp` -> `0.0.0.0/0`
  - `22/tcp` -> sadece sizin ofis/IP (`x.x.x.x/32`)
- Kapalı olmalı:
  - `8080` public
  - `5432` public

---

## 12) Beyaz Ekran Hata Ayıklama

```bash
curl -I http://asenovo.com
curl -s http://asenovo.com | head -n 40
curl -I http://asenovo.com/assets/
sudo tail -f /var/log/nginx/error.log
sudo tail -f /var/log/nginx/access.log
```

Tarayıcıda:
- Console'da JS hatası var mı?
- Network'te `/assets/*.js` 200 dönüyor mu?
- `/api/...` çağrıları doğru hosta gidiyor mu?

