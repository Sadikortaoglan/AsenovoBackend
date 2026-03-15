# Marketing Trial Runbook

## Amaç

Marketing website uzerinden acilan demo/trial ortamlarinin:

- duplicate istekleri yeni tenant acmadan mevcut demoya yonlendirmesi
- sureli calismasi
- suresi dolunca pasife alinmasi
- grace period sonunda temizlenmesi

icin operasyon notlari.

## Canli DNS mantigi

Her yeni demo icin Route53'te tek tek kayit acilmaz.

Kullanilacak model:

- `*.asenovo.com`
veya tercihen
- `*.demo.asenovo.com`

tek wildcard DNS kaydi ile tum demo subdomainleri ayni ingress/proxy'ye yonlenir.

Ornek:

- `demo-abc.demo.asenovo.com`
- `demo-xyz.demo.asenovo.com`

ayri Route53 kaydi gerektirmez.

## Uygulama davranisi

- local ortamda `asenovo.marketing.app-url-template=http://{tenant}.lvh.me:5173/login`
- canli ortamda `asenovo.marketing.app-url-template=https://{tenant}.asenovo.com/login`
- local ortamda `asenovo.marketing.mail-enabled=false`
- canli ortamda `asenovo.marketing.mail-enabled=true`

Bu sayede login URL environment'a gore dogru uretilir.

Ek guard:

- `local/staging/test` ortaminda `.com` demo domain uretilirse backend istegi bloklar
- `prod` ortaminda `.local` demo domain uretilirse backend istegi bloklar
- `local/staging/test` ortaminda demo DB prefix'i non-prod olmak zorundadir

## Trial duplicate kurali

Ayni:

- email + company
veya
- email + phone

ile acik trial varsa:

- yeni tenant acilmaz
- mevcut demo bilgileri dondurulur
- gerekiyorsa gecici sifre yenilenir
- erisim maili tekrar gonderilir

## Trial lifecycle

Varsayilanlar:

- aktif demo suresi: 7 gun
- cleanup grace suresi: 7 gun

Akis:

1. `READY` durumda aktif demo calisir
2. `expires_at` gecince tenant/subscription pasife alinir
3. grace period bitince dedicated DB silinir
4. request durumu `CLEANED_UP` olur

## Gerekli prod env degerleri

```env
ASENOVO_MARKETING_ENVIRONMENT=prod
ASENOVO_MARKETING_MAIL_ENABLED=true
ASENOVO_MARKETING_APP_URL_TEMPLATE=https://{tenant}.asenovo.com/login
ASENOVO_MARKETING_SUPPORT_EMAIL=support@asenovo.com
ASENOVO_MARKETING_FROM_EMAIL=no-reply@asenovo.com
ASENOVO_MARKETING_TRIAL_CLEANUP_GRACE_DAYS=7
ASENOVO_MARKETING_TRIAL_EXPIRE_CRON=0 0 4 * * *
ASENOVO_MARKETING_TRIAL_CLEANUP_CRON=0 30 4 * * *
ASENOVO_MARKETING_DEMO_DB_ADMIN_URL=
ASENOVO_MARKETING_DEMO_DB_ADMIN_USERNAME=
ASENOVO_MARKETING_DEMO_DB_ADMIN_PASSWORD=
ASENOVO_MARKETING_DEMO_DB_NAME_PREFIX=asenovo_demo_
ASENOVO_MARKETING_DEMO_DB_TEMPLATE_NAME=asenovo_demo_template
```

## SES / SMTP notu

Mail hatasi demo olusumunu bozmamali.

Beklenen urun davranisi:

- demo `READY` ise kullanici bilgilere ekrandan ulasabilir
- mail gitmese bile akış fail olmaz
- FE sadece `accessEmailSent=false` ise uyarı gosterir

AWS SES kullaniliyorsa:

- sender domain verify edilmeli
- gerekiyorsa alici/sandbox kisitlari kaldirilmali

## Local notu

Local varsayilanlari:

```env
ASENOVO_MARKETING_ENVIRONMENT=local
ASENOVO_MARKETING_MAIL_ENABLED=false
ASENOVO_MARKETING_APP_URL_TEMPLATE=http://{tenant}.lvh.me:5173/login
ASENOVO_MARKETING_DEMO_DB_NAME_PREFIX=asenovo_demo_local_
```

Bu durumda:

- local demo local DB'de acilir
- mail gercek kullaniciya gitmez
- login URL icin hosts kaydi gerekmez
- erisim bilgileri sadece ekranda kullanilir

`/etc/hosts` wildcard desteklemez.

Bu nedenle local default olarak `lvh.me` kullanilir. `lvh.me` localhost'a cozuldugu icin her yeni demo icin tek tek hosts kaydi gerekmez.

Canli tarafta bu problem wildcard DNS ile cozulecegi icin her tenant icin tek tek Route53 kaydi acilmaz.
