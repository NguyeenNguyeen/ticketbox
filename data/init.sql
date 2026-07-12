-- Script khởi tạo cơ sở dữ liệu cho Ticketbox
-- Tạo bảng cấu hình ứng dụng cơ bản
CREATE TABLE IF NOT EXISTS app_settings (
    id SERIAL PRIMARY KEY,
    key_name VARCHAR(100) UNIQUE NOT NULL,
    key_value TEXT
);

INSERT INTO app_settings (key_name, key_value)
VALUES ('app_name', 'Ticketbox')
ON CONFLICT (key_name) DO NOTHING;
