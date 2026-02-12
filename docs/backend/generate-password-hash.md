# Password Hash Generator

Patron kullanıcısı için doğru BCrypt hash oluşturmak için:

## Option 1: Java ile (Backend'te)

Backend'te bir test class'ı oluşturun veya mevcut bir class'ta main method ekleyin:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password";
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
    }
}
```

## Option 2: Online BCrypt Generator

https://bcrypt-generator.com/ adresini kullanarak "password" için hash oluşturun.

Round: 10
Password: password

## Option 3: Database'de Direkt Güncelleme

Eğer database çalışıyorsa, backend üzerinden yeni bir user oluşturup hash'ini alabilirsiniz, veya:

```sql
-- Yeni hash ile güncelle (hash'i backend'ten alın)
UPDATE users SET password_hash = '$2a$10$NEW_HASH_HERE' WHERE username = 'patron';
```

## Mevcut Hash Kontrolü

V2__seed_data.sql'deki hash: `$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi`

Bu hash'in "password" ile eşleşip eşleşmediğini kontrol etmek için backend'te:

```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
boolean matches = encoder.matches("password", "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi");
System.out.println("Matches: " + matches);
```

