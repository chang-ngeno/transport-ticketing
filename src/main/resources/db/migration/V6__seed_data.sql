-- =============================================================
-- V6 – Seed data for testing (idempotent)
-- Passwords are BCrypt-hashed value of "password123"
-- M-PESA credential columns left NULL (require runtime encryption)
-- =============================================================

-- ── Tenants ───────────────────────────────────────────────────
INSERT INTO tenant (id, name, mpesa_shortcode, mpesa_encryption_salt,
                    mpesa_consumer_key_encrypted, mpesa_consumer_secret_encrypted, mpesa_passkey_encrypted)
VALUES
  (1, 'Nairobi Express Sacco',  '174379', 'abcdef1234567890', NULL, NULL, NULL),
  (2, 'Mombasa Link Transport', '600000', 'fedcba0987654321', NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

SELECT setval('tenant_id_seq', GREATEST((SELECT MAX(id) FROM tenant), 1));

-- ── Stages ────────────────────────────────────────────────────
INSERT INTO stage (id, tenant_id, name, location) VALUES
  (1, 1, 'Githurai 45',   'Along Thika Road, near Total Petrol Station'),
  (2, 1, 'Ruiru Stage',   'Ruiru Town, opposite Equity Bank'),
  (3, 1, 'CBD Archives',  'Nairobi CBD, Archives Bus Stop'),
  (4, 2, 'Mombasa Ferry', 'Likoni Ferry Crossing, South Coast'),
  (5, 2, 'Nyali Stage',   'Nyali Bridge Roundabout')
ON CONFLICT (id) DO NOTHING;

SELECT setval('stage_id_seq', GREATEST((SELECT MAX(id) FROM stage), 1));

-- ── Users ─────────────────────────────────────────────────────
-- BCrypt hash of "password123"
INSERT INTO app_user (id, username, password, role, tenant_id, stage_id) VALUES
  (1, 'tenant_admin_nbi', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'TENANT_ADMIN',    1, NULL),
  (2, 'stage_head_g45',   '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'STAGE_HEAD',      1, 1),
  (3, 'attendant_g45',    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'STAGE_ATTENDANT', 1, 1),
  (4, 'attendant_ruiru',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'STAGE_ATTENDANT', 1, 2),
  (5, 'tenant_admin_msa', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'TENANT_ADMIN',    2, NULL),
  (6, 'stage_head_ferry', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVYYnLq2tm', 'STAGE_HEAD',      2, 4)
ON CONFLICT (id) DO NOTHING;

SELECT setval('app_user_id_seq', GREATEST((SELECT MAX(id) FROM app_user), 1));

-- ── Vehicles ──────────────────────────────────────────────────
INSERT INTO vehicle (id, stage_id, registration_number, capacity, is_active) VALUES
  (1, 1, 'KDA 001A', 14, TRUE),
  (2, 1, 'KDA 002B', 14, TRUE),
  (3, 1, 'KCB 100C', 33, TRUE),
  (4, 2, 'KDD 300D', 14, TRUE),
  (5, 2, 'KDE 400E', 14, FALSE),
  (6, 4, 'KCF 500F', 14, TRUE),
  (7, 4, 'KCG 600G', 33, TRUE)
ON CONFLICT (id) DO NOTHING;

SELECT setval('vehicle_id_seq', GREATEST((SELECT MAX(id) FROM vehicle), 1));

-- ── Trips ─────────────────────────────────────────────────────
INSERT INTO trip (id, tenant_id, from_stage_id, to_destination, route,
                  departure_time, total_seats, booked_seats, price_per_seat) VALUES
  (1,  1, 1, 'Nairobi CBD',          'via Thika Rd',        NOW() + INTERVAL '1 hour',     14, 3,  100.00),
  (2,  1, 1, 'Nairobi CBD',          'via Thika Rd',        NOW() + INTERVAL '3 hours',    14, 0,  100.00),
  (3,  1, 1, 'Nairobi CBD',          'via Thika Rd',        NOW() + INTERVAL '5 hours',    14, 0,  100.00),
  (4,  1, 2, 'Nairobi CBD',          'via Eastern Bypass',  NOW() + INTERVAL '2 hours',    14, 5,  120.00),
  (5,  1, 2, 'Jomo Kenyatta Airport','via Eastern Bypass',  NOW() + INTERVAL '4 hours',    14, 2,  350.00),
  (6,  1, 3, 'Githurai 45',          'via Thika Rd',        NOW() + INTERVAL '1 hour',     14, 0,   90.00),
  (7,  1, 1, 'Nairobi CBD',          'via Thika Rd',        NOW() - INTERVAL '2 days',     14, 14, 100.00),
  (8,  1, 1, 'Nairobi CBD',          'via Thika Rd',        NOW() - INTERVAL '1 day',      14, 10, 100.00),
  (9,  2, 4, 'Mombasa CBD',          'via Nyali Bridge',    NOW() + INTERVAL '90 minutes', 14, 2,   80.00),
  (10, 2, 5, 'Likoni Ferry',         'Direct',              NOW() + INTERVAL '2 hours',    14, 0,   60.00)
ON CONFLICT (id) DO NOTHING;

SELECT setval('trip_id_seq', GREATEST((SELECT MAX(id) FROM trip), 1));

-- ── Fare windows ──────────────────────────────────────────────
INSERT INTO fare (trip_id, effective_from, effective_to, price_per_seat, created_by)
SELECT * FROM (VALUES
  (1, NOW() - INTERVAL '30 minutes', NOW() + INTERVAL '2 hours',  150.00::DECIMAL, 2),
  (2, NOW() + INTERVAL '2 hours',    NOW() + INTERVAL '4 hours',  130.00::DECIMAL, 2),
  (4, NOW() + INTERVAL '1 hour',     NOW() + INTERVAL '3 hours',  160.00::DECIMAL, 2),
  (5, NOW() + INTERVAL '3 hours',    NULL::TIMESTAMP,              400.00::DECIMAL, 1)
) AS v(trip_id, effective_from, effective_to, price_per_seat, created_by)
WHERE NOT EXISTS (SELECT 1 FROM fare WHERE fare.trip_id = v.trip_id);

-- ── Bookings ──────────────────────────────────────────────────
INSERT INTO booking (id, tenant_id, trip_id, ticket_id, checkout_request_id,
                     phone_number, amount, price_paid, status, created_at) VALUES
  (1,  1, 1, 'TKT-A1B2C3D4', 'ws_CO_001', '+254711000001', 150.00, 150.00, 'PAID',    NOW() - INTERVAL '50 minutes'),
  (2,  1, 1, 'TKT-B2C3D4E5', 'ws_CO_002', '+254711000002', 150.00, 150.00, 'PAID',    NOW() - INTERVAL '45 minutes'),
  (3,  1, 1, 'TKT-C3D4E5F6', 'ws_CO_003', '+254711000003', 150.00, 150.00, 'PENDING', NOW() - INTERVAL '10 minutes'),
  (4,  1, 4, 'TKT-D4E5F6G7', 'ws_CO_004', '+254722000001', 160.00, 160.00, 'PAID',    NOW() - INTERVAL '1 hour'),
  (5,  1, 4, 'TKT-E5F6G7H8', 'ws_CO_005', '+254722000002', 160.00, 160.00, 'PAID',    NOW() - INTERVAL '55 minutes'),
  (6,  1, 4, 'TKT-F6G7H8I9', 'ws_CO_006', '+254722000003', 160.00, 160.00, 'PAID',    NOW() - INTERVAL '50 minutes'),
  (7,  1, 4, 'TKT-G7H8I9J0', 'ws_CO_007', '+254722000004', 160.00, 160.00, 'PAID',    NOW() - INTERVAL '48 minutes'),
  (8,  1, 4, 'TKT-H8I9J0K1', 'ws_CO_008', '+254722000005', 160.00, 160.00, 'FAILED',  NOW() - INTERVAL '40 minutes'),
  (9,  1, 7, 'TKT-I9J0K1L2', NULL,        '+254733000001', 100.00, 100.00, 'PAID',    NOW() - INTERVAL '2 days'),
  (10, 1, 7, 'TKT-J0K1L2M3', NULL,        '+254733000002', 100.00, 100.00, 'PAID',    NOW() - INTERVAL '2 days'),
  (11, 1, 7, 'TKT-K1L2M3N4', NULL,        '+254733000003', 100.00, 100.00, 'FAILED',  NOW() - INTERVAL '2 days'),
  (12, 1, 8, 'TKT-L2M3N4O5', NULL,        '+254744000001', 100.00, 100.00, 'PAID',    NOW() - INTERVAL '1 day'),
  (13, 1, 8, 'TKT-M3N4O5P6', NULL,        '+254744000002', 100.00, 100.00, 'PAID',    NOW() - INTERVAL '1 day'),
  (14, 2, 9, 'TKT-N4O5P6Q7', 'ws_CO_014', '+254755000001',  80.00,  80.00, 'PAID',    NOW() - INTERVAL '1 hour'),
  (15, 2, 9, 'TKT-O5P6Q7R8', 'ws_CO_015', '+254755000002',  80.00,  80.00, 'PENDING', NOW() - INTERVAL '30 minutes')
ON CONFLICT (id) DO NOTHING;

SELECT setval('booking_id_seq', GREATEST((SELECT MAX(id) FROM booking), 1));
