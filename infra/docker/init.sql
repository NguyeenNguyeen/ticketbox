-- init.sql
CREATE TABLE IF NOT EXISTS concerts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL
);

-- Tạo sẵn một concert mẫu để cả team test
INSERT INTO concerts (name, description, event_date) 
VALUES ('Anh Trai Say Hi - Live Concert', 'Concert quy tụ 30 anh trai', '2026-12-20 19:00:00')
ON CONFLICT DO NOTHING;
