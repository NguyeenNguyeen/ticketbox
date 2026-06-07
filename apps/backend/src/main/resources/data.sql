-- data.sql

-- Insert sample users (password is 'password' encoded with BCrypt)
INSERT INTO users (id, username, password, role)
VALUES 
(1, 'customer1', '$2a$10$slYQmyNdGzTn7ZLBIAChCO218bE4U2r/g.mI1vI5s9J6Q/Y/o3WwW', 'CUSTOMER'),
(2, 'admin1', '$2a$10$slYQmyNdGzTn7ZLBIAChCO218bE4U2r/g.mI1vI5s9J6Q/Y/o3WwW', 'ORGANIZER')
ON CONFLICT (id) DO NOTHING;

-- Insert sample concert (Anh Trai Say Hi)
INSERT INTO concerts (id, name, description, start_time, end_time, location, artist_biography)
VALUES 
(1, 'Anh Trai Say Hi - Live Concert', 'Concert quy tụ 30 anh trai đình đám nhất hiện nay.', '2026-12-20 19:00:00', '2026-12-20 23:00:00', 'Sân vận động Mỹ Đình, Hà Nội', 'HIEUTHUHAI, Rhyder, HURRYKNG, Negav, Quang Hùng MasterD')
ON CONFLICT (id) DO NOTHING;

-- Insert sample ticket categories for the concert
INSERT INTO ticket_categories (id, concert_id, name, price, total_quantity, available_quantity, version)
VALUES 
(1, 1, 'SVIP', 5000000.00, 200, 200, 0),
(2, 1, 'VIP', 3500000.00, 500, 500, 0),
(3, 1, 'CAT1', 2000000.00, 1000, 1000, 0),
(4, 1, 'CAT2', 1200000.00, 1500, 1500, 0),
(5, 1, 'GA', 800000.00, 3000, 3000, 0)
ON CONFLICT (id) DO NOTHING;

-- Adjust sequence so subsequent inserts don't collide with our manual IDs
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('concerts_id_seq', (SELECT MAX(id) FROM concerts));
SELECT setval('ticket_categories_id_seq', (SELECT MAX(id) FROM ticket_categories));
