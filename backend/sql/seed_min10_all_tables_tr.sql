-- Ensure minimum 10 rows in each public domain table with Turkish dummy data

-- 1) USERS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM users), 10) i
)
INSERT INTO users (username, password_hash, role, active, created_at)
SELECT
  'tr_min10_kullanici_' || i,
  '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
  CASE WHEN i % 3 = 0 THEN 'ADMIN' WHEN i % 2 = 0 THEN 'PERSONEL' ELSE 'PATRON' END,
  true,
  CURRENT_TIMESTAMP - (i || ' hour')::interval
FROM g
ON CONFLICT (username) DO NOTHING;

-- 2) BUILDINGS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM buildings), 12) i
)
INSERT INTO buildings (name, address, city, district, created_at, updated_at)
SELECT
  'TR Min10 Bina ' || i,
  'Örnek Mah. Deneme Sok. No:' || (100 + i),
  CASE WHEN i % 3 = 0 THEN 'İzmir' WHEN i % 3 = 1 THEN 'Ankara' ELSE 'İstanbul' END,
  CASE WHEN i % 2 = 0 THEN 'Konak' ELSE 'Çankaya' END,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM buildings b WHERE b.name = 'TR Min10 Bina ' || i);

-- 3) CURRENT ACCOUNTS (one per building, unique building_id)
WITH need AS (
  SELECT GREATEST(0, 10 - (SELECT count(*) FROM current_accounts)) n
), candidates AS (
  SELECT b.id AS building_id, row_number() OVER (ORDER BY b.id) rn
  FROM buildings b
  LEFT JOIN current_accounts ca ON ca.building_id = b.id
  WHERE ca.id IS NULL
)
INSERT INTO current_accounts (building_id, name, authorized_person, phone, debt, credit, balance, created_at, updated_at)
SELECT
  c.building_id,
  'TR Min10 Cari ' || c.building_id,
  'Yetkili ' || c.building_id,
  '0532' || lpad((1000000 + c.building_id)::text, 7, '0'),
  (1000 + c.building_id)::numeric,
  (300 + c.building_id)::numeric,
  ((300 + c.building_id) - (1000 + c.building_id))::numeric,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM candidates c
CROSS JOIN need n
WHERE c.rn <= n.n;

-- 4) ELEVATORS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM elevators), 10) i
)
INSERT INTO elevators (
  identity_number, building_name, address, elevator_number, floor_count, capacity, speed,
  technical_notes, drive_type, machine_brand, door_type, installation_year, serial_number,
  control_system, rope, modernization, inspection_date, label_date, label_type, expiry_date, status,
  building_id, blue_label, manager_name, manager_tc_identity_no, manager_phone, manager_email,
  created_at, updated_at
)
SELECT
  'TR-MIN10-ELEV-' || lpad(i::text, 4, '0'),
  b.name,
  b.address,
  'E-' || i,
  5 + (i % 10),
  630 + (i * 10),
  1.0 + ((i % 4)::numeric / 10.0),
  'TR Min10 teknik not ' || i,
  CASE WHEN i % 2 = 0 THEN 'Traction' ELSE 'Hydraulic' END,
  CASE WHEN i % 2 = 0 THEN 'Mitsubishi' ELSE 'Otis' END,
  CASE WHEN i % 2 = 0 THEN 'Otomatik' ELSE 'Yarı Otomatik' END,
  2015 + (i % 10),
  'TRSN-MIN10-' || i,
  CASE WHEN i % 2 = 0 THEN 'Monarch' ELSE 'Arkel' END,
  (6 + (i % 4)) || ' Halat',
  CASE WHEN i % 3 = 0 THEN '2025 Revizyon' ELSE 'Yok' END,
  CURRENT_DATE - ((i % 120) || ' day')::interval,
  CURRENT_DATE - ((i % 120) || ' day')::interval,
  CASE WHEN i % 4 = 0 THEN 'RED'::label_type WHEN i % 4 = 1 THEN 'GREEN'::label_type WHEN i % 4 = 2 THEN 'YELLOW'::label_type ELSE 'BLUE'::label_type END,
  CURRENT_DATE + ((180 + i) || ' day')::interval,
  'ACTIVE'::elevator_status,
  b.id,
  false,
  'Yönetici ' || i,
  lpad((20000000000 + i)::text, 11, '0'),
  '0533' || lpad((2000000 + i)::text, 7, '0'),
  'yonetici' || i || '@ornek.com',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (
  SELECT id, name, address
  FROM buildings
  ORDER BY id
  OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM buildings))
  LIMIT 1
) b ON true
WHERE NOT EXISTS (
  SELECT 1 FROM elevators e WHERE e.identity_number = 'TR-MIN10-ELEV-' || lpad(i::text, 4, '0')
);

-- 5) PARTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM parts), 10) i
)
INSERT INTO parts (name, description, unit_price, stock, created_at)
SELECT
  'TR Min10 Parça ' || i,
  'TR Min10 açıklama ' || i,
  (100 + i * 15)::numeric,
  5 + i,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM parts p WHERE p.name = 'TR Min10 Parça ' || i);

-- 6) MAINTENANCE TEMPLATES
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_templates), 10) i
)
INSERT INTO maintenance_templates (name, status, frequency_days, created_at, updated_at)
SELECT
  'TR Min10 Şablon ' || i,
  CASE WHEN i % 2 = 0 THEN 'ACTIVE' ELSE 'PASSIVE' END,
  15 + (i % 90),
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM maintenance_templates t WHERE t.name = 'TR Min10 Şablon ' || i);

-- 7) MAINTENANCE SECTIONS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_sections), 10) i
)
INSERT INTO maintenance_sections (template_id, name, sort_order, created_at, active)
SELECT
  t.id,
  'TR Min10 Bölüm ' || i,
  i,
  CURRENT_TIMESTAMP,
  true
FROM g
JOIN LATERAL (
  SELECT id
  FROM maintenance_templates
  ORDER BY id
  OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_templates))
  LIMIT 1
) t ON true
WHERE NOT EXISTS (
  SELECT 1 FROM maintenance_sections s WHERE s.template_id = t.id AND s.name = 'TR Min10 Bölüm ' || i
);

-- 8) MAINTENANCE ITEMS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_items), 10) i
)
INSERT INTO maintenance_items (section_id, title, description, mandatory, allow_photo, allow_note, sort_order, is_active, created_at)
SELECT
  s.id,
  'TR Min10 Bakım Maddesi ' || i,
  'TR Min10 bakım maddesi açıklaması ' || i,
  true,
  true,
  true,
  i,
  true,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (
  SELECT id
  FROM maintenance_sections
  ORDER BY id
  OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_sections))
  LIMIT 1
) s ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_items mi WHERE mi.title = 'TR Min10 Bakım Maddesi ' || i);

-- 9) QR PROOFS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM qr_proofs), 10) i
)
INSERT INTO qr_proofs (elevator_id, token_hash, issued_at, expires_at, used_at, used_by, nonce, ip, created_at)
SELECT
  e.id,
  'tr_min10_token_' || i,
  CURRENT_TIMESTAMP - INTERVAL '1 hour',
  CURRENT_TIMESTAMP + INTERVAL '2 hour',
  CURRENT_TIMESTAMP - INTERVAL '30 minute',
  u.id,
  'tr_min10_nonce_' || i,
  '127.0.0.1',
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (
  SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1
) e ON true
JOIN LATERAL (
  SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1
) u ON true
WHERE NOT EXISTS (SELECT 1 FROM qr_proofs q WHERE q.token_hash = 'tr_min10_token_' || i);

-- 10) MAINTENANCE PLANS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_plans), 10) i
)
INSERT INTO maintenance_plans (
  elevator_id, template_id, planned_date, assigned_technician_id, status, created_at,
  completed_at, qr_proof_id, note, price, started_remotely, started_by_role, started_at, started_by_user_id, started_from_ip
)
SELECT
  e.id,
  t.id,
  CURRENT_DATE - ((i % 5) || ' day')::interval,
  u.id,
  CASE WHEN i % 3 = 0 THEN 'COMPLETED' WHEN i % 3 = 1 THEN 'PLANNED' ELSE 'IN_PROGRESS' END,
  CURRENT_TIMESTAMP,
  CASE WHEN i % 3 = 0 THEN CURRENT_TIMESTAMP - INTERVAL '1 day' ELSE NULL END,
  CASE WHEN i % 3 = 0 THEN q.id ELSE NULL END,
  'TR Min10 Plan ' || i,
  (1500 + i * 100)::numeric,
  false,
  CASE WHEN i % 2 = 0 THEN 'ADMIN' ELSE 'PERSONEL' END,
  CURRENT_TIMESTAMP - INTERVAL '2 day',
  u.id,
  '192.168.10.' || i
FROM g
JOIN LATERAL (
  SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1
) e ON true
JOIN LATERAL (
  SELECT id FROM maintenance_templates ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_templates)) LIMIT 1
) t ON true
JOIN LATERAL (
  SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1
) u ON true
LEFT JOIN LATERAL (
  SELECT id FROM qr_proofs ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM qr_proofs)) LIMIT 1
) q ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_plans mp WHERE mp.note = 'TR Min10 Plan ' || i);

-- 11) MAINTENANCES
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenances), 10) i
)
INSERT INTO maintenances (elevator_id, date, label_type, description, technician_user_id, amount, is_paid, payment_date, created_at)
SELECT
  e.id,
  CURRENT_DATE - ((i % 30) || ' day')::interval,
  CASE WHEN i % 4 = 0 THEN 'RED'::label_type WHEN i % 4 = 1 THEN 'GREEN'::label_type WHEN i % 4 = 2 THEN 'YELLOW'::label_type ELSE 'BLUE'::label_type END,
  'TR Min10 Bakım Kaydı ' || i,
  u.id,
  (1000 + i * 80)::numeric,
  (i % 2 = 0),
  CASE WHEN i % 2 = 0 THEN CURRENT_DATE - ((i % 10) || ' day')::interval ELSE NULL END,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenances m WHERE m.description = 'TR Min10 Bakım Kaydı ' || i);

-- 12) FAULTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM faults), 10) i
)
INSERT INTO faults (elevator_id, fault_subject, contact_person, building_authorized_message, description, status, created_at)
SELECT
  e.id,
  'TR Min10 Arıza Konusu ' || i,
  'TR Min10 Kişi ' || i,
  'TR Min10 bina mesajı ' || i,
  'TR Min10 arıza detay ' || i,
  CASE WHEN i % 2 = 0 THEN 'OPEN' ELSE 'COMPLETED' END,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
WHERE NOT EXISTS (SELECT 1 FROM faults f WHERE f.description = 'TR Min10 arıza detay ' || i);

-- 13) INSPECTIONS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM inspections), 10) i
)
INSERT INTO inspections (elevator_id, date, result, description, inspection_color, contacted_person_name, report_no, created_at)
SELECT
  e.id,
  CURRENT_DATE - ((i % 60) || ' day')::interval,
  CASE WHEN i % 3 = 0 THEN 'FAILED' WHEN i % 3 = 1 THEN 'PASSED' ELSE 'PENDING' END,
  'TR Min10 muayene açıklama ' || i,
  CASE WHEN i % 4 = 0 THEN 'RED'::inspection_color WHEN i % 4 = 1 THEN 'GREEN'::inspection_color WHEN i % 4 = 2 THEN 'YELLOW'::inspection_color ELSE 'ORANGE'::inspection_color END,
  'TR Min10 İrtibat ' || i,
  'TR-MIN10-RPR-' || lpad(i::text, 4, '0'),
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
WHERE NOT EXISTS (SELECT 1 FROM inspections i2 WHERE i2.report_no = 'TR-MIN10-RPR-' || lpad(i::text, 4, '0'));

-- 14) OFFERS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM offers), 10) i
)
INSERT INTO offers (elevator_id, date, vat_rate, discount_amount, subtotal, total_amount, status, created_at)
SELECT
  e.id,
  CURRENT_DATE - ((i % 20) || ' day')::interval,
  20,
  (i * 5)::numeric,
  (2000 + i * 150)::numeric,
  (2400 + i * 180)::numeric,
  CASE WHEN i % 3 = 0 THEN 'ACCEPTED' WHEN i % 3 = 1 THEN 'PENDING' ELSE 'REJECTED' END,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
WHERE NOT EXISTS (
  SELECT 1 FROM offers o WHERE o.subtotal = (2000 + i * 150)::numeric AND o.elevator_id = e.id
);

-- 15) OFFER ITEMS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM offer_items), 10) i
)
INSERT INTO offer_items (offer_id, part_id, quantity, unit_price, line_total)
SELECT
  o.id,
  p.id,
  1 + (i % 4),
  (100 + i * 10)::numeric,
  ((1 + (i % 4)) * (100 + i * 10))::numeric
FROM g
JOIN LATERAL (SELECT id FROM offers ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM offers)) LIMIT 1) o ON true
JOIN LATERAL (SELECT id FROM parts ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM parts)) LIMIT 1) p ON true
WHERE NOT EXISTS (
  SELECT 1 FROM offer_items oi WHERE oi.offer_id = o.id AND oi.part_id = p.id
);

-- 16) PAYMENT RECEIPTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM payment_receipts), 10) i
)
INSERT INTO payment_receipts (maintenance_id, amount, payer_name, date, note, created_at)
SELECT
  m.id,
  (750 + i * 50)::numeric,
  'TR Min10 Ödeyen ' || i,
  CURRENT_DATE - ((i % 10) || ' day')::interval,
  'TR Min10 ödeme fişi ' || i,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM maintenances ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenances)) LIMIT 1) m ON true
WHERE NOT EXISTS (SELECT 1 FROM payment_receipts pr WHERE pr.note = 'TR Min10 ödeme fişi ' || i);

-- 17) REVISION OFFERS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM revision_offers), 10) i
)
INSERT INTO revision_offers (elevator_id, building_id, current_account_id, parts_total, labor_total, total_price, status, created_at, updated_at)
SELECT
  e.id,
  b.id,
  ca.id,
  (3000 + i * 100)::numeric,
  (1200 + i * 60)::numeric,
  (4200 + i * 160)::numeric,
  CASE WHEN i % 4 = 0 THEN 'APPROVED' WHEN i % 4 = 1 THEN 'DRAFT' WHEN i % 4 = 2 THEN 'SENT' ELSE 'REJECTED' END::revision_offer_status,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id, building_id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
JOIN LATERAL (SELECT id FROM buildings WHERE id = e.building_id LIMIT 1) b ON true
JOIN LATERAL (SELECT id FROM current_accounts WHERE building_id = b.id LIMIT 1) ca ON true
WHERE NOT EXISTS (
  SELECT 1 FROM revision_offers ro WHERE ro.elevator_id = e.id AND ro.total_price = (4200 + i * 160)::numeric
);

-- 18) REFRESH TOKENS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM refresh_tokens), 10) i
)
INSERT INTO refresh_tokens (user_id, token, expires_at, created_at, revoked_at, is_revoked)
SELECT
  u.id,
  'tr_min10_refresh_token_' || i,
  CURRENT_TIMESTAMP + INTERVAL '7 day',
  CURRENT_TIMESTAMP,
  NULL,
  false
FROM g
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM refresh_tokens rt WHERE rt.token = 'tr_min10_refresh_token_' || i);

-- 19) MAINTENANCE SESSIONS (insert as IN_PROGRESS to satisfy constraints)
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_sessions), 10) i
)
INSERT INTO maintenance_sessions (
  plan_id, elevator_id, template_id, technician_id, start_at, end_at, status,
  started_by_qr_scan, qr_proof_id, gps_lat, gps_lng, device_info, overall_note, signature_url, created_at
)
SELECT
  mp.id,
  e.id,
  t.id,
  u.id,
  CURRENT_TIMESTAMP - ((i % 12) || ' hour')::interval,
  NULL,
  'IN_PROGRESS',
  false,
  NULL,
  39.9 + (i::numeric / 1000),
  32.8 + (i::numeric / 1000),
  'TR Min10 Cihaz ' || i,
  'TR Min10 Oturum ' || i,
  NULL,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id, elevator_id, template_id FROM maintenance_plans ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_plans)) LIMIT 1) mp ON true
JOIN LATERAL (SELECT id FROM elevators WHERE id = mp.elevator_id LIMIT 1) e ON true
JOIN LATERAL (SELECT id FROM maintenance_templates WHERE id = mp.template_id LIMIT 1) t ON true
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_sessions ms WHERE ms.overall_note = 'TR Min10 Oturum ' || i);

-- 20) MAINTENANCE STEP RESULTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_step_results), 10) i
)
INSERT INTO maintenance_step_results (session_id, item_id, result, note, created_at, updated_at)
SELECT
  s.id,
  mi.id,
  CASE WHEN i % 3 = 0 THEN 'ISSUE_FOUND' WHEN i % 3 = 1 THEN 'COMPLETED' ELSE 'NOT_APPLICABLE' END,
  'TR Min10 adım sonucu ' || i,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM maintenance_sessions ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_sessions)) LIMIT 1) s ON true
JOIN LATERAL (SELECT id FROM maintenance_items ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_items)) LIMIT 1) mi ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_step_results msr WHERE msr.session_id = s.id AND msr.item_id = mi.id);

-- 21) MAINTENANCE ATTACHMENTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_attachments), 10) i
)
INSERT INTO maintenance_attachments (session_id, item_id, file_url, file_type, created_at)
SELECT
  s.id,
  mi.id,
  '/api/files/min10/maintenance_attachment_' || i || '.jpg',
  'PHOTO',
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM maintenance_sessions ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_sessions)) LIMIT 1) s ON true
JOIN LATERAL (SELECT id FROM maintenance_items ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_items)) LIMIT 1) mi ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_attachments ma WHERE ma.file_url = '/api/files/min10/maintenance_attachment_' || i || '.jpg');

-- 22) MAINTENANCE PLAN PHOTOS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM maintenance_plan_photos), 10) i
)
INSERT INTO maintenance_plan_photos (plan_id, file_url, file_type, uploaded_by, created_at)
SELECT
  p.id,
  '/api/files/min10/plan_photo_' || i || '.jpg',
  'PHOTO',
  u.id,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM maintenance_plans ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_plans)) LIMIT 1) p ON true
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM maintenance_plan_photos mpp WHERE mpp.file_url = '/api/files/min10/plan_photo_' || i || '.jpg');

-- 23) QR SCAN LOGS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM qr_scan_logs), 10) i
)
INSERT INTO qr_scan_logs (
  elevator_id, technician_id, session_id, qr_proof_id, scan_timestamp,
  gps_lat, gps_lng, device_info, ip_address, is_valid, validation_error, created_at
)
SELECT
  e.id,
  u.id,
  s.id,
  q.id,
  CURRENT_TIMESTAMP - ((i % 24) || ' hour')::interval,
  39.90 + (i::numeric / 1000),
  32.85 + (i::numeric / 1000),
  'TR Min10 QR Cihaz ' || i,
  '10.0.0.' || i,
  true,
  'TR Min10 QR Log ' || i,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
JOIN LATERAL (SELECT id FROM maintenance_sessions ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenance_sessions)) LIMIT 1) s ON true
JOIN LATERAL (SELECT id FROM qr_proofs ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM qr_proofs)) LIMIT 1) q ON true
WHERE NOT EXISTS (SELECT 1 FROM qr_scan_logs qsl WHERE qsl.validation_error = 'TR Min10 QR Log ' || i);

-- 24) FILE ATTACHMENTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM file_attachments), 10) i
)
INSERT INTO file_attachments (entity_type, entity_id, file_name, content_type, size, storage_key, url, uploaded_by_user_id, created_at)
SELECT
  CASE WHEN i % 3 = 0 THEN 'ELEVATOR' WHEN i % 3 = 1 THEN 'MAINTENANCE' ELSE 'OFFER' END,
  CASE WHEN i % 3 = 0 THEN e.id WHEN i % 3 = 1 THEN m.id ELSE o.id END,
  'tr_min10_dosya_' || i || '.pdf',
  'application/pdf',
  10240 + i,
  'min10/' || i || '/tr_min10_dosya_' || i || '.pdf',
  '/api/files/min10/tr_min10_dosya_' || i || '.pdf',
  u.id,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
JOIN LATERAL (SELECT id FROM maintenances ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM maintenances)) LIMIT 1) m ON true
JOIN LATERAL (SELECT id FROM offers ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM offers)) LIMIT 1) o ON true
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM file_attachments fa WHERE fa.storage_key = 'min10/' || i || '/tr_min10_dosya_' || i || '.pdf');

-- 25) AUDIT LOGS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM audit_logs), 10) i
)
INSERT INTO audit_logs (user_id, action, entity_type, entity_id, metadata_json, created_at)
SELECT
  u.id,
  'TR_MIN10_AKSİYON_' || i,
  'SYSTEM',
  i,
  '{"aciklama":"TR Min10 denetim kaydı ' || i || '"}',
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM users ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM users)) LIMIT 1) u ON true
WHERE NOT EXISTS (SELECT 1 FROM audit_logs al WHERE al.action = 'TR_MIN10_AKSİYON_' || i);

-- 26) EDM SETTINGS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM edm_settings), 10) i
)
INSERT INTO edm_settings (username, encrypted_password, email, invoice_series_earchive, invoice_series_efatura, mode, created_at, updated_at)
SELECT
  'tr_min10_edm_' || i,
  'TR_MIN10_SECRET_' || i,
  'tr_min10_edm_' || i || '@ornek.com',
  'EA' || (i % 10),
  'EF' || (i % 10),
  CASE WHEN i % 2 = 0 THEN 'PRODUCTION' ELSE 'TEST' END,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g;

-- 27) INVOICE RECORDS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM invoice_records), 10) i
)
INSERT INTO invoice_records (
  invoice_no, invoice_date, direction, profile, status, sender_name, sender_vkn_tckn,
  receiver_name, receiver_vkn_tckn, currency, amount, note, source,
  created_at, updated_at
)
SELECT
  'TR-MIN10-FAT-' || lpad(i::text, 4, '0'),
  CURRENT_DATE - ((i % 30) || ' day')::interval,
  CASE WHEN i % 2 = 0 THEN 'INCOMING' ELSE 'OUTGOING' END,
  CASE WHEN i % 2 = 0 THEN 'E-FATURA' ELSE 'TICARI FATURA' END,
  CASE WHEN i % 3 = 0 THEN 'SUCCESS' WHEN i % 3 = 1 THEN 'DRAFT' ELSE 'WAITING' END,
  CASE WHEN i % 2 = 0 THEN 'TR Min10 Gönderen ' || i ELSE NULL END,
  CASE WHEN i % 2 = 0 THEN lpad((3000000000 + i)::text, 10, '0') ELSE NULL END,
  CASE WHEN i % 2 = 1 THEN 'TR Min10 Alıcı ' || i ELSE NULL END,
  CASE WHEN i % 2 = 1 THEN lpad((4000000000 + i)::text, 10, '0') ELSE NULL END,
  'TRY',
  (500 + i * 75)::numeric,
  'TR Min10 fatura notu ' || i,
  'MANUAL',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM invoice_records ir WHERE ir.invoice_no = 'TR-MIN10-FAT-' || lpad(i::text, 4, '0'));

-- 28) CASH ACCOUNTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM cash_accounts), 10) i
)
INSERT INTO cash_accounts (name, currency, total_in, total_out, balance, created_at, updated_at)
SELECT
  'TR Min10 Kasa ' || i,
  'TRY',
  0,
  0,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
ON CONFLICT (name) DO NOTHING;

-- 29) BANK ACCOUNTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM bank_accounts), 10) i
)
INSERT INTO bank_accounts (name, currency, total_in, total_out, balance, created_at, updated_at)
SELECT
  'TR Min10 Banka ' || i,
  'TRY',
  0,
  0,
  0,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
ON CONFLICT (name) DO NOTHING;

-- 30) PAYMENT TRANSACTIONS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM payment_transactions), 10) i
)
INSERT INTO payment_transactions (
  current_account_id, building_id, payment_type, amount, description, payment_date,
  cash_account_id, bank_account_id, created_at, updated_at
)
SELECT
  ca.id,
  ca.building_id,
  CASE WHEN i % 3 = 0 THEN 'POS' WHEN i % 3 = 1 THEN 'CASH' ELSE 'BANK' END,
  (300 + i * 25)::numeric,
  'TR Min10 tahsilat ' || i,
  CURRENT_TIMESTAMP - ((i % 48) || ' hour')::interval,
  CASE WHEN i % 3 = 1 THEN c.id ELSE NULL END,
  CASE WHEN i % 3 IN (0,2) THEN b.id ELSE NULL END,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id, building_id FROM current_accounts ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM current_accounts)) LIMIT 1) ca ON true
LEFT JOIN LATERAL (SELECT id FROM cash_accounts ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM cash_accounts)) LIMIT 1) c ON true
LEFT JOIN LATERAL (SELECT id FROM bank_accounts ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM bank_accounts)) LIMIT 1) b ON true
WHERE NOT EXISTS (SELECT 1 FROM payment_transactions pt WHERE pt.description = 'TR Min10 tahsilat ' || i);

-- 31) STOCK ITEMS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM stock_items), 10) i
)
INSERT INTO stock_items (
  product_name, stock_group, model_name, unit, vat_rate, purchase_price, sale_price,
  stock_in, stock_out, current_stock, created_at, updated_at
)
SELECT
  'TR Min10 Stok Ürün ' || i,
  CASE WHEN i % 2 = 0 THEN 'Elektrik' ELSE 'Mekanik' END,
  'TRM-' || i,
  'ADET',
  CASE WHEN i % 2 = 0 THEN 20 ELSE 10 END,
  (50 + i * 10)::numeric,
  (80 + i * 12)::numeric,
  (20 + i)::numeric,
  (i % 5)::numeric,
  ((20 + i) - (i % 5))::numeric,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM stock_items si WHERE si.product_name = 'TR Min10 Stok Ürün ' || i);

-- 32) STOCK TRANSFERS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM stock_transfers), 10) i
)
INSERT INTO stock_transfers (from_stock_id, to_stock_id, quantity, transfer_date, note, created_at)
SELECT
  s1.id,
  s2.id,
  1 + (i % 3),
  CURRENT_TIMESTAMP - ((i % 24) || ' hour')::interval,
  'TR Min10 stok transfer notu ' || i,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM stock_items ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM stock_items)) LIMIT 1) s1 ON true
JOIN LATERAL (SELECT id FROM stock_items ORDER BY id OFFSET ((g.i) % (SELECT GREATEST(count(*), 1) FROM stock_items)) LIMIT 1) s2 ON true
WHERE s1.id <> s2.id
  AND NOT EXISTS (SELECT 1 FROM stock_transfers st WHERE st.note = 'TR Min10 stok transfer notu ' || i);

-- 33) STATUS DETECTION REPORTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM status_detection_reports), 10) i
)
INSERT INTO status_detection_reports (
  report_date, building_name, elevator_name, identity_number, status, file_path, note, created_at, updated_at
)
SELECT
  CURRENT_DATE - ((i % 90) || ' day')::interval,
  'TR Min10 Bina Rapor ' || i,
  'TR Min10 Asansör Rapor ' || i,
  'TR-MIN10-ID-' || lpad(i::text, 4, '0'),
  CASE WHEN i % 2 = 0 THEN 'ONAYLANDI' ELSE 'TASLAK' END,
  '/api/files/min10/status_report_' || i || '.pdf',
  'TR Min10 durum tespit raporu ' || i,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
WHERE NOT EXISTS (SELECT 1 FROM status_detection_reports sdr WHERE sdr.note = 'TR Min10 durum tespit raporu ' || i);

-- 34) ELEVATOR LABELS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM elevator_labels), 10) i
)
INSERT INTO elevator_labels (elevator_id, label_name, start_at, end_at, description, file_path, created_at, updated_at)
SELECT
  e.id,
  CASE WHEN i % 3 = 0 THEN 'KIRMIZI' WHEN i % 3 = 1 THEN 'MAVİ' ELSE 'YEŞİL' END,
  CURRENT_TIMESTAMP - ((i % 60) || ' day')::interval,
  CURRENT_TIMESTAMP + ((300 + i) || ' day')::interval,
  'TR Min10 etiket açıklama ' || i,
  '/api/files/min10/etiket_' || i || '.pdf',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
WHERE NOT EXISTS (SELECT 1 FROM elevator_labels el WHERE el.description = 'TR Min10 etiket açıklama ' || i);

-- 35) ELEVATOR CONTRACTS
WITH g AS (
  SELECT generate_series((SELECT count(*) + 1 FROM elevator_contracts), 10) i
)
INSERT INTO elevator_contracts (elevator_id, contract_date, contract_html, file_path, created_at, updated_at)
SELECT
  e.id,
  CURRENT_DATE - ((i % 40) || ' day')::interval,
  '<p>TR Min10 sözleşme metni ' || i || '</p>',
  '/api/files/min10/sozlesme_' || i || '.pdf',
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
FROM g
JOIN LATERAL (SELECT id FROM elevators ORDER BY id OFFSET ((g.i - 1) % (SELECT GREATEST(count(*), 1) FROM elevators)) LIMIT 1) e ON true
WHERE NOT EXISTS (SELECT 1 FROM elevator_contracts ec WHERE ec.contract_html = '<p>TR Min10 sözleşme metni ' || i || '</p>');
