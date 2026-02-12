# Production Hard Reset - Komutlar

EC2 production sunucusunda hard reset yapmak için:

## ⚠️ DİKKAT: Hard reset TÜM local değişiklikleri KALICI olarak siler!

### Güvenli Hard Reset (Önerilen)

```bash
# 1. .env.production'ı yedekle (MUTLAKA!)
cp .env.production .env.production.backup

# 2. Hard reset yap (tüm local değişiklikleri sil)
git reset --hard origin/main

# 3. Untracked dosyaları temizle (.env.production dahil)
git clean -fd

# 4. Pull yap (güncel kodu al)
git pull

# 5. .env.production'ı geri koy (eğer git'te yoksa)
if [ ! -f .env.production ]; then
    cp .env.production.backup .env.production
    echo "✅ .env.production geri yüklendi"
else
    echo "✅ .env.production git'ten geldi"
    rm .env.production.backup
fi
```

### Tek Komut (Hızlı)

```bash
# .env.production'ı yedekle ve hard reset yap
cp .env.production .env.production.backup && \
git reset --hard origin/main && \
git clean -fd && \
git pull && \
[ ! -f .env.production ] && cp .env.production.backup .env.production || echo ".env.production git'ten geldi"
```

### Adım Adım Açıklama

1. **`cp .env.production .env.production.backup`** - .env.production'ı yedekle
2. **`git reset --hard origin/main`** - Tüm local değişiklikleri sil, origin/main'e reset et
3. **`git clean -fd`** - Untracked dosyaları ve klasörleri sil
4. **`git pull`** - Güncel kodu çek
5. **`.env.production kontrolü`** - Eğer git'te yoksa yedekten geri yükle

### Sonuç

- ✅ Tüm local değişiklikler silindi
- ✅ .env.production korundu (yedekten geri yüklendi)
- ✅ Kod git'ten gelen en güncel versiyon
