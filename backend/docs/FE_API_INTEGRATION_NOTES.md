# Sara Asansor FE Integration Notes (QA-Verified)

## Base
- Base URL: `http://localhost:8081/api`
- Auth: `Authorization: Bearer <accessToken>`
- Login: `POST /auth/login`
- Standard response:
```json
{
  "success": true,
  "message": "string",
  "data": {},
  "errors": null
}
```

## Global Rules
- All list endpoints support pagination with `page` and `size` when applicable.
- Not-found routes now return `404` (not `500`).
- Sensitive EDM password is not returned as plain value.

## 1) Maintenance Completions
- `GET /maintenance-completions?page=0&size=25`

## 2) Elevator Labels (multipart payload)
- `GET /elevator-labels?page=0&size=25`
- `POST /elevator-labels` (`multipart/form-data`, field: `payload`)
- `PUT /elevator-labels/{id}` (`multipart/form-data`, field: `payload`)
- `DELETE /elevator-labels/{id}`

Create/Update payload:
```json
{
  "elevatorId": 1,
  "labelName": "KIRMIZI",
  "startAt": "2026-02-01T10:00:00",
  "endAt": "2027-02-01T10:00:00",
  "description": "Etiket açıklaması"
}
```

## 3) Elevator Contracts (multipart payload)
- `GET /elevator-contracts?page=0&size=25`
- `POST /elevator-contracts` (`multipart/form-data`, field: `payload`)
- `PUT /elevator-contracts/{id}` (`multipart/form-data`, field: `payload`)
- `DELETE /elevator-contracts/{id}`

Create/Update payload:
```json
{
  "elevatorId": 1,
  "contractDate": "2026-02-20",
  "contractHtml": "<p>Sözleşme metni</p>"
}
```

## 4) EDM Module
- `GET /edm/invoices/incoming?page=0&size=25`
- `GET /edm/invoices/outgoing?page=0&size=25`
- `POST /edm/invoices/manual`
- `POST /edm/invoices/merge`
- `GET /edm/vkn-tckn/validate?value=11111111110`
- `GET /edm/settings`
- `PUT /edm/settings`

Manual invoice payload:
```json
{
  "invoiceNo": "MAN-001",
  "invoiceDate": "2026-02-22",
  "direction": "OUTGOING",
  "profile": "TICARI FATURA",
  "status": "DRAFT",
  "receiverName": "Alıcı AŞ",
  "receiverVknTckn": "1234567890",
  "currency": "TRY",
  "amount": 1250.50,
  "note": "Manuel fatura"
}
```

EDM settings payload:
```json
{
  "username": "edm_user",
  "password": "StrongPass123",
  "email": "mail@ornek.com",
  "invoiceSeriesEarchive": "SAR",
  "invoiceSeriesEfatura": "MUR",
  "mode": "PRODUCTION"
}
```

## 5) Payments (Tahsilat)
- `GET /payment-transactions?page=0&size=25`
- `POST /payment-transactions`
- `PUT /payment-transactions/{id}`
- `DELETE /payment-transactions/{id}`
- `GET /payment-transactions/cash-accounts`
- `POST /payment-transactions/cash-accounts`
- `DELETE /payment-transactions/cash-accounts/{id}`
- `GET /payment-transactions/bank-accounts`
- `POST /payment-transactions/bank-accounts`
- `DELETE /payment-transactions/bank-accounts/{id}`

Payment payload:
```json
{
  "paymentType": "CASH",
  "amount": 750.50,
  "description": "Tahsilat açıklaması",
  "paymentDate": "2026-02-22T12:00:00",
  "cashAccountId": 1
}
```

## 6) Stocks
- `GET /stocks?page=0&size=25`
- `POST /stocks`
- `PUT /stocks/{id}`
- `DELETE /stocks/{id}`
- `GET /stocks/transfers?page=0&size=25`
- `POST /stocks/transfers`
- `GET /stocks/models`
- `GET /stocks/vat-rates`

Stock payload:
```json
{
  "productName": "Motor",
  "stockGroup": "Motor",
  "modelName": "MTR-X",
  "unit": "Adet",
  "vatRate": 20,
  "purchasePrice": 1000,
  "salePrice": 1400,
  "stockIn": 10,
  "stockOut": 0
}
```

Transfer payload:
```json
{
  "fromStockId": 2,
  "toStockId": 1,
  "quantity": 5,
  "transferDate": "2026-02-22T14:00:00",
  "note": "Transfer notu"
}
```

## 7) Proposals / Revision
- `GET /proposals?page=0&size=25`
- `POST /proposals`
- `POST /proposals/{proposalId}/items`
- `DELETE /proposals/{proposalId}/items/{itemId}`

Proposal payload:
```json
{
  "elevatorId": 1,
  "date": "2026-02-22",
  "vatRate": 20,
  "discountAmount": 0,
  "status": "PENDING",
  "items": []
}
```

Proposal line item payload:
```json
{
  "partId": 1,
  "quantity": 2,
  "unitPrice": 175
}
```

## 8) Status Detection Reports (multipart payload)
- `GET /reports/status-detections?page=0&size=25`
- `POST /reports/status-detections` (`multipart/form-data`, field: `payload`)
- `PUT /reports/status-detections/{id}` (`multipart/form-data`, field: `payload`)
- `DELETE /reports/status-detections/{id}`

Payload:
```json
{
  "reportDate": "2026-02-22",
  "buildingName": "Pasha House",
  "elevatorName": "Pasha Rezidans",
  "identityNumber": "ELEV-001",
  "status": "ACTIVE",
  "note": "Rapor notu"
}
```

## 9) Faults (Arıza)
- `POST /faults`
- `GET /faults?status=OPEN`
- `GET /faults?status=COMPLETED`
- `PUT /faults/{id}/status?status=COMPLETED`
- `DELETE /faults/{id}`

Important enum note:
- Accepted status values: `OPEN` / `ACIK`, `COMPLETED` / `TAMAMLANDI`
- `RESOLVED` is **not** accepted.

## QA Result
- Full CRUD/flow automation run: `53 PASS / 0 FAIL`
- Evidence log: `/Users/sadikortaoglan/Desktop/SaraAsansor/backend/qa-crud-full.log`
- Automation script: `/Users/sadikortaoglan/Desktop/SaraAsansor/backend/qa_crud_full.sh`
- Postman collection (updated): `/Users/sadikortaoglan/Desktop/SaraAsansor/backend/SaraAsansor_API.postman_collection.json`
