-- =============================================================================
-- AgriPrice Rwanda - Demonstration Seed Data
-- Academic Purpose: Final Assessment Test (FAT) - Advanced Web & Internet Programming
-- NOTICE: All records represent realistic fictional demonstration data.
-- Default Password for all demonstration agents: Agent@123456
-- Hashing Algorithm: PBKDF2WithHmacSHA256 (65,536 iterations, 16-byte random salt)
-- =============================================================================

USE agriprice;

-- -----------------------------------------------------------------------------
-- 1. Insert Market Agents (At least 4 agents)
-- -----------------------------------------------------------------------------
INSERT INTO agents (agent_id, full_name, email, phone, password_hash, password_salt, status, created_at) VALUES
(1, 'Delphin Mugisha', 'delphin@agriprice.rw', '+250788123456', 
    '2ce31fb183a1f2c7a3d6aa8fecf5ebce96b7f759f2309da5f6568505b70afe6c', 
    'a12aa70e43891d0b025062c84fcb80b8', 'ACTIVE', '2026-10-20 08:00:00'),
(2, 'Jean Claude Bizimana', 'bizimana@agriprice.rw', '+250788234567', 
    'f3c974010573048a998c7af3b8d61cfe587d4c8f3260ba3d04b1c8e3879dbf34', 
    '78163c5cb38b75942c5e8cd8fc34abc1', 'ACTIVE', '2026-10-20 08:30:00'),
(3, 'Aline Uwase', 'uwase@agriprice.rw', '+250788345678', 
    'f26c041f4330a8fefb811da8b5496babae3f7ef487700832f13baea42dc5678f', 
    '5f334c4d1952649accf73a1a3476d32b', 'ACTIVE', '2026-10-21 09:00:00'),
(4, 'Eric Habimana', 'habimana@agriprice.rw', '+250788456789', 
    'f4d714e583936c25fbb3169b82a51bcc9a91e7cc1ea18af8bb16f09c7db5cf14', 
    '6069ffd4aa1a13b8f6f5b1c68c97210b', 'ACTIVE', '2026-10-21 09:15:00');

-- -----------------------------------------------------------------------------
-- 2. Insert Markets (At least 3 markets; 4 included covering Rubavu & Kigali)
-- -----------------------------------------------------------------------------
INSERT INTO markets (market_id, market_name, district, province, location_description, created_at) VALUES
(1, 'Gisenyi Central Market', 'Rubavu', 'Western Province', 
    'Centrally located near Grand Barriere border with Goma, primary retail market in Rubavu town.', '2026-10-20 08:00:00'),
(2, 'Rubavu Cross-Border Market', 'Rubavu', 'Western Province', 
    'Petite Barriere commercial zone, specializes in high-volume regional cross-border produce trade.', '2026-10-20 08:15:00'),
(3, 'Mahoko Modern Market', 'Rubavu', 'Western Province', 
    'Mahoko Commercial Center along Kigali-Rubavu highway, agricultural aggregation hub direct from farmers.', '2026-10-20 08:30:00'),
(4, 'Nyabugogo Central Market', 'Nyarugenge', 'Kigali City', 
    'National logistics hub and commercial market terminal connecting provincial harvests to the capital city.', '2026-10-20 09:00:00');

-- -----------------------------------------------------------------------------
-- 3. Insert Agricultural Products (At least 5 products; 6 included)
-- -----------------------------------------------------------------------------
INSERT INTO products (product_id, product_name, category, default_unit, description, status) VALUES
(1, 'Maize Grain', 'Cereals', 'kg', 'Clean dry white and yellow maize grain suitable for household posho flour and animal feed.', 'ACTIVE'),
(2, 'Irish Potatoes (Kinigi)', 'Tubers', 'kg', 'Premium volcanic red-skinned table potatoes cultivated in the fertile highlands of Rubavu & Musanze.', 'ACTIVE'),
(3, 'Dry Beans (Colta)', 'Legumes', 'kg', 'High-protein red-speckled bush beans, a daily nutritional staple across Rwanda.', 'ACTIVE'),
(4, 'Cassava Flour', 'Flour/Tubers', 'kg', 'High-grade, finely ground sun-dried cassava flour for making traditional Rwandan ubugali.', 'ACTIVE'),
(5, 'Red Onions', 'Vegetables', 'kg', 'Pungent, dry-cured bulb onions sourced from Rubavu marshlands and local irrigation schemes.', 'ACTIVE'),
(6, 'Bananas (Matooke)', 'Fruits', 'kg', 'Freshly harvested highland cooking green bananas (Igitoki) from Western Province farms.', 'ACTIVE');

-- -----------------------------------------------------------------------------
-- 4. Insert Daily Price Reports (Over 50 reports across 15 distinct days)
-- Date Span: 2026-10-25 to 2026-11-09 (16 calendar days)
-- -----------------------------------------------------------------------------
INSERT INTO price_reports (report_id, product_id, market_id, agent_id, unit, price_rwf, report_date, created_at) VALUES
-- Day 1: 2026-10-25
(1,  1, 1, 1, 'kg', 480.00, '2026-10-25', '2026-10-25 08:10:00'),
(2,  2, 1, 1, 'kg', 420.00, '2026-10-25', '2026-10-25 08:15:00'),
(3,  3, 1, 1, 'kg', 950.00, '2026-10-25', '2026-10-25 08:20:00'),
(4,  1, 2, 2, 'kg', 460.00, '2026-10-25', '2026-10-25 08:45:00'),
(5,  2, 2, 2, 'kg', 400.00, '2026-10-25', '2026-10-25 08:50:00'),
(6,  1, 3, 3, 'kg', 440.00, '2026-10-25', '2026-10-25 09:10:00'),
(7,  2, 3, 3, 'kg', 380.00, '2026-10-25', '2026-10-25 09:15:00'),

-- Day 2: 2026-10-26
(8,  1, 1, 1, 'kg', 490.00, '2026-10-26', '2026-10-26 08:05:00'),
(9,  3, 1, 1, 'kg', 960.00, '2026-10-26', '2026-10-26 08:10:00'),
(10, 4, 1, 1, 'kg', 620.00, '2026-10-26', '2026-10-26 08:15:00'),
(11, 1, 3, 3, 'kg', 450.00, '2026-10-26', '2026-10-26 09:00:00'),
(12, 5, 3, 3, 'kg', 1150.00, '2026-10-26', '2026-10-26 09:05:00'),

-- Day 3: 2026-10-27
(13, 2, 1, 1, 'kg', 430.00, '2026-10-27', '2026-10-27 08:20:00'),
(14, 5, 1, 1, 'kg', 1200.00, '2026-10-27', '2026-10-27 08:25:00'),
(15, 2, 2, 2, 'kg', 410.00, '2026-10-27', '2026-10-27 08:55:00'),
(16, 6, 2, 2, 'kg', 360.00, '2026-10-27', '2026-10-27 09:00:00'),

-- Day 4: 2026-10-28
(17, 1, 1, 1, 'kg', 500.00, '2026-10-28', '2026-10-28 08:10:00'),
(18, 3, 2, 2, 'kg', 940.00, '2026-10-28', '2026-10-28 08:40:00'),
(19, 4, 3, 3, 'kg', 590.00, '2026-10-28', '2026-10-28 09:10:00'),
(20, 6, 3, 3, 'kg', 340.00, '2026-10-28', '2026-10-28 09:15:00'),

-- Day 5: 2026-10-29
(21, 2, 1, 1, 'kg', 440.00, '2026-10-29', '2026-10-29 08:30:00'),
(22, 5, 2, 2, 'kg', 1180.00, '2026-10-29', '2026-10-29 08:50:00'),
(23, 1, 4, 4, 'kg', 540.00, '2026-10-29', '2026-10-29 09:30:00'),
(24, 2, 4, 4, 'kg', 490.00, '2026-10-29', '2026-10-29 09:35:00'),

-- Day 6: 2026-10-30
(25, 3, 1, 1, 'kg', 980.00, '2026-10-30', '2026-10-30 08:15:00'),
(26, 4, 2, 2, 'kg', 610.00, '2026-10-30', '2026-10-30 08:45:00'),
(27, 2, 3, 3, 'kg', 390.00, '2026-10-30', '2026-10-30 09:05:00'),

-- Day 7: 2026-10-31
(28, 1, 1, 1, 'kg', 510.00, '2026-10-31', '2026-10-31 08:10:00'),
(29, 2, 2, 2, 'kg', 420.00, '2026-10-31', '2026-10-31 08:40:00'),
(30, 5, 3, 3, 'kg', 1160.00, '2026-10-31', '2026-10-31 09:10:00'),
(31, 3, 4, 4, 'kg', 1080.00, '2026-10-31', '2026-10-31 09:40:00'),

-- Day 8: 2026-11-01
(32, 1, 2, 2, 'kg', 470.00, '2026-11-01', '2026-11-01 08:20:00'),
(33, 4, 1, 1, 'kg', 630.00, '2026-11-01', '2026-11-01 08:35:00'),
(34, 6, 3, 3, 'kg', 350.00, '2026-11-01', '2026-11-01 09:05:00'),

-- Day 9: 2026-11-02
(35, 2, 1, 1, 'kg', 450.00, '2026-11-02', '2026-11-02 08:15:00'),
(36, 3, 1, 1, 'kg', 1000.00, '2026-11-02', '2026-11-02 08:20:00'),
(37, 5, 2, 2, 'kg', 1220.00, '2026-11-02', '2026-11-02 08:50:00'),
(38, 1, 3, 3, 'kg', 460.00, '2026-11-02', '2026-11-02 09:10:00'),

-- Day 10: 2026-11-03
(39, 1, 1, 1, 'kg', 510.00, '2026-11-03', '2026-11-03 08:10:00'),
(40, 2, 2, 2, 'kg', 430.00, '2026-11-03', '2026-11-03 08:35:00'),
(41, 4, 3, 3, 'kg', 600.00, '2026-11-03', '2026-11-03 09:00:00'),
(42, 5, 4, 4, 'kg', 1350.00, '2026-11-03', '2026-11-03 09:30:00'),

-- Day 11: 2026-11-04
(43, 3, 2, 2, 'kg', 970.00, '2026-11-04', '2026-11-04 08:30:00'),
(44, 5, 1, 1, 'kg', 1250.00, '2026-11-04', '2026-11-04 08:40:00'),
(45, 6, 2, 2, 'kg', 370.00, '2026-11-04', '2026-11-04 08:55:00'),
(46, 2, 3, 3, 'kg', 400.00, '2026-11-04', '2026-11-04 09:15:00'),

-- Day 12: 2026-11-05
(47, 1, 1, 1, 'kg', 520.00, '2026-11-05', '2026-11-05 08:05:00'),
(48, 2, 1, 1, 'kg', 460.00, '2026-11-05', '2026-11-05 08:10:00'),
(49, 1, 2, 2, 'kg', 480.00, '2026-11-05', '2026-11-05 08:40:00'),
(50, 3, 3, 3, 'kg', 940.00, '2026-11-05', '2026-11-05 09:05:00'),

-- Day 13: 2026-11-06
(51, 4, 1, 1, 'kg', 640.00, '2026-11-06', '2026-11-06 08:20:00'),
(52, 5, 2, 2, 'kg', 1230.00, '2026-11-06', '2026-11-06 08:45:00'),
(53, 2, 4, 4, 'kg', 510.00, '2026-11-06', '2026-11-06 09:25:00'),
(54, 6, 4, 4, 'kg', 420.00, '2026-11-06', '2026-11-06 09:30:00'),

-- Day 14: 2026-11-07
(55, 1, 1, 1, 'kg', 530.00, '2026-11-07', '2026-11-07 08:10:00'),
(56, 3, 1, 1, 'kg', 1020.00, '2026-11-07', '2026-11-07 08:15:00'),
(57, 2, 2, 2, 'kg', 440.00, '2026-11-07', '2026-11-07 08:40:00'),
(58, 1, 3, 3, 'kg', 470.00, '2026-11-07', '2026-11-07 09:00:00'),
(59, 2, 3, 3, 'kg', 410.00, '2026-11-07', '2026-11-07 09:05:00'),

-- Day 15: 2026-11-08
(60, 2, 1, 1, 'kg', 470.00, '2026-11-08', '2026-11-08 08:10:00'),
(61, 4, 2, 2, 'kg', 620.00, '2026-11-08', '2026-11-08 08:35:00'),
(62, 5, 3, 3, 'kg', 1190.00, '2026-11-08', '2026-11-08 09:00:00'),
(63, 1, 4, 4, 'kg', 560.00, '2026-11-08', '2026-11-08 09:20:00'),

-- Day 16: 2026-11-09 (Latest prices for current display)
(64, 1, 1, 1, 'kg', 540.00, '2026-11-09', '2026-11-09 08:00:00'),
(65, 2, 1, 1, 'kg', 480.00, '2026-11-09', '2026-11-09 08:05:00'),
(66, 3, 1, 1, 'kg', 1050.00, '2026-11-09', '2026-11-09 08:10:00'),
(67, 4, 1, 1, 'kg', 650.00, '2026-11-09', '2026-11-09 08:15:00'),
(68, 5, 1, 1, 'kg', 1280.00, '2026-11-09', '2026-11-09 08:20:00'),
(69, 6, 1, 1, 'kg', 390.00, '2026-11-09', '2026-11-09 08:25:00'),

(70, 1, 2, 2, 'kg', 490.00, '2026-11-09', '2026-11-09 08:30:00'),
(71, 2, 2, 2, 'kg', 450.00, '2026-11-09', '2026-11-09 08:35:00'),
(72, 3, 2, 2, 'kg', 990.00, '2026-11-09', '2026-11-09 08:40:00'),
(73, 5, 2, 2, 'kg', 1240.00, '2026-11-09', '2026-11-09 08:45:00'),

(74, 1, 3, 3, 'kg', 480.00, '2026-11-09', '2026-11-09 09:00:00'),
(75, 2, 3, 3, 'kg', 420.00, '2026-11-09', '2026-11-09 09:05:00'),
(76, 4, 3, 3, 'kg', 610.00, '2026-11-09', '2026-11-09 09:10:00'),
(77, 6, 3, 3, 'kg', 360.00, '2026-11-09', '2026-11-09 09:15:00'),

(78, 1, 4, 4, 'kg', 570.00, '2026-11-09', '2026-11-09 09:20:00'),
(79, 2, 4, 4, 'kg', 520.00, '2026-11-09', '2026-11-09 09:25:00'),
(80, 3, 4, 4, 'kg', 1100.00, '2026-11-09', '2026-11-09 09:30:00');

-- -----------------------------------------------------------------------------
-- 5. Insert Sample Audit Log Records (Demonstrating traceability and corrections)
-- -----------------------------------------------------------------------------
INSERT INTO audit_log (audit_id, agent_id, report_id, action, old_value, new_value, action_date) VALUES
(1, 1, 17, 'UPDATE_PRICE', '{"price_rwf": 490.00, "reason": "Initial morning survey"}', 
    '{"price_rwf": 500.00, "reason": "Mid-day border fuel surcharge adjustment"}', '2026-10-28 14:22:10'),
(2, 2, 43, 'CORRECT_PRICE_ENTRY', '{"price_rwf": 9700.00, "error": "Decimal slip"}', 
    '{"price_rwf": 970.00, "correction": "Standard market retail rate verified"}', '2026-11-04 11:05:44'),
(3, 3, 62, 'UPDATE_PRICE', '{"price_rwf": 1150.00}', 
    '{"price_rwf": 1190.00, "reason": "Heavy rainfall delayed delivery trucks"}', '2026-11-08 16:18:02');

-- -----------------------------------------------------------------------------
-- 6. Insert Sample Demonstration Watchlist Records
-- -----------------------------------------------------------------------------
INSERT INTO watchlist (watchlist_id, visitor_session_id, product_id, market_id, target_price, created_at) VALUES
(1, 'demo-visitor-session-001', 1, 1, 500.00, '2026-11-05 10:15:00'),
(2, 'demo-visitor-session-001', 2, 1, 450.00, '2026-11-05 10:17:00'),
(3, 'demo-visitor-session-002', 3, 2, 950.00, '2026-11-06 14:20:00'),
(4, 'demo-visitor-session-002', 5, 3, 1150.00, '2026-11-07 11:45:00');
