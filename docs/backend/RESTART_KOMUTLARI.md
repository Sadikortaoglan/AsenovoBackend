# Backend Restart Komutları

## Backend'i Durdur

```bash
cd backend

# Yöntem 1: Process adıyla durdur
pkill -f "SaraAsansorApiApplication"

# Yöntem 2: Port ile durdur
lsof -ti:8080 | xargs kill -9

# Yöntem 3: Her ikisi de
pkill -f "SaraAsansorApiApplication" || lsof -ti:8080 | xargs kill -9
```

## Backend'i Başlat

```bash
cd backend

# Yöntem 1: Maven ile (önerilen)
mvn spring-boot:run

# Yöntem 2: Background'da çalıştır
mvn spring-boot:run > /tmp/backend.log 2>&1 &

# Yöntem 3: JAR ile
mvn clean package -DskipTests
java -jar target/sara-asansor-api-1.0.0.jar
```

## Hızlı Restart (Durdur + Başlat)

```bash
cd backend

# Durdur
pkill -f "SaraAsansorApiApplication" || lsof -ti:8080 | xargs kill -9

# 2 saniye bekle
sleep 2

# Başlat
mvn spring-boot:run
```

## Durum Kontrolü

```bash
# Process kontrolü
ps aux | grep SaraAsansorApiApplication

# Port kontrolü
lsof -i:8080

# API test
curl http://localhost:8080/api/swagger-ui.html
```

## Önemli Not

⚠️ **Şu anda compilation hataları var**, bu yüzden backend başlamıyor. 
Önce hataları düzeltmeniz gerekiyor.

Hataları görmek için:
```bash
cd backend
mvn clean compile
```

