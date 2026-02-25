#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081/api}"
USER_NAME="${USER_NAME:-patron}"
USER_PASS="${USER_PASS:-password}"
LOG_FILE="${LOG_FILE:-qa-crud-full.log}"

: > "$LOG_FILE"
PASS=0
FAIL=0

log(){ echo "$*" | tee -a "$LOG_FILE"; }

req(){
  local method="$1" url="$2" body="${3:-}" ctype="${4:-application/json}" auth="${5:-1}"
  local out status
  local cmd=(curl -sS -w "\n__STATUS__:%{http_code}" -X "$method" "$url")
  if [[ "$auth" == "1" ]]; then cmd+=( -H "Authorization: Bearer $TOKEN" ); fi
  if [[ -n "$body" ]]; then cmd+=( -H "Content-Type: $ctype" -d "$body" ); fi
  out="$("${cmd[@]}")"
  status="${out##*__STATUS__:}"
  BODY="${out%__STATUS__:*}"
  STATUS="$status"
}

assert_status(){
  local name="$1" expected="$2"
  if [[ "$STATUS" == "$expected" ]]; then
    log "PASS [$name] status=$STATUS"
    PASS=$((PASS+1))
  else
    log "FAIL [$name] expected=$expected got=$STATUS body=$BODY"
    FAIL=$((FAIL+1))
  fi
}

assert_one_of(){
  local name="$1" e1="$2" e2="$3"
  if [[ "$STATUS" == "$e1" || "$STATUS" == "$e2" ]]; then
    log "PASS [$name] status=$STATUS"
    PASS=$((PASS+1))
  else
    log "FAIL [$name] expected=$e1|$e2 got=$STATUS body=$BODY"
    FAIL=$((FAIL+1))
  fi
}

extract_jq(){
  local filter="$1"
  echo "$BODY" | jq -r "$filter // empty" 2>/dev/null || true
}

# Login
req POST "$BASE_URL/auth/login" "{\"username\":\"$USER_NAME\",\"password\":\"$USER_PASS\"}" "application/json" 0
assert_status "auth.login" 200
TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // .accessToken // empty')"
if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  log "FAIL [auth.token] token not found"
  FAIL=$((FAIL+1))
  log "SUMMARY pass=$PASS fail=$FAIL"
  exit 1
fi
log "INFO token_len=${#TOKEN}"

# lookup required ids
req GET "$BASE_URL/elevators?page=0&size=1"
assert_status "elevators.list" 200
ELEVATOR_ID="$(extract_jq '.data.content[0].id')"
if [[ -z "$ELEVATOR_ID" ]]; then ELEVATOR_ID="1"; fi

req GET "$BASE_URL/parts"
assert_status "parts.list" 200
PART_ID="$(extract_jq '.data[0].id')"
if [[ -z "$PART_ID" ]]; then
  req POST "$BASE_URL/parts" '{"name":"QA Parça","description":"QA otomasyon parçası","unitPrice":150.0,"stock":25}'
  assert_status "parts.create" 200
  PART_ID="$(extract_jq '.data.id')"
fi

# A) Elevator Labels CRUD
payload_label='{"elevatorId":'"$ELEVATOR_ID"',"labelName":"QA-ETIKET-'"$(date +%s)"'","startAt":"2026-02-01T10:00:00","endAt":"2027-02-01T10:00:00","description":"QA etiket test"}'
out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X POST "$BASE_URL/elevator-labels" \
 -H "Authorization: Bearer $TOKEN" \
 -F "payload=$payload_label;type=application/json")
STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
assert_status "elevator-labels.create" 201
LABEL_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/elevator-labels?page=0&size=5"
assert_status "elevator-labels.list" 200

if [[ -n "$LABEL_ID" ]]; then
  payload_label_upd='{"id":'"$LABEL_ID"',"elevatorId":'"$ELEVATOR_ID"',"labelName":"QA-ETIKET-GUNCEL-'"$(date +%s)"'","startAt":"2026-03-01T10:00:00","endAt":"2027-03-01T10:00:00","description":"QA etiket güncel"}'
  out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X PUT "$BASE_URL/elevator-labels/$LABEL_ID" \
   -H "Authorization: Bearer $TOKEN" \
   -F "payload=$payload_label_upd;type=application/json")
  STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
  assert_status "elevator-labels.update" 200

  req DELETE "$BASE_URL/elevator-labels/$LABEL_ID"
  assert_status "elevator-labels.delete" 200
fi

# A) Elevator Contracts CRUD
payload_contract='{"elevatorId":'"$ELEVATOR_ID"',"contractDate":"2026-02-20","contractHtml":"<p>QA sözleşme</p>"}'
out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X POST "$BASE_URL/elevator-contracts" \
 -H "Authorization: Bearer $TOKEN" \
 -F "payload=$payload_contract;type=application/json")
STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
assert_status "elevator-contracts.create" 201
CONTRACT_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/elevator-contracts?page=0&size=5"
assert_status "elevator-contracts.list" 200

if [[ -n "$CONTRACT_ID" ]]; then
  payload_contract_upd='{"id":'"$CONTRACT_ID"',"elevatorId":'"$ELEVATOR_ID"',"contractDate":"2026-03-20","contractHtml":"<p>QA sözleşme güncel</p>"}'
  out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X PUT "$BASE_URL/elevator-contracts/$CONTRACT_ID" \
   -H "Authorization: Bearer $TOKEN" \
   -F "payload=$payload_contract_upd;type=application/json")
  STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
  assert_status "elevator-contracts.update" 200

  req DELETE "$BASE_URL/elevator-contracts/$CONTRACT_ID"
  assert_status "elevator-contracts.delete" 200
fi

# B) Maintenance completed list
req GET "$BASE_URL/maintenance-completions?page=0&size=10"
assert_status "maintenance-completions.list" 200

# D) EDM module
req GET "$BASE_URL/edm/invoices/incoming?page=0&size=5"
assert_status "edm.incoming.list" 200

req GET "$BASE_URL/edm/invoices/outgoing?page=0&size=5"
assert_status "edm.outgoing.list" 200

req POST "$BASE_URL/edm/invoices/manual" '{"invoiceNo":"QA-MAN-'"$(date +%s)"'","invoiceDate":"2026-02-22","direction":"OUTGOING","profile":"TICARI FATURA","status":"DRAFT","receiverName":"QA ALICI","receiverVknTckn":"1234567890","currency":"TRY","amount":1234.56,"note":"QA manuel fatura"}'
assert_status "edm.manual.create" 201
MANUAL_INVOICE_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/edm/vkn-tckn/validate?value=11111111110"
assert_status "edm.vkn.validate" 200

req PUT "$BASE_URL/edm/settings" '{"username":"qa_edm_user","password":"QaTest1234","email":"qa@example.com","invoiceSeriesEarchive":"QAE","invoiceSeriesEfatura":"QAF","mode":"PRODUCTION"}'
assert_status "edm.settings.save" 200

req GET "$BASE_URL/edm/settings"
assert_status "edm.settings.get" 200

if [[ -n "$MANUAL_INVOICE_ID" ]]; then
  req POST "$BASE_URL/edm/invoices/merge" '{"invoiceIds":['"$MANUAL_INVOICE_ID"','"$MANUAL_INVOICE_ID"']}'
  assert_one_of "edm.invoices.merge.validation" 400 500
fi

# E) Payments CRUD
req POST "$BASE_URL/payment-transactions/cash-accounts" '{"name":"QA Kasa '"$(date +%s)"'","currency":"TRY"}'
assert_status "payments.cash-account.create" 201
CASH_ID="$(extract_jq '.data.id')"

req POST "$BASE_URL/payment-transactions/bank-accounts" '{"name":"QA Banka '"$(date +%s)"'","currency":"TRY"}'
assert_status "payments.bank-account.create" 201
BANK_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/payment-transactions/cash-accounts"
assert_status "payments.cash-account.list" 200

req GET "$BASE_URL/payment-transactions/bank-accounts"
assert_status "payments.bank-account.list" 200

pay_payload='{"paymentType":"CASH","amount":750.50,"description":"QA tahsilat","paymentDate":"2026-02-22T12:00:00","cashAccountId":'"$CASH_ID"',"buildingId":null,"currentAccountId":null}'
req POST "$BASE_URL/payment-transactions" "$pay_payload"
assert_status "payments.create" 201
PAY_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/payment-transactions?page=0&size=10"
assert_status "payments.list" 200

if [[ -n "$PAY_ID" ]]; then
  pay_upd='{"id":'"$PAY_ID"',"paymentType":"BANK","amount":990.25,"description":"QA tahsilat güncel","paymentDate":"2026-02-22T13:00:00","bankAccountId":'"$BANK_ID"',"buildingId":null,"currentAccountId":null}'
  req PUT "$BASE_URL/payment-transactions/$PAY_ID" "$pay_upd"
  assert_status "payments.update" 200

  req DELETE "$BASE_URL/payment-transactions/$PAY_ID"
  assert_status "payments.delete" 200
fi

if [[ -n "$CASH_ID" ]]; then
  req DELETE "$BASE_URL/payment-transactions/cash-accounts/$CASH_ID"
  assert_status "payments.cash-account.delete" 200
fi

if [[ -n "$BANK_ID" ]]; then
  req DELETE "$BASE_URL/payment-transactions/bank-accounts/$BANK_ID"
  assert_status "payments.bank-account.delete" 200
fi

# F) Stock CRUD + transfer
stock1='{"productName":"QA Motor '"$(date +%s)"'","stockGroup":"Motor","modelName":"MTR-QA","unit":"Adet","vatRate":20,"purchasePrice":1000,"salePrice":1400,"stockIn":15,"stockOut":0}'
req POST "$BASE_URL/stocks" "$stock1"
assert_status "stocks.create.1" 201
STOCK1_ID="$(extract_jq '.data.id')"

stock2='{"productName":"QA Kablo '"$(date +%s)"'","stockGroup":"Elektrik","modelName":"KBL-QA","unit":"Metre","vatRate":10,"purchasePrice":20,"salePrice":35,"stockIn":500,"stockOut":0}'
req POST "$BASE_URL/stocks" "$stock2"
assert_status "stocks.create.2" 201
STOCK2_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/stocks?page=0&size=10"
assert_status "stocks.list" 200

if [[ -n "$STOCK1_ID" ]]; then
  stock1_upd='{"id":'"$STOCK1_ID"',"productName":"QA Motor Guncel","stockGroup":"Motor","modelName":"MTR-QA-2","unit":"Adet","vatRate":20,"purchasePrice":1100,"salePrice":1500,"stockIn":20,"stockOut":1}'
  req PUT "$BASE_URL/stocks/$STOCK1_ID" "$stock1_upd"
  assert_status "stocks.update" 200
fi

if [[ -n "$STOCK1_ID" && -n "$STOCK2_ID" ]]; then
  transfer='{"fromStockId":'"$STOCK2_ID"',"toStockId":'"$STOCK1_ID"',"quantity":5,"transferDate":"2026-02-22T14:00:00","note":"QA transfer"}'
  req POST "$BASE_URL/stocks/transfers" "$transfer"
  assert_status "stocks.transfer.create" 201
fi

req GET "$BASE_URL/stocks/transfers?page=0&size=10"
assert_status "stocks.transfer.list" 200

req GET "$BASE_URL/stocks/models"
assert_status "stocks.models.list" 200

req GET "$BASE_URL/stocks/vat-rates"
assert_status "stocks.vat-rates.list" 200

if [[ -n "$STOCK1_ID" ]]; then
  req DELETE "$BASE_URL/stocks/$STOCK1_ID"
  assert_status "stocks.delete.1" 200
fi
if [[ -n "$STOCK2_ID" ]]; then
  req DELETE "$BASE_URL/stocks/$STOCK2_ID"
  assert_status "stocks.delete.2" 200
fi

# G) Proposals
proposal='{"elevatorId":'"$ELEVATOR_ID"',"date":"2026-02-22","vatRate":20,"discountAmount":0,"status":"PENDING","items":[]}'
req POST "$BASE_URL/proposals" "$proposal"
assert_status "proposals.create" 201
PROPOSAL_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/proposals?page=0&size=10"
assert_status "proposals.list" 200

if [[ -n "$PROPOSAL_ID" && -n "$PART_ID" ]]; then
  req POST "$BASE_URL/proposals/$PROPOSAL_ID/items" '{"partId":'"$PART_ID"',"quantity":2,"unitPrice":175.0}'
  assert_status "proposals.items.add" 200
  ITEM_ID="$(extract_jq '.data.items[-1].id')"

  if [[ -n "$ITEM_ID" ]]; then
    req DELETE "$BASE_URL/proposals/$PROPOSAL_ID/items/$ITEM_ID"
    assert_status "proposals.items.remove" 200
  fi
fi

# H) Status detection reports CRUD
payload_report='{"reportDate":"2026-02-22","buildingName":"QA Bina","elevatorName":"QA Asansor","identityNumber":"QA-IDENT-001","status":"ACTIVE","note":"QA rapor notu"}'
out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X POST "$BASE_URL/reports/status-detections" \
 -H "Authorization: Bearer $TOKEN" \
 -F "payload=$payload_report;type=application/json")
STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
assert_status "reports.status.create" 201
REPORT_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/reports/status-detections?page=0&size=10"
assert_status "reports.status.list" 200

if [[ -n "$REPORT_ID" ]]; then
  payload_report_upd='{"id":'"$REPORT_ID"',"reportDate":"2026-02-23","buildingName":"QA Bina Guncel","elevatorName":"QA Asansor 2","identityNumber":"QA-IDENT-002","status":"PASSIVE","note":"QA rapor notu güncel"}'
  out=$(curl -sS -w "\n__STATUS__:%{http_code}" -X PUT "$BASE_URL/reports/status-detections/$REPORT_ID" \
   -H "Authorization: Bearer $TOKEN" \
   -F "payload=$payload_report_upd;type=application/json")
  STATUS="${out##*__STATUS__:}"; BODY="${out%__STATUS__:*}"
  assert_status "reports.status.update" 200

  req DELETE "$BASE_URL/reports/status-detections/$REPORT_ID"
  assert_status "reports.status.delete" 200
fi

# C) Fault CRUD + status change
fault_payload='{"elevatorId":'"$ELEVATOR_ID"',"faultSubject":"QA arıza testi","contactPerson":"QA Yetkili","buildingAuthorizedMessage":"Bilgilendirildi","description":"QA otomasyon testi"}'
req POST "$BASE_URL/faults" "$fault_payload"
assert_status "faults.create" 200
FAULT_ID="$(extract_jq '.data.id')"

req GET "$BASE_URL/faults?status=OPEN"
assert_status "faults.list.open" 200

if [[ -n "$FAULT_ID" ]]; then
  req PUT "$BASE_URL/faults/$FAULT_ID/status?status=COMPLETED" "" "application/json"
  assert_status "faults.status.change" 200

  req GET "$BASE_URL/faults?status=COMPLETED"
  assert_status "faults.list.completed" 200

  req DELETE "$BASE_URL/faults/$FAULT_ID"
  assert_status "faults.delete" 200
fi

# route not found should be 404 (global handler)
req GET "$BASE_URL/definitely-not-existing-endpoint"
assert_status "routing.not-found.404" 404

log "SUMMARY pass=$PASS fail=$FAIL"
if [[ "$FAIL" -gt 0 ]]; then exit 1; fi
