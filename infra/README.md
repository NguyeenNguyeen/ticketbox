# Hướng dẫn cài đặt Docker và khởi động môi trường TicketBox (Windows & macOS)

## 1. Cấu trúc dự án

```text
ticketbox/
│
├── README.md
├── .gitignore
├── .env.example
│
├── docs/
│   ├── architecture/
│   ├── api/
│   └── reports/
│
├── apps/
│   ├── frontend/
│   ├── backend/
│   └── mobile/
│
├── infra/
│   └── docker/
│       ├── docker-compose.yml
│       ├── init.sql
│   └── README.md
│
└── assets/
```

Lưu ý:

* `.env.example` nằm ở thư mục gốc dự án.
* Người dùng sẽ tạo file `.env` tại thư mục gốc.
* Docker Compose sẽ đọc các biến môi trường từ file này.

---

## 2. Yêu cầu hệ thống

### Windows

* Windows 10/11 64-bit
* RAM tối thiểu: 8 GB
* Dung lượng trống tối thiểu: 5 GB
* Đã bật Virtualization trong BIOS

### macOS

* macOS 13 trở lên
* Intel hoặc Apple Silicon (M1/M2/M3)
* RAM tối thiểu: 8 GB

---

## 3. Cài đặt Docker Desktop

Tải Docker Desktop:

https://www.docker.com/products/docker-desktop/

Chọn đúng phiên bản:

* Windows
* Mac Intel
* Mac Apple Silicon

Sau khi cài đặt, mở Terminal hoặc PowerShell:

```bash
docker --version
```

Kiểm tra Docker Compose:

```bash
docker compose version
```

Nếu xuất hiện phiên bản nghĩa là cài đặt thành công.

---

## 4. Tạo file môi trường

### Windows

```bash
copy .env.example .env
```

### macOS

```bash
cp .env.example .env
```

Hoặc tạo thủ công:

```text
POSTGRES_DB=ticketbox
POSTGRES_USER=ticketbox
POSTGRES_PASSWORD=ticketbox

GEMINI_API_KEY=your_apikey
RESEND_API_KEY=your_apikey
```

File `.env` phải nằm tại:

```text
ticketbox/.env
```

---

## 5. Di chuyển tới thư mục Docker

```bash
cd infra/docker
```

Kiểm tra:

```text
ticketbox/
└── infra/
    └── docker/
        ├── docker-compose.yml
        ├── init.sql --chỉ dùng cho việc thử kết nối postgreSQL, không dùng cho app
    └── README.md
```

---

## 6. Khởi động các dịch vụ

Từ thư mục:

```text
ticketbox/infra/docker
```

chạy:

```bash
docker compose up -d
```

Docker sẽ tự động tải:

* postgres:15-alpine
* redis:7-alpine
* rabbitmq:3-management-alpine

Lần đầu có thể mất vài phút.

---

## 7. Kiểm tra container

```bash
docker ps
```

Kết quả mong muốn:

```text
ticketbox_postgres
ticketbox_redis
ticketbox_rabbitmq
```

Trạng thái:

```text
Up
```

---

## 8. Kiểm tra PostgreSQL

```bash
docker exec -it ticketbox_postgres sh
```

```bash
psql -U ticketbox -d ticketbox
```

Kiểm tra:

```sql
SELECT version();
```

Nếu trả về thông tin PostgreSQL nghĩa là thành công.

Thoát:

```sql
\q rồi Enter
```
và để thoát khỏi shell

```bash
exit hoặc Ctrl + D
```
---

## 9. Kiểm tra Redis

```bash
docker exec -it ticketbox_redis redis-cli
```

```redis
PING
```

Kết quả:

```text
PONG
```

Kiểm tra ghi dữ liệu:

```redis
SET test hello
GET test
```

Kết quả:

```text
"hello"
```

---

## 10. Kiểm tra RabbitMQ

Mở trình duyệt:

```text
http://localhost:15672
```

Đăng nhập:

```text
Username: ticketbox
Password: ticketbox
```

Nếu xuất hiện Dashboard RabbitMQ nghĩa là thành công.

---

## 11. Các cổng dịch vụ

### PostgreSQL

```text
Host: localhost
Port: 5432

Database: ticketbox
Username: ticketbox
Password: ticketbox
```

### Redis

```text
Host: localhost
Port: 6379
```

### RabbitMQ

```text
Host: localhost
Port: 5672
```

### RabbitMQ Dashboard

```text
http://localhost:15672
Username: ticketbox
Password: ticketbox
```

---

## 12. Kết nối Spring Boot

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ticketbox
    username: ticketbox
    password: ticketbox
```

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: ticketbox
    password: ticketbox
```

---

## 13. Các lệnh Docker thường dùng

Khởi động:

```bash
docker compose up -d
```

Dừng:

```bash
docker compose stop
```

Khởi động lại:

```bash
docker compose restart
```

Xem container:

```bash
docker ps
```

Xem log:

```bash
docker logs ticketbox_rabbitmq
```

Tắt và xóa container:

```bash
docker compose down
```

Tắt và xóa cả dữ liệu:

```bash
docker compose down -v
```

---

## 14. Kết quả mong muốn

Sau khi hoàn thành:

* PostgreSQL hoạt động tại localhost:5432
* Redis hoạt động tại localhost:6379
* RabbitMQ hoạt động tại localhost:5672
* RabbitMQ Dashboard hoạt động tại localhost:15672

Mọi thành viên trong nhóm chỉ cần:

```bash
git pull
cd infra/docker
docker compose up -d
```

là có môi trường phát triển giống hệt nhau trên Windows hoặc macOS.

