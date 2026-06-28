-- data.sql

-- Insert sample users (password is 'password' encoded with BCrypt)
-- LƯU Ý: TÀI KHOẢN NÀY KHÔNG DÙNG ĐỂ TEST EMAIL. EMAIL CHỈ HOẠT ĐỘNG VỚI RESEND NẾU BẠN TỰ TẠO TÀI KHOẢN MỚI CÙNG EMAIL RESEND.
INSERT INTO users (id, username, password, email, role, full_name)
VALUES 
(1, 'customer1', '$2a$10$c45d7ef7xEUGOOxi.l4L9.GwTFTzK5kp02CrIfiadl7iffCA6xY1G', 'customer1@gmail.com', 'CUSTOMER', 'Khán Giả 1'),
(2, 'admin1', '$2a$10$c45d7ef7xEUGOOxi.l4L9.GwTFTzK5kp02CrIfiadl7iffCA6xY1G', 'admin1@ticketbox.vn', 'ORGANIZER', 'Quản trị viên 1'),
(3, 'checker1', '$2a$10$c45d7ef7xEUGOOxi.l4L9.GwTFTzK5kp02CrIfiadl7iffCA6xY1G', 'checker1@ticketbox.vn', 'CHECKER', 'Nhân viên soát vé')
ON CONFLICT (id) DO UPDATE SET password = EXCLUDED.password, username = EXCLUDED.username, email = EXCLUDED.email, role = EXCLUDED.role, full_name = EXCLUDED.full_name;

-- Insert sample concerts
INSERT INTO concerts (id, name, description, start_time, end_time, location, sale_start_time, cancelled_status)
VALUES 
(1, 'Anh Trai Say Hi - Live Concert', 'Concert quy tụ 30 anh trai đình đám nhất hiện nay.', '2026-12-20 19:00:00', '2026-12-20 23:00:00', 'Sân vận động Mỹ Đình, Hà Nội', '2026-06-01 00:00:00', NULL),
(2, 'Anh Trai Vượt Ngàn Chông Gai', 'Live concert bùng nổ của các anh tài.', '2027-01-15 19:00:00', '2027-01-15 23:00:00', 'Nhà thi đấu Phú Thọ, TP.HCM', '2026-06-01 00:00:00', NULL),
(3, 'Em Xinh Say Hi - Concert', 'Show diễn âm nhạc lãng mạn dành cho giới trẻ.', '2027-02-14 19:00:00', '2027-02-14 23:00:00', 'Trung tâm Hội nghị Quốc gia, Hà Nội', '2026-06-15 00:00:00', NULL),
(4, 'Chị Đẹp Đạp Gió Rẽ Sóng', 'Đêm nhạc thăng hoa của các chị đẹp.', '2027-03-08 19:00:00', '2027-03-08 23:00:00', 'Sân vận động Thống Nhất, TP.HCM', '2026-07-01 00:00:00', NULL)
ON CONFLICT (id) DO UPDATE SET 
  name = EXCLUDED.name, description = EXCLUDED.description,
  start_time = EXCLUDED.start_time, end_time = EXCLUDED.end_time,
  location = EXCLUDED.location, sale_start_time = EXCLUDED.sale_start_time;

-- Insert sample artists
INSERT INTO artists (id, name, avatar_url, bio)
VALUES
(1, 'HIEUTHUHAI', 'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&q=80&w=200&h=200', 'HIEUTHUHAI sinh năm 1999 tại TP.HCM. Anh nổi lên từ chương trình King Of Rap. Phong cách âm nhạc hiện đại, ngoại hình sáng giúp anh thu hút đông đảo khán giả trẻ.'),
(2, 'Rhyder', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=200&h=200', 'Rhyder tên thật là Quang Anh, Quán quân Giọng Hát Việt Nhí mùa đầu tiên. Sự trở lại mạnh mẽ trong các gameshow gần đây cho thấy sự lột xác ngoạn mục về hình ảnh và tư duy âm nhạc.'),
(3, 'HURRYKNG', 'https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&q=80&w=200&h=200', 'HURRYKNG là một rapper trẻ với nhiều bản hit tạo trend. Sự nghiệp của anh bắt đầu thăng hoa sau khi tham gia các tổ đội Rap tại miền Nam.'),
(4, 'Tuấn Hưng', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&q=80&w=200&h=200', 'Nam ca sĩ kì cựu của âm nhạc Việt Nam với hàng loạt bản hit gắn liền với tuổi thanh xuân của thế hệ 8x, 9x đời đầu. Giọng hát nam tính, mạnh mẽ là điểm đặc trưng.'),
(5, 'Amee', 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200&h=200', 'Nữ ca sĩ Gen Z với phong cách kẹo ngọt, sở hữu hàng loạt MV triệu view và vũ đạo viral trên mạng xã hội TikTok.'),
(6, 'Mỹ Tâm', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=200&h=200', 'Họa mi tóc nâu, tượng đài của âm nhạc Việt Nam trong suốt hơn 20 năm qua. Sở hữu lượng fan đông đảo bậc nhất và những liveshow quy mô kỷ lục.')
ON CONFLICT (id) DO NOTHING;

-- Map artists to concerts
INSERT INTO concert_artists (concert_id, artist_id)
VALUES
(1, 1), (1, 2), (1, 3),
(2, 4),
(3, 5),
(4, 6)
ON CONFLICT DO NOTHING;

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
SELECT setval('artists_id_seq', (SELECT MAX(id) FROM artists));
SELECT setval('ticket_categories_id_seq', (SELECT MAX(id) FROM ticket_categories));
