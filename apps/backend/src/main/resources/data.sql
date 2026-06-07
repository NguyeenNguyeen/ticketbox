-- data.sql

-- Insert sample users (password is 'password' encoded with BCrypt)
INSERT INTO users (id, username, password, role)
VALUES 
(1, 'customer1', '$2a$10$slYQmyNdGzTn7ZLBIAChCO218bE4U2r/g.mI1vI5s9J6Q/Y/o3WwW', 'CUSTOMER'),
(2, 'admin1', '$2a$10$slYQmyNdGzTn7ZLBIAChCO218bE4U2r/g.mI1vI5s9J6Q/Y/o3WwW', 'ORGANIZER')
ON CONFLICT (id) DO NOTHING;

-- Insert sample concerts
INSERT INTO concerts (id, name, description, start_time, end_time, location, artist_biography)
VALUES 
(1, 'Anh Trai Say Hi - Live Concert', 'Concert quy tụ 30 anh trai đình đám nhất hiện nay.', '2026-12-20 19:00:00', '2026-12-20 23:00:00', 'Sân vận động Mỹ Đình, Hà Nội', 'HIEUTHUHAI, Rhyder, HURRYKNG, Negav, Quang Hùng MasterD'),
(2, 'Anh Trai Vượt Ngàn Chông Gai', 'Live concert bùng nổ của các anh tài.', '2027-01-15 19:00:00', '2027-01-15 23:00:00', 'Nhà thi đấu Phú Thọ, TP.HCM', 'Tuấn Hưng, Bằng Kiều, Đàm Vĩnh Hưng, Quang Dũng'),
(3, 'Em Xinh Say Hi - Concert', 'Show diễn âm nhạc lãng mạn dành cho giới trẻ.', '2027-02-14 19:00:00', '2027-02-14 23:00:00', 'Trung tâm Hội nghị Quốc gia, Hà Nội', 'Amee, Juky San, Hoàng Duyên, Vũ Cát Tường'),
(4, 'Chị Đẹp Đạp Gió Rẽ Sóng', 'Đêm nhạc thăng hoa của các chị đẹp.', '2027-03-08 19:00:00', '2027-03-08 23:00:00', 'Sân vận động Thống Nhất, TP.HCM', 'Mỹ Tâm, Thu Minh, Hồ Ngọc Hà, Thanh Lam')
ON CONFLICT (id) DO NOTHING;

-- Insert sample ticket categories for the concerts
INSERT INTO ticket_categories (id, concert_id, name, price, total_quantity, available_quantity, version)
VALUES 
-- Concert 1 (Anh Trai Say Hi)
(1, 1, 'SVIP', 5000000.00, 200, 200, 0),
(2, 1, 'VIP', 3500000.00, 500, 500, 0),
(3, 1, 'CAT1', 2000000.00, 1000, 1000, 0),
(4, 1, 'CAT2', 1200000.00, 1500, 1500, 0),
(5, 1, 'GA', 800000.00, 3000, 3000, 0),
-- Concert 2 (Anh Trai Vượt Ngàn Chông Gai)
(6, 2, 'SVIP', 4500000.00, 150, 150, 0),
(7, 2, 'VIP', 3000000.00, 400, 400, 0),
(8, 2, 'CAT1', 1800000.00, 800, 800, 0),
(9, 2, 'CAT2', 1000000.00, 1200, 1200, 0),
(10, 2, 'GA', 600000.00, 2000, 2000, 0),
-- Concert 3 (Em Xinh Say Hi)
(11, 3, 'SVIP', 3000000.00, 100, 100, 0),
(12, 3, 'VIP', 2000000.00, 300, 300, 0),
(13, 3, 'CAT1', 1500000.00, 600, 600, 0),
(14, 3, 'CAT2', 1000000.00, 1000, 1000, 0),
(15, 3, 'GA', 500000.00, 1500, 1500, 0),
-- Concert 4 (Chị Đẹp Đạp Gió Rẽ Sóng)
(16, 4, 'SVIP', 5000000.00, 250, 250, 0),
(17, 4, 'VIP', 3500000.00, 600, 600, 0),
(18, 4, 'CAT1', 2200000.00, 1200, 1200, 0),
(19, 4, 'CAT2', 1500000.00, 1800, 1800, 0),
(20, 4, 'GA', 800000.00, 3500, 3500, 0)
ON CONFLICT (id) DO NOTHING;

-- Adjust sequence so subsequent inserts don't collide with our manual IDs
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('concerts_id_seq', (SELECT MAX(id) FROM concerts));
SELECT setval('ticket_categories_id_seq', (SELECT MAX(id) FROM ticket_categories));
