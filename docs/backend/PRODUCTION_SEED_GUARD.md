# Production Seed Data Guard

## Critical Requirement

**Dummy / seed / test data must NEVER run in production.**
Not once, not by mistake, not by migration, not by script.

- ✅ **DEV** = Dummy data allowed
- ❌ **PROD** = Dummy data FORBIDDEN

## Protection Layers

### 1. Flyway Migration Separation

**Schema Migrations (Production):**
- Location: `db/migration/`
- Files: `V1__init_schema.sql`, `V3__...`, `V4__...`
- **Runs in PROD**: ✅ YES

**Seed Data (Development Only):**
- Location: `db/migration/dev/`
- Files: `V2__seed_data.sql`, `V7__comprehensive_seed_data.sql`
- **Runs in PROD**: ❌ NO

**Configuration:**

```yaml
# application-prod.yml
spring:
  flyway:
    locations: classpath:db/migration
    # dev/ folder is EXCLUDED - seed data NEVER runs in production

# application-dev.yml
spring:
  flyway:
    locations: classpath:db/migration,classpath:db/migration/dev
```

### 2. Application-Level Seed Flag

**application-prod.yml:**
```yaml
app:
  seed:
    enabled: false  # FORBIDDEN in production
```

**application-dev.yml:**
```yaml
app:
  seed:
    enabled: true  # Allowed in development
```

### 3. Production Seed Guard (Hard Kill Switch)

**File:** `ProductionSeedGuard.java`

**Function:**
- Monitors active Spring profile
- Checks `app.seed.enabled` flag
- Verifies Flyway locations configuration
- **Throws exception** if seed data attempted in production

**Guard Checks:**
```java
1. If SPRING_PROFILES_ACTIVE=prod AND app.seed.enabled=true
   → THROWS IllegalStateException (BLOCKED)

2. If SPRING_PROFILES_ACTIVE=prod AND spring.flyway.locations contains /dev/
   → THROWS IllegalStateException (BLOCKED)
```

### 4. Docker / Environment Safety

**docker-compose.prod.yml:**
```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod  # MUST be prod
```

**Verification:**
```bash
# Check active profile
docker exec sara-asansor-api-prod env | grep SPRING_PROFILES_ACTIVE
# Expected: SPRING_PROFILES_ACTIVE=prod
```

## Current Migration Structure

```
db/migration/
├── V1__init_schema.sql                    ✅ PROD (schema)
├── V3__add_fault_inspection_payment_tables.sql  ✅ PROD (schema)
├── V4__create_refresh_tokens_table.sql    ✅ PROD (schema)
└── dev/
    ├── V2__seed_data.sql                  ❌ PROD (seed)
    └── V7__comprehensive_seed_data.sql    ❌ PROD (seed)
```

## Verification Steps

### 1. Check Flyway Locations

**Production:**
```bash
# Check application-prod.yml
grep -A 3 "flyway:" application-prod.yml
# Expected: locations: classpath:db/migration (NO dev/)
```

**Development:**
```bash
# Check application-dev.yml
grep -A 3 "flyway:" application-dev.yml
# Expected: locations: classpath:db/migration,classpath:db/migration/dev
```

### 2. Check Seed Flag

**Production:**
```bash
grep "seed:" application-prod.yml
# Expected: enabled: false
```

**Development:**
```bash
grep "seed:" application-dev.yml
# Expected: enabled: true
```

### 3. Test Production Guard

**Start Production:**
```bash
docker-compose -f docker-compose.prod.yml up -d
```

**Check Logs:**
```bash
docker logs sara-asansor-api-prod | grep -i "seed guard"
# Expected: ✅ Production Seed Guard: ACTIVE - Seed data is BLOCKED
```

**If seed data attempted:**
```
CRITICAL ERROR: Dummy/seed data execution is BLOCKED in production...
```

### 4. Verify No Seed Data in Production

**Check Flyway Schema History:**
```bash
docker exec sara-asansor-postgres-prod psql -U sara_asansor -d sara_asansor -c \
  "SELECT version, description FROM flyway_schema_history ORDER BY installed_rank;"
```

**Verify:**
- NO migrations from `dev/` folder
- Only schema migrations (V1, V3, V4)

**Check Database:**
```bash
# Should be EMPTY (no dummy users, elevators, etc.)
docker exec sara-asansor-postgres-prod psql -U sara_asansor -d sara_asansor -c \
  "SELECT COUNT(*) FROM users WHERE username = 'patron';"
# Expected: 0 (or your actual admin user)
```

## Adding New Seed Data

### ❌ WRONG: Don't Add to Main Migration Folder

```sql
-- db/migration/V10__new_seed.sql  ❌ WRONG
INSERT INTO users ...
```

### ✅ CORRECT: Add to Dev Folder

```sql
-- db/migration/dev/V10__new_seed.sql  ✅ CORRECT
INSERT INTO users ...
```

### Naming Convention

- **Schema migrations**: `db/migration/V{N}__description.sql`
- **Seed data**: `db/migration/dev/V{N}__description.sql`

## Safety Checklist

Before deploying to production:

- [ ] `application-prod.yml`: `spring.flyway.locations` = `classpath:db/migration` (NO dev/)
- [ ] `application-prod.yml`: `app.seed.enabled: false`
- [ ] `docker-compose.prod.yml`: `SPRING_PROFILES_ACTIVE=prod`
- [ ] All seed data files are in `db/migration/dev/`
- [ ] No `CommandLineRunner` with seed data (or wrapped with `@Profile("dev")`)
- [ ] No `@PostConstruct` seed methods without `@Profile("dev")`
- [ ] Production Seed Guard is active (check logs)

## Troubleshooting

### Error: "Seed data execution is BLOCKED in production"

**Cause:** `app.seed.enabled=true` in production profile

**Fix:**
```yaml
# application-prod.yml
app:
  seed:
    enabled: false
```

### Error: "Production Flyway configuration includes dev/ folder"

**Cause:** `spring.flyway.locations` includes `classpath:db/migration/dev`

**Fix:**
```yaml
# application-prod.yml
spring:
  flyway:
    locations: classpath:db/migration  # Remove ,classpath:db/migration/dev
```

### Seed Data Runs in Production

**Possible Causes:**
1. `SPRING_PROFILES_ACTIVE` not set to `prod`
2. Seed file in wrong location (`db/migration/` instead of `db/migration/dev/`)
3. Flyway locations includes `dev/` folder

**Debug:**
```bash
# Check active profile
docker exec sara-asansor-api-prod env | grep SPRING_PROFILES_ACTIVE

# Check Flyway locations
docker exec sara-asansor-api-prod cat /app/BOOT-INF/classes/application-prod.yml | grep flyway -A 3

# Check guard status
docker logs sara-asansor-api-prod | grep -i "seed guard"
```

## Summary

✅ **Production Configuration:**
- Flyway locations: `classpath:db/migration` (NO dev/)
- Seed enabled: `false`
- Profile: `prod`
- Guard: ACTIVE

✅ **Development Configuration:**
- Flyway locations: `classpath:db/migration,classpath:db/migration/dev`
- Seed enabled: `true`
- Profile: `dev`
- Guard: INACTIVE

**Result:** Seed data NEVER runs in production.
