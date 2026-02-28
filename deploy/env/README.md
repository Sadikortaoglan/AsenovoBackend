# Production Env Templates

Bu klasörde gerçek secret yoktur. Sadece şablon bulunur.

## Backend

Kaynak dosya:
- `backend.prod.env.example`

Sunucuda kullanım:

```bash
cp deploy/env/backend.prod.env.example /home/ec2-user/sara-backend/.env
# sonra .env içindeki REPLACE_* alanlarını gerçek değerlerle doldur
```

## Frontend

Kaynak dosya:
- `frontend.prod.env.example`

Build pipeline veya sunucu env'de:

```bash
VITE_API_BASE_URL=https://api.asenovo.com/api
```

## Güvenlik Notu

- Gerçek `.env` dosyaları git'e commit edilmez.
- Secretlar sadece sunucuda, CI secret manager'da veya vault'ta tutulur.
