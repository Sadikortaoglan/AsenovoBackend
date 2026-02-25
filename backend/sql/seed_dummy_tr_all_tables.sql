-- Turkish dummy seed for all domain tables (idempotent)
-- Target DB: sara (local)

DO $$
DECLARE
    v_patron_id BIGINT;
    v_teknisyen_id BIGINT;
    v_admin_id BIGINT;

    v_bina1_id BIGINT;
    v_bina2_id BIGINT;

    v_cari1_id BIGINT;
    v_cari2_id BIGINT;

    v_asansor1_id BIGINT;
    v_asansor2_id BIGINT;

    v_parca1_id BIGINT;
    v_parca2_id BIGINT;

    v_sablon_id BIGINT;
    v_bolum_id BIGINT;
    v_madde1_id BIGINT;
    v_madde2_id BIGINT;

    v_qr_id BIGINT;
    v_plan_tamam_id BIGINT;
    v_plan_planli_id BIGINT;

    v_session_tamam_id BIGINT;
    v_session_devam_id BIGINT;

    v_bakim_id BIGINT;
    v_teklif_id BIGINT;
    v_tahsilat_id BIGINT;

    v_kasa_id BIGINT;
    v_banka_id BIGINT;

    v_stok1_id BIGINT;
    v_stok2_id BIGINT;

    v_sozlesme_id BIGINT;
BEGIN
    -- USERS
    INSERT INTO users (username, password_hash, role, active, created_at)
    VALUES
      ('tr_patron', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PATRON', true, CURRENT_TIMESTAMP),
      ('tr_teknisyen', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PERSONEL', true, CURRENT_TIMESTAMP),
      ('tr_admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMIN', true, CURRENT_TIMESTAMP)
    ON CONFLICT (username) DO NOTHING;

    SELECT id INTO v_patron_id FROM users WHERE username = 'tr_patron';
    SELECT id INTO v_teknisyen_id FROM users WHERE username = 'tr_teknisyen';
    SELECT id INTO v_admin_id FROM users WHERE username = 'tr_admin';

    -- BUILDINGS
    INSERT INTO buildings (name, address, city, district, created_at, updated_at)
    SELECT 'TR DUMMY MERKEZ PLAZA', 'Cumhuriyet Mah. Atatürk Cad. No:45', 'İzmir', 'Konak', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE name = 'TR DUMMY MERKEZ PLAZA');

    INSERT INTO buildings (name, address, city, district, created_at, updated_at)
    SELECT 'TR DUMMY BAHAR SİTESİ', 'Yenişehir Mah. Lale Sok. No:12', 'Ankara', 'Çankaya', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE name = 'TR DUMMY BAHAR SİTESİ');

    SELECT id INTO v_bina1_id FROM buildings WHERE name = 'TR DUMMY MERKEZ PLAZA';
    SELECT id INTO v_bina2_id FROM buildings WHERE name = 'TR DUMMY BAHAR SİTESİ';

    -- CURRENT ACCOUNTS
    INSERT INTO current_accounts (building_id, name, authorized_person, phone, debt, credit, balance, created_at, updated_at)
    SELECT v_bina1_id, 'TR DUMMY MERKEZ PLAZA CARİ', 'Ahmet Yılmaz', '05321234567', 25000, 5000, -20000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM current_accounts WHERE building_id = v_bina1_id);

    INSERT INTO current_accounts (building_id, name, authorized_person, phone, debt, credit, balance, created_at, updated_at)
    SELECT v_bina2_id, 'TR DUMMY BAHAR SİTESİ CARİ', 'Zeynep Kaya', '05329876543', 12000, 3000, -9000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM current_accounts WHERE building_id = v_bina2_id);

    SELECT id INTO v_cari1_id FROM current_accounts WHERE building_id = v_bina1_id;
    SELECT id INTO v_cari2_id FROM current_accounts WHERE building_id = v_bina2_id;

    -- ELEVATORS
    INSERT INTO elevators (
      identity_number, building_name, address, elevator_number, floor_count, capacity, speed,
      technical_notes, drive_type, machine_brand, door_type, installation_year, serial_number,
      control_system, rope, modernization, inspection_date, label_date, label_type, expiry_date, status,
      building_id, blue_label, manager_name, manager_tc_identity_no, manager_phone, manager_email,
      created_at, updated_at
    )
    SELECT
      'TR-ASM-0001', 'TR DUMMY MERKEZ PLAZA', 'Cumhuriyet Mah. Atatürk Cad. No:45', 'A-1', 12, 1000, 1.6,
      'TR_DUMMY asansör teknik notu', 'Traction', 'Mitsubishi', 'Otomatik', 2021, 'TRSN-0001',
      'Monarch', '8 Halat', 'Yok', CURRENT_DATE - INTERVAL '2 month', CURRENT_DATE - INTERVAL '2 month',
      'GREEN'::label_type, CURRENT_DATE + INTERVAL '10 month', 'ACTIVE'::elevator_status,
      v_bina1_id, false, 'Murat Kızılgün', '12345678901', '05331234567', 'murat@example.com',
      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM elevators WHERE identity_number = 'TR-ASM-0001');

    INSERT INTO elevators (
      identity_number, building_name, address, elevator_number, floor_count, capacity, speed,
      technical_notes, drive_type, machine_brand, door_type, installation_year, serial_number,
      control_system, rope, modernization, inspection_date, label_date, label_type, expiry_date, status,
      building_id, blue_label, manager_name, manager_tc_identity_no, manager_phone, manager_email,
      created_at, updated_at
    )
    SELECT
      'TR-ASM-0002', 'TR DUMMY BAHAR SİTESİ', 'Yenişehir Mah. Lale Sok. No:12', 'B-2', 8, 800, 1.2,
      'TR_DUMMY ikinci asansör', 'Hydraulic', 'Otis', 'Yarı Otomatik', 2019, 'TRSN-0002',
      'Arkel', '6 Halat', '2024 Revizyon', CURRENT_DATE - INTERVAL '6 month', CURRENT_DATE - INTERVAL '6 month',
      'YELLOW'::label_type, CURRENT_DATE + INTERVAL '6 month', 'ACTIVE'::elevator_status,
      v_bina2_id, false, 'Gül Demir', '23456789012', '05339876543', 'gul@example.com',
      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM elevators WHERE identity_number = 'TR-ASM-0002');

    SELECT id INTO v_asansor1_id FROM elevators WHERE identity_number = 'TR-ASM-0001';
    SELECT id INTO v_asansor2_id FROM elevators WHERE identity_number = 'TR-ASM-0002';

    -- PARTS
    INSERT INTO parts (name, description, unit_price, stock, created_at)
    SELECT 'TR DUMMY Kapı Kilidi', 'Asansör kapı emniyet kilidi', 1450, 20, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM parts WHERE name = 'TR DUMMY Kapı Kilidi');

    INSERT INTO parts (name, description, unit_price, stock, created_at)
    SELECT 'TR DUMMY Kumanda Kartı', 'Asansör ana kontrol kartı', 3750, 8, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM parts WHERE name = 'TR DUMMY Kumanda Kartı');

    SELECT id INTO v_parca1_id FROM parts WHERE name = 'TR DUMMY Kapı Kilidi';
    SELECT id INTO v_parca2_id FROM parts WHERE name = 'TR DUMMY Kumanda Kartı';

    -- MAINTENANCE TEMPLATE / SECTION / ITEMS
    INSERT INTO maintenance_templates (name, status, frequency_days, created_at, updated_at)
    SELECT 'TR DUMMY Aylık Bakım Şablonu', 'ACTIVE', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM maintenance_templates WHERE name = 'TR DUMMY Aylık Bakım Şablonu');

    SELECT id INTO v_sablon_id FROM maintenance_templates WHERE name = 'TR DUMMY Aylık Bakım Şablonu';

    INSERT INTO maintenance_sections (template_id, name, sort_order, created_at, active)
    SELECT v_sablon_id, 'TR DUMMY Makine Dairesi', 1, CURRENT_TIMESTAMP, true
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_sections WHERE template_id = v_sablon_id AND name = 'TR DUMMY Makine Dairesi'
    );

    SELECT id INTO v_bolum_id FROM maintenance_sections WHERE template_id = v_sablon_id AND name = 'TR DUMMY Makine Dairesi';

    INSERT INTO maintenance_items (section_id, title, description, mandatory, allow_photo, allow_note, sort_order, is_active, created_at)
    SELECT v_bolum_id, 'TR DUMMY Motor Kontrolü', 'Motor sıcaklık ve ses kontrolü', true, true, true, 1, true, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_items WHERE section_id = v_bolum_id AND title = 'TR DUMMY Motor Kontrolü'
    );

    INSERT INTO maintenance_items (section_id, title, description, mandatory, allow_photo, allow_note, sort_order, is_active, created_at)
    SELECT v_bolum_id, 'TR DUMMY Fren Testi', 'Fren tutma ve bırakma testi', true, true, true, 2, true, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_items WHERE section_id = v_bolum_id AND title = 'TR DUMMY Fren Testi'
    );

    SELECT id INTO v_madde1_id FROM maintenance_items WHERE section_id = v_bolum_id AND title = 'TR DUMMY Motor Kontrolü';
    SELECT id INTO v_madde2_id FROM maintenance_items WHERE section_id = v_bolum_id AND title = 'TR DUMMY Fren Testi';

    -- QR PROOF
    INSERT INTO qr_proofs (elevator_id, token_hash, issued_at, expires_at, used_at, used_by, nonce, ip, created_at)
    SELECT v_asansor1_id, 'tr_dummy_qr_token_2026_01', CURRENT_TIMESTAMP - INTERVAL '2 hour', CURRENT_TIMESTAMP + INTERVAL '2 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour', v_teknisyen_id, 'trdummy_nonce_01', '127.0.0.1', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM qr_proofs WHERE token_hash = 'tr_dummy_qr_token_2026_01');

    SELECT id INTO v_qr_id FROM qr_proofs WHERE token_hash = 'tr_dummy_qr_token_2026_01';

    -- MAINTENANCE PLANS
    INSERT INTO maintenance_plans (
      elevator_id, template_id, planned_date, assigned_technician_id, status, created_at,
      completed_at, qr_proof_id, note, price, started_remotely, started_by_role, started_at, started_by_user_id, started_from_ip
    )
    SELECT
      v_asansor1_id, v_sablon_id, CURRENT_DATE - INTERVAL '3 day', v_teknisyen_id, 'COMPLETED', CURRENT_TIMESTAMP,
      CURRENT_TIMESTAMP - INTERVAL '2 day', v_qr_id, 'TR_DUMMY tamamlanan bakım', 2750, false, 'PERSONEL', CURRENT_TIMESTAMP - INTERVAL '3 day', v_teknisyen_id, '192.168.1.10'
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_plans WHERE note = 'TR_DUMMY tamamlanan bakım'
    );

    INSERT INTO maintenance_plans (
      elevator_id, template_id, planned_date, assigned_technician_id, status, created_at,
      started_remotely, started_by_role
    )
    SELECT
      v_asansor2_id, v_sablon_id, CURRENT_DATE + INTERVAL '5 day', v_teknisyen_id, 'PLANNED', CURRENT_TIMESTAMP,
      false, 'PERSONEL'
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_plans WHERE elevator_id = v_asansor2_id AND status = 'PLANNED' AND planned_date = CURRENT_DATE + INTERVAL '5 day'
    );

    SELECT id INTO v_plan_tamam_id FROM maintenance_plans WHERE note = 'TR_DUMMY tamamlanan bakım' LIMIT 1;
    SELECT id INTO v_plan_planli_id FROM maintenance_plans WHERE elevator_id = v_asansor2_id AND status = 'PLANNED' ORDER BY id DESC LIMIT 1;

    -- MAINTENANCE SESSIONS
    INSERT INTO maintenance_sessions (
      plan_id, elevator_id, template_id, technician_id, start_at, end_at, status,
      started_by_qr_scan, qr_proof_id, gps_lat, gps_lng, device_info, overall_note, signature_url, created_at
    )
    SELECT
      v_plan_tamam_id, v_asansor1_id, v_sablon_id, v_teknisyen_id, CURRENT_TIMESTAMP - INTERVAL '3 day', CURRENT_TIMESTAMP - INTERVAL '3 day' + INTERVAL '55 minute', 'COMPLETED',
      true, v_qr_id, 38.4237, 27.1428, 'Android TR Dummy', 'TR_DUMMY oturum tamamlandı', '/imza/tr_dummy_1.png', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_sessions WHERE overall_note = 'TR_DUMMY oturum tamamlandı'
    );

    INSERT INTO maintenance_sessions (
      plan_id, elevator_id, template_id, technician_id, start_at, end_at, status,
      started_by_qr_scan, qr_proof_id, gps_lat, gps_lng, device_info, overall_note, created_at
    )
    SELECT
      v_plan_planli_id, v_asansor2_id, v_sablon_id, v_teknisyen_id, CURRENT_TIMESTAMP - INTERVAL '30 minute', NULL, 'IN_PROGRESS',
      false, NULL, 39.9208, 32.8541, 'Android TR Dummy', 'TR_DUMMY oturum devam ediyor', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_sessions WHERE overall_note = 'TR_DUMMY oturum devam ediyor'
    );

    SELECT id INTO v_session_tamam_id FROM maintenance_sessions WHERE overall_note = 'TR_DUMMY oturum tamamlandı' LIMIT 1;
    SELECT id INTO v_session_devam_id FROM maintenance_sessions WHERE overall_note = 'TR_DUMMY oturum devam ediyor' LIMIT 1;

    -- STEP RESULTS
    INSERT INTO maintenance_step_results (session_id, item_id, result, note, created_at, updated_at)
    SELECT v_session_tamam_id, v_madde1_id, 'COMPLETED', 'TR_DUMMY motor normal', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_step_results WHERE session_id = v_session_tamam_id AND item_id = v_madde1_id
    );

    INSERT INTO maintenance_step_results (session_id, item_id, result, note, created_at, updated_at)
    SELECT v_session_tamam_id, v_madde2_id, 'ISSUE_FOUND', 'TR_DUMMY fren balatası aşınmış', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_step_results WHERE session_id = v_session_tamam_id AND item_id = v_madde2_id
    );

    -- ATTACHMENTS / PHOTOS
    INSERT INTO maintenance_attachments (session_id, item_id, file_url, file_type, created_at)
    SELECT v_session_tamam_id, v_madde1_id, '/api/files/maintenance/tr_dummy_motor.jpg', 'PHOTO', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_attachments WHERE file_url = '/api/files/maintenance/tr_dummy_motor.jpg'
    );

    INSERT INTO maintenance_plan_photos (plan_id, file_url, file_type, uploaded_by, created_at)
    SELECT v_plan_tamam_id, '/api/files/maintenance-plan/tr_dummy_plan_1.jpg', 'PHOTO', v_teknisyen_id, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenance_plan_photos WHERE file_url = '/api/files/maintenance-plan/tr_dummy_plan_1.jpg'
    );

    -- MAINTENANCES
    INSERT INTO maintenances (elevator_id, date, label_type, description, technician_user_id, amount, is_paid, payment_date, created_at)
    SELECT v_asansor1_id, CURRENT_DATE - INTERVAL '2 day', 'GREEN'::label_type, 'TR_DUMMY aylık bakım tamamlandı', v_teknisyen_id, 2750, true, CURRENT_DATE - INTERVAL '1 day', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM maintenances WHERE description = 'TR_DUMMY aylık bakım tamamlandı'
    );

    SELECT id INTO v_bakim_id FROM maintenances WHERE description = 'TR_DUMMY aylık bakım tamamlandı' LIMIT 1;

    -- FAULTS
    INSERT INTO faults (elevator_id, fault_subject, contact_person, building_authorized_message, description, status, created_at)
    SELECT v_asansor2_id, 'Kapı sensör hatası', 'Mehmet Usta', 'Asansör sık sık kat arasında kalıyor', 'TR_DUMMY arıza kaydı', 'OPEN', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM faults WHERE description = 'TR_DUMMY arıza kaydı');

    -- INSPECTIONS
    INSERT INTO inspections (elevator_id, date, result, description, inspection_color, contacted_person_name, report_no, created_at)
    SELECT v_asansor1_id, CURRENT_DATE - INTERVAL '20 day', 'PASSED', 'TR_DUMMY periyodik muayene geçti', 'GREEN'::inspection_color, 'Site Yöneticisi Ahmet', 'TR-RPR-2026-001', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM inspections WHERE report_no = 'TR-RPR-2026-001');

    -- OFFERS / OFFER ITEMS
    INSERT INTO offers (elevator_id, date, vat_rate, discount_amount, subtotal, total_amount, status, created_at)
    SELECT v_asansor1_id, CURRENT_DATE - INTERVAL '5 day', 20, 100, 5200, 6140, 'PENDING', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM offers WHERE elevator_id = v_asansor1_id AND subtotal = 5200 AND total_amount = 6140
    );

    SELECT id INTO v_teklif_id FROM offers WHERE elevator_id = v_asansor1_id AND subtotal = 5200 ORDER BY id DESC LIMIT 1;

    INSERT INTO offer_items (offer_id, part_id, quantity, unit_price, line_total)
    SELECT v_teklif_id, v_parca1_id, 2, 1450, 2900
    WHERE NOT EXISTS (SELECT 1 FROM offer_items WHERE offer_id = v_teklif_id AND part_id = v_parca1_id);

    INSERT INTO offer_items (offer_id, part_id, quantity, unit_price, line_total)
    SELECT v_teklif_id, v_parca2_id, 1, 2300, 2300
    WHERE NOT EXISTS (SELECT 1 FROM offer_items WHERE offer_id = v_teklif_id AND part_id = v_parca2_id);

    -- PAYMENT RECEIPTS
    INSERT INTO payment_receipts (maintenance_id, amount, payer_name, date, note, created_at)
    SELECT v_bakim_id, 2750, 'TR DUMMY MERKEZ PLAZA CARİ', CURRENT_DATE - INTERVAL '1 day', 'TR_DUMMY ödeme tahsil edildi', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM payment_receipts WHERE note = 'TR_DUMMY ödeme tahsil edildi');

    -- REVISION OFFERS
    INSERT INTO revision_offers (elevator_id, building_id, current_account_id, parts_total, labor_total, total_price, status, created_at, updated_at)
    SELECT v_asansor1_id, v_bina1_id, v_cari1_id, 4200, 1800, 6000, 'DRAFT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM revision_offers WHERE elevator_id = v_asansor1_id AND total_price = 6000
    );

    -- REFRESH TOKENS
    INSERT INTO refresh_tokens (user_id, token, expires_at, created_at, revoked_at, is_revoked)
    SELECT v_patron_id, 'tr_dummy_refresh_token_2026', CURRENT_TIMESTAMP + INTERVAL '7 day', CURRENT_TIMESTAMP, NULL, false
    WHERE NOT EXISTS (SELECT 1 FROM refresh_tokens WHERE token = 'tr_dummy_refresh_token_2026');

    -- EDM SETTINGS
    INSERT INTO edm_settings (username, encrypted_password, email, invoice_series_earchive, invoice_series_efatura, mode, created_at, updated_at)
    SELECT 'tr_edm_kullanici', 'TR_DUMMY_ENCRYPTED_SECRET', 'edm.tr@example.com', 'TRA', 'TRF', 'PRODUCTION', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM edm_settings WHERE username = 'tr_edm_kullanici');

    -- INVOICE RECORDS
    INSERT INTO invoice_records (
      invoice_no, invoice_date, direction, profile, status, sender_name, sender_vkn_tckn,
      receiver_name, receiver_vkn_tckn, currency, amount, note, source, maintenance_plan_id,
      created_at, updated_at
    )
    SELECT
      'TR-FAT-OUT-0001', CURRENT_DATE - INTERVAL '1 day', 'OUTGOING', 'TICARI FATURA', 'SUCCESS', NULL, NULL,
      'TR DUMMY MERKEZ PLAZA', '1234567890', 'TRY', 2750, 'TR_DUMMY giden fatura', 'MANUAL', v_plan_tamam_id,
      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM invoice_records WHERE invoice_no = 'TR-FAT-OUT-0001');

    INSERT INTO invoice_records (
      invoice_no, invoice_date, direction, profile, status, sender_name, sender_vkn_tckn,
      receiver_name, receiver_vkn_tckn, currency, amount, note, source,
      created_at, updated_at
    )
    SELECT
      'TR-FAT-IN-0001', CURRENT_DATE - INTERVAL '2 day', 'INCOMING', 'E-FATURA', 'SUCCESS', 'Anadolu Asansör A.Ş.', '9988776655',
      NULL, NULL, 'TRY', 3890, 'TR_DUMMY gelen fatura', 'MANUAL',
      CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM invoice_records WHERE invoice_no = 'TR-FAT-IN-0001');

    -- CASH / BANK
    INSERT INTO cash_accounts (name, currency, total_in, total_out, balance, created_at, updated_at)
    SELECT 'TR DUMMY KASA', 'TRY', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM cash_accounts WHERE name = 'TR DUMMY KASA');

    INSERT INTO bank_accounts (name, currency, total_in, total_out, balance, created_at, updated_at)
    SELECT 'TR DUMMY BANKA', 'TRY', 0, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM bank_accounts WHERE name = 'TR DUMMY BANKA');

    SELECT id INTO v_kasa_id FROM cash_accounts WHERE name = 'TR DUMMY KASA';
    SELECT id INTO v_banka_id FROM bank_accounts WHERE name = 'TR DUMMY BANKA';

    -- PAYMENT TRANSACTIONS
    INSERT INTO payment_transactions (
      current_account_id, building_id, payment_type, amount, description, payment_date,
      cash_account_id, bank_account_id, created_at, updated_at
    )
    SELECT v_cari1_id, v_bina1_id, 'CASH', 1500, 'TR_DUMMY nakit tahsilat', CURRENT_TIMESTAMP - INTERVAL '1 day', v_kasa_id, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM payment_transactions WHERE description = 'TR_DUMMY nakit tahsilat');

    INSERT INTO payment_transactions (
      current_account_id, building_id, payment_type, amount, description, payment_date,
      cash_account_id, bank_account_id, created_at, updated_at
    )
    SELECT v_cari2_id, v_bina2_id, 'BANK', 2200, 'TR_DUMMY banka tahsilat', CURRENT_TIMESTAMP - INTERVAL '12 hour', NULL, v_banka_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM payment_transactions WHERE description = 'TR_DUMMY banka tahsilat');

    -- STOCK
    INSERT INTO stock_items (
      product_name, stock_group, model_name, unit, vat_rate, purchase_price, sale_price,
      stock_in, stock_out, current_stock, created_at, updated_at
    )
    SELECT 'TR DUMMY Kapı Fotoseli', 'Güvenlik', 'KF-2026', 'ADET', 20, 450, 680, 50, 5, 45, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM stock_items WHERE product_name = 'TR DUMMY Kapı Fotoseli');

    INSERT INTO stock_items (
      product_name, stock_group, model_name, unit, vat_rate, purchase_price, sale_price,
      stock_in, stock_out, current_stock, created_at, updated_at
    )
    SELECT 'TR DUMMY Kumanda Butonu', 'Elektrik', 'KB-100', 'ADET', 10, 120, 195, 80, 20, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM stock_items WHERE product_name = 'TR DUMMY Kumanda Butonu');

    SELECT id INTO v_stok1_id FROM stock_items WHERE product_name = 'TR DUMMY Kapı Fotoseli';
    SELECT id INTO v_stok2_id FROM stock_items WHERE product_name = 'TR DUMMY Kumanda Butonu';

    INSERT INTO stock_transfers (from_stock_id, to_stock_id, quantity, transfer_date, note, created_at)
    SELECT v_stok1_id, v_stok2_id, 3, CURRENT_TIMESTAMP - INTERVAL '6 hour', 'TR_DUMMY stok transferi', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM stock_transfers WHERE note = 'TR_DUMMY stok transferi');

    -- STATUS DETECTION REPORTS
    INSERT INTO status_detection_reports (
      report_date, building_name, elevator_name, identity_number, status, file_path, note, created_at, updated_at
    )
    SELECT CURRENT_DATE - INTERVAL '10 day', 'TR DUMMY MERKEZ PLAZA', 'TR DUMMY ASANSÖR-1', 'TR-ASM-0001', 'ONAYLANDI', '/api/files/reports/tr_dummy_rapor_1.pdf', 'TR_DUMMY durum tespit raporu', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM status_detection_reports WHERE note = 'TR_DUMMY durum tespit raporu');

    -- ELEVATOR LABELS
    INSERT INTO elevator_labels (elevator_id, label_name, start_at, end_at, description, file_path, created_at, updated_at)
    SELECT v_asansor1_id, 'MAVİ', CURRENT_TIMESTAMP - INTERVAL '30 day', CURRENT_TIMESTAMP + INTERVAL '335 day', 'TR_DUMMY etiket kaydı', '/api/files/labels/tr_dummy_label.pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM elevator_labels WHERE description = 'TR_DUMMY etiket kaydı');

    -- ELEVATOR CONTRACTS
    INSERT INTO elevator_contracts (elevator_id, contract_date, contract_html, file_path, created_at, updated_at)
    SELECT v_asansor1_id, CURRENT_DATE - INTERVAL '15 day', '<p>TR_DUMMY bakım sözleşmesi metni</p>', '/api/files/contracts/tr_dummy_contract.pdf', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM elevator_contracts WHERE contract_html = '<p>TR_DUMMY bakım sözleşmesi metni</p>');

    SELECT id INTO v_sozlesme_id FROM elevator_contracts WHERE elevator_id = v_asansor1_id ORDER BY id DESC LIMIT 1;

    -- FILE ATTACHMENTS
    INSERT INTO file_attachments (
      entity_type, entity_id, file_name, content_type, size, storage_key, url, uploaded_by_user_id, created_at
    )
    SELECT 'ELEVATOR', v_asansor1_id, 'tr_dummy_asansor_dosya.pdf', 'application/pdf', 102400, 'elevator/' || v_asansor1_id || '/tr_dummy_asansor_dosya.pdf', '/api/files/elevator/tr_dummy_asansor_dosya.pdf', v_admin_id, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM file_attachments WHERE storage_key = 'elevator/' || v_asansor1_id || '/tr_dummy_asansor_dosya.pdf');

    INSERT INTO file_attachments (
      entity_type, entity_id, file_name, content_type, size, storage_key, url, uploaded_by_user_id, created_at
    )
    SELECT 'MAINTENANCE', v_bakim_id, 'tr_dummy_bakim_fotograf.jpg', 'image/jpeg', 512000, 'maintenance/' || v_bakim_id || '/tr_dummy_bakim_fotograf.jpg', '/api/files/maintenance/tr_dummy_bakim_fotograf.jpg', v_teknisyen_id, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM file_attachments WHERE storage_key = 'maintenance/' || v_bakim_id || '/tr_dummy_bakim_fotograf.jpg');

    INSERT INTO file_attachments (
      entity_type, entity_id, file_name, content_type, size, storage_key, url, uploaded_by_user_id, created_at
    )
    SELECT 'OFFER', v_teklif_id, 'tr_dummy_teklif_ek.xlsx', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 20480, 'offer/' || v_teklif_id || '/tr_dummy_teklif_ek.xlsx', '/api/files/offer/tr_dummy_teklif_ek.xlsx', v_patron_id, CURRENT_TIMESTAMP
    WHERE NOT EXISTS (SELECT 1 FROM file_attachments WHERE storage_key = 'offer/' || v_teklif_id || '/tr_dummy_teklif_ek.xlsx');

    -- QR SCAN LOGS
    INSERT INTO qr_scan_logs (
      elevator_id, technician_id, session_id, qr_proof_id, scan_timestamp,
      gps_lat, gps_lng, device_info, ip_address, is_valid, validation_error, created_at
    )
    SELECT v_asansor1_id, v_teknisyen_id, v_session_tamam_id, v_qr_id, CURRENT_TIMESTAMP - INTERVAL '3 day',
           38.4237, 27.1428, 'Android TR Dummy', '192.168.1.10', true, 'TR_DUMMY', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM qr_scan_logs WHERE validation_error = 'TR_DUMMY' AND qr_proof_id = v_qr_id
    );

    -- AUDIT LOGS
    INSERT INTO audit_logs (user_id, action, entity_type, entity_id, metadata_json, created_at)
    SELECT v_admin_id, 'TR_DUMMY_SEED_EXECUTED', 'SYSTEM', 0, '{"kaynak":"seed_dummy_tr_all_tables.sql","dil":"tr"}', CURRENT_TIMESTAMP
    WHERE NOT EXISTS (
      SELECT 1 FROM audit_logs WHERE action = 'TR_DUMMY_SEED_EXECUTED'
    );

    RAISE NOTICE 'TR dummy seed completed successfully.';
END $$;
