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

- local ortamda `asenovo.marketing.app-base-domain=asenovo.local`
- canli ortamda `asenovo.marketing.app-base-domain=asenovo.com`

Bu sayede login URL environment'a gore dogru uretilir.

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
ASENOVO_MARKETING_APP_BASE_DOMAIN=asenovo.com
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

`/etc/hosts` wildcard desteklemez.

Local alternatifler:

- `dnsmasq`
- `nip.io` / `sslip.io`
- tek tek hosts kaydi

Canli tarafta bu problem wildcard DNS ile cozulecegi icin her tenant icin tek tek Route53 kaydi acilmaz.
