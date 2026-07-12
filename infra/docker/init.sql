-- init.sql
CREATE TABLE IF NOT EXISTS concerts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    location VARCHAR(255),
    artist_biography TEXT
);

-- Tạo sẵn một concert mẫu để cả team test
INSERT INTO concerts (name, description, start_time, end_time) 
VALUES ('Anh Trai Say Hi - Live Concert', 'Concert quy tụ 30 anh trai', '2026-12-20 19:00:00', '2026-12-20 23:00:00')
ON CONFLICT DO NOTHING;
