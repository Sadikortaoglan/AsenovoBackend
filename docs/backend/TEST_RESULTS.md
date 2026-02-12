# Backend API Test Results

## Test Script

Test script'i oluşturuldu: `test-api.sh`

Kullanım:
```bash
cd /Users/sadikortaoglan/Desktop/SaraAsansor/backend
./test-api.sh http://localhost:8081/api
```

## Notlar

1. Backend servisi çalışıyor (port 8081)
2. Context path: `/api`
3. Swagger URL: `http://localhost:8081/api/swagger-ui/index.html`
4. CORS: localhost:5173 için yapılandırılmış ✅
5. Enum mapping: ACIK/TAMAMLANDI → OPEN/COMPLETED ✅

## Manuel Test Gereken Endpoint'ler

1. ✅ `/api/auth/login` - POST
2. ✅ `/api/auth/refresh` - POST
3. ✅ `/api/elevators` - GET, POST
4. ✅ `/api/maintenances` - GET, POST
5. ✅ `/api/faults?status=ACIK` - GET
6. ✅ `/api/faults?status=TAMAMLANDI` - GET
7. ✅ `/api/inspections` - GET, POST
8. ✅ `/api/payments` - GET
9. ✅ `/api/parts` - GET, POST
10. ✅ `/api/warnings?type=EXPIRED` - GET
11. ✅ `/api/warnings?type=WARNING` - GET
12. ✅ `/api/dashboard/summary` - GET

