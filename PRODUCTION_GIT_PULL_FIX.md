# Production Git Pull Fix

EC2 production sunucusunda git pull yaparken local değişiklikler çakışıyor.

## Durum
- `src/services/fault.service.ts` - modified
- `package.json` - modified
- `.env.production` - untracked

## Çözüm

### 1. .env.production dosyasını yedekle (önemli!)
```bash
# .env.production dosyasını yedekle
cp .env.production .env.production.backup
```

### 2. Local değişiklikleri discard et
```bash
# Modified dosyaları eski haline döndür
git restore src/services/fault.service.ts
git restore package.json

# .env.production'ı geçici olarak taşı (pull sonrası geri koyacağız)
mv .env.production .env.production.local
```

### 3. Git pull yap
```bash
git pull
```

### 4. .env.production'ı geri koy (eğer gerekirse)
```bash
# Eğer .env.production dosyası git'te yoksa, local versiyonu geri koy
# Ama önce git'ten gelen versiyonu kontrol et
if [ ! -f .env.production ]; then
    mv .env.production.local .env.production
fi
```

## Alternatif: Stash kullan (daha güvenli)

Eğer değişiklikleri kaybetmek istemiyorsan:

```bash
# 1. .env.production'ı yedekle
cp .env.production .env.production.backup

# 2. .env.production'ı geçici olarak taşı
mv .env.production .env.production.local

# 3. Değişiklikleri stash'le
git stash push -m "Production local changes before pull"

# 4. Pull yap
git pull

# 5. Stash'i kontrol et (gerekirse)
git stash list

# 6. .env.production'ı geri koy
if [ ! -f .env.production ]; then
    mv .env.production.local .env.production
fi
```

## Önerilen: Production'da sadece discard

Production ortamında local değişiklikler genellikle istenmez:

```bash
# 1. .env.production'ı yedekle
cp .env.production .env.production.backup

# 2. Tüm local değişiklikleri discard et
git restore .
git clean -fd

# 3. .env.production'ı geri koy (eğer git'te yoksa)
if [ ! -f .env.production ]; then
    cp .env.production.backup .env.production
fi

# 4. Pull yap
git pull
```

## Önemli Notlar

1. **.env.production** dosyası production environment variables içeriyor olabilir, mutlaka yedekle!
2. Production'da local değişiklikler genellikle istenmez
3. Pull sonrası `.env.production` dosyasının git'te olup olmadığını kontrol et
4. Eğer `.env.production` git'te varsa, local versiyonunu sil ve git'ten gelen versiyonu kullan
