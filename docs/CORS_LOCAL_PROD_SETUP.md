# CORS Local ve Prod Kurulum Notu (Multi-Tenant)

## 1) Local (subdomain ile geliştirme)

`/etc/hosts`:

```
127.0.0.1 default.sara.local
127.0.0.1 acme.sara.local
```

`.env.local` (backend):

```
CORS_ALLOWED_ORIGINS=http://*.sara.local:5173,http://localhost:5173,http://127.0.0.1:5173
```

Not:
- Backend CORS tarafı wildcard pattern destekler (`setAllowedOriginPatterns`).
- Bu ayar local tenant subdomain testleri için uygundur.

## 2) Prod (önerilen güvenli model)

FE tenant domain:
- `https://<tenant>.app.yourdomain.com`

Backend env:

```
CORS_ALLOWED_ORIGINS=https://*.app.yourdomain.com
```

Not:
- `http` değil `https` kullanın.
- Ana kök domaine kör wildcard vermeyin (`*.yourdomain.com` yerine `*.app.yourdomain.com`).
- Yalnızca FE’nin gerçekten yayınlandığı originleri açın.

## 3) FE ekibine verilecek teknik gereksinimler

1. API base URL host bazlı dinamik olmalı, hardcode olmamalı.
2. Her istekte `Authorization: Bearer <token>` gönderilmeli.
3. Tenant host değiştiğinde token temizlenip yeniden login yapılmalı.
4. FE origini CORS allowlist’e eklenmeden login akışı çalışmaz.
5. Local testte FE şu URL’lerden açılmalı:
   - `http://default.sara.local:5173`
   - `http://acme.sara.local:5173`

## 4) Hızlı doğrulama (preflight)

```bash
curl -i -X OPTIONS "http://default.sara.local:8080/api/auth/login" \
  -H "Origin: http://acme.sara.local:5173" \
  -H "Access-Control-Request-Method: POST"
```

Beklenen:
- `200` veya `204`
- `Access-Control-Allow-Origin` header'ı dönmeli.
