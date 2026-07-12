# TICKETBOX

## Cấu trúc thư mục
```text
ticketbox/
├── README.md
├── .env
├── .env.example
├── .gitignore
├── EnvChecker.java
├── mock_vip_guests.csv
├── plan_frontend.md
├── agent/
├── apps/
│   ├── backend/
│   ├── frontend/
│   └── mobileapp/
├── blueprint/
│   ├── design.md
│   ├── proposal.md
│   ├── image/
│   └── specs/
├── data/
├── infra/
│   └── docker/
│       ├── docker-compose.yml
│       └── init.sql
├── scripts/
├── src/
├── Ticketbox/
└── uploads/
```

## Git Workflow & Merge Policy

### Branch Structure

```text
main
├── develop
├── backend
├── frontend
├── mobileapp
└── infra

(Optional)
├── b_<feature-name>
├── f_<feature-name>
├── m_<feature-name>
└── i_<feature-name>
```

* `main`: Chứa phiên bản ổn định của dự án, sẵn sàng triển khai.
* `develop`: Nhánh tích hợp dùng để kiểm thử và đánh giá các chức năng trước khi đưa vào `main`.
* `backend, frontend, ...`: module làm việc của mỗi người, làm việc phần được giao
* `b_*, f_*, ...`: *(Không bắt buộc phải chia)* Nhánh phát triển riêng cho từng chức năng hoặc nhiệm vụ nếu cần, nó không cần thiết vì mỗi người 1 module (Đọc ở *Development Rules*).

### Development Workflow

1. Luôn pull từ `develop` khi có thay đổi trước khi bắt đầu công việc.Không pull từ main vào các nhánh module. Các nhánh module phải thường xuyên đồng bộ với develop để nhận các thay đổi mới nhất từ các thành viên khác.

```bash
git checkout branch_name
git pull origin develop
```

2. Thực hiện công việc trên nhánh cá nhân.

3. Commit thường xuyên với nội dung rõ ràng và tuân theo quy ước đặt tên commit của dự án, tránh làm rất nhiều thứ rồi mới commit 1 lần.
Sử dụng các tiền tố sau:

```text
feat: thêm chức năng mới
fix: sửa lỗi
docs: cập nhật tài liệu
refactor: cải tiến mã nguồn
style: thay đổi định dạng mã nguồn
test: bổ sung hoặc chỉnh sửa kiểm thử
chore: thay đổi cấu hình, Docker, CI/CD,...
```

4. Sau khi hoàn thành, cập nhật nhánh với phiên bản mới nhất của `develop` và xử lý mọi xung đột nếu có.

5. Tạo Pull Request (PR) từ nhánh `branch_name (module của bạn)` vào `develop`.

### Pull request Requirements

Trước khi Pull request vào `develop`, thành viên phải đảm bảo:

* Code biên dịch và chạy thành công.
* Không còn lỗi cú pháp hoặc lỗi runtime đã biết.
* Chức năng đã được kiểm thử cơ bản.
* Không làm ảnh hưởng đến các chức năng hiện có.
* Đã xử lý toàn bộ conflict với phiên bản mới nhất của `develop`.
* Đã cập nhật tài liệu hoặc cấu hình liên quan (nếu có).

### Main Branch Protection

* Không được commit hoặc push trực tiếp lên `main`.
* Mọi thay đổi trên `main` phải thông qua Pull Request từ `develop`.
* Chỉ trưởng nhóm hoặc người được phân quyền mới được merge vào `main`.
* Chỉ merge vào `main` khi toàn bộ chức năng đã được kiểm thử và xác nhận hoạt động ổn định.

### Development Rules

- Mỗi thành viên chịu trách nhiệm phát triển trên nhánh module của mình.
- Trong trường hợp cần thực hiện một tính năng lớn hoặc thử nghiệm riêng, thành viên có thể tạo thêm nhánh chức năng từ nhánh module tương ứng.
- Sau khi hoàn thành, nhánh chức năng sẽ được merge trở lại nhánh module (backend, frontend, ...) trước khi merge vào `develop`.
- Việc sử dụng nhánh chức năng là không bắt buộc và phụ thuộc vào độ phức tạp của công việc, tuỳ ý, hoặc khi có nhiều người tham gia hỗ trợ.

## 📧 Hướng Dẫn Test Tính Năng Email (E-ticket)

Hệ thống sử dụng **RabbitMQ** và **Resend** để gửi email E-ticket khi khách hàng mua vé thành công. Khi chạy ở môi trường phát triển (Local/Demo), hệ thống sử dụng **Resend Sandbox Mode**.

### ⚠️ Lưu Ý Quan Trọng (Resend Sandbox)
1. **Email Gửi Đi (Sender):** Mặc định hệ thống dùng `onboarding@resend.dev` (cấu hình trong `.env.example`).
2. **Email Nhận (Recipient):** Resend Sandbox **CHỈ CHO PHÉP** gửi email đến địa chỉ email mà bạn đã dùng để đăng ký tài khoản Resend. Bất kỳ email nào khác sẽ bị từ chối (Lỗi 403 Forbidden).

### ❌ KHÔNG Dùng Tài Khoản Seed (customer1) Để Test Email
Tài khoản mặc định `customer1` trong database có email là `customer1@gmail.com`. Vì email này không thuộc về tài khoản Resend của bạn, **email E-ticket sẽ bị lỗi và không gửi được**. Mặc dù tài khoản này có thể dùng để test các luồng khác như giao diện, thanh toán, nhưng KHÔNG dùng để test nhận email.

### ✅ Quy Trình Test Email Đề Xuất
Để đảm bảo tính năng gửi E-ticket hoạt động trơn tru trên máy của bạn:
1. Đảm bảo bạn đã nhập `RESEND_API_KEY` của bạn vào file `.env`.
2. Khởi động hệ thống Backend và Frontend.
3. Vào trang Đăng Ký (Register) trên ứng dụng, **tự tạo một tài khoản mới** bằng chính địa chỉ email mà bạn đã đăng ký Resend.
4. Đăng nhập bằng tài khoản mới vừa tạo.
6. Kiểm tra hộp thư (Inbox) của email đó để xem E-ticket PDF đính kèm.

## 🚀 Hướng dẫn Build & Run

Dưới đây là hướng dẫn đầy đủ để chạy dự án Ticketbox trên máy local theo đúng trình tự. Nếu bạn là người mới làm quen với dự án, hãy đọc từ đầu đến cuối và làm theo từng bước.

### 1) Yêu cầu trước khi bắt đầu
Trước khi chạy ứng dụng, hãy đảm bảo máy của bạn đã cài đặt các công cụ sau:
- Java 17 hoặc mới hơn
- Maven 3.8+
- Node.js 20+
- npm (đi kèm với Node.js)
- Docker Desktop hoặc Docker Engine và Docker Compose

Nếu bạn chưa cài đặt, hãy cài trước rồi mới tiếp tục. Đây là những công cụ cần thiết để chạy backend, frontend và các dịch vụ phụ trợ như PostgreSQL, Redis và RabbitMQ.

### 2) Clone dự án và vào thư mục gốc
```bash
git clone <repository-url>
cd ticketbox
```

Nếu repo đã có sẵn trên máy, chỉ cần vào thư mục gốc:
```bash
cd ticketbox
```

### 3) Cấu hình file biến môi trường
Tạo file `.env` từ mẫu có sẵn [.env.example](.env.example):

```bash
cp .env.example .env
```

Sau đó mở file `.env` và chỉnh lại các giá trị cần thiết. Các biến quan trọng thường là:
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_DB`
- `GEMINI_API_KEY`
- `RESEND_API_KEY`

Nếu bạn chưa có API key thật, có thể để trống cho các phần chưa dùng đến, nhưng nếu cần test các tính năng AI hoặc email thì nên điền đúng giá trị.

> Lưu ý: Hãy chỉ cấu hình một file `.env` ở thư mục gốc. Dự án sẽ đọc file này khi khởi động backend.

### 4) Khởi động các dịch vụ phụ trợ bằng Docker
Dự án cần PostgreSQL, Redis và RabbitMQ để vận hành đầy đủ. Bạn có thể chạy tất cả bằng Docker Compose:

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

Lệnh này sẽ khởi động các container sau:
- PostgreSQL trên cổng 5432
- Redis trên cổng 6379
- RabbitMQ trên cổng 5672
- RabbitMQ Management UI trên cổng 15672

Bạn có thể kiểm tra trạng thái container bằng lệnh:

```bash
docker ps
```

Nếu các container chưa chạy, hãy kiểm tra log:

```bash
docker compose -f infra/docker/docker-compose.yml logs -f
```

### 5) Chạy Backend (Spring Boot)
Backend nằm trong thư mục [apps/backend](apps/backend). Hãy mở terminal ở thư mục gốc của repo trước khi chạy các lệnh dưới đây, vì file `.env` nằm ở thư mục gốc.

#### 5.1 Linux / macOS
```bash
cd /path/to/ticketbox
set -a
source .env
set +a
cd apps/backend
mvn clean install
mvn spring-boot:run
```

#### 5.2 Windows PowerShell
```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
Set-Location apps/backend
mvn clean install
mvn spring-boot:run
```

Sau khi backend khởi động thành công, ứng dụng sẽ chạy tại:
- http://localhost:8080

Bạn có thể kiểm tra bằng cách mở URL này trong trình duyệt hoặc xem log của terminal. Nếu bạn muốn kiểm tra nhanh bằng lệnh, có thể thử:
```bash
curl -i http://localhost:8080/actuator/health
```

Lưu ý: vì backend có cấu hình Spring Security, một số endpoint có thể trả về `403 Forbidden` nếu chưa được xác thực. Nếu log terminal hiển thị `Tomcat started on port 8080` và `Started Application`, thì backend đã khởi động đúng.

### 6) Chạy Frontend (Next.js)
Frontend nằm trong thư mục [apps/frontend](apps/frontend).

Mở một terminal mới, chạy các lệnh sau:

```bash
cd apps/frontend
npm install
npm run dev
```

Sau khi khởi động xong, frontend sẽ chạy tại:
- http://localhost:3000

Nếu port 3000 đang bị chiếm, Next.js có thể tự đổi sang port khác. Hãy kiểm tra terminal output để xác nhận URL chính xác.

### 7) Build production (không chạy dev server)
#### 7.1 Build Backend
```bash
cd apps/backend
mvn clean package
```

Lệnh này sẽ build file JAR có thể chạy ở môi trường production.

#### 7.2 Build Frontend
```bash
cd apps/frontend
npm run build
```

Sau khi build xong, bạn có thể chạy frontend ở chế độ production bằng:

```bash
npm run start
```

### 8) Kiểm tra sau khi chạy
Sau khi cả backend và frontend đã chạy, hãy kiểm tra các điểm sau:
- Backend có phản hồi trên http://localhost:8080 hay không
- Frontend có mở trên http://localhost:3000 hay không
- Docker container PostgreSQL, Redis và RabbitMQ đang ở trạng thái `Up`
- Nếu cần test email, hãy đảm bảo `RESEND_API_KEY` đã được cấu hình đúng và dùng email đã xác thực trong Resend sandbox

### 9) Các lỗi thường gặp
#### Lỗi không kết nối được database
- Kiểm tra Docker container đã chạy chưa bằng `docker ps`
- Kiểm tra file `.env` có đúng `POSTGRES_*` không
- Kiểm tra log của container bằng `docker compose -f infra/docker/docker-compose.yml logs postgres`

#### Lỗi `npm install` thất bại
- Kiểm tra Node.js đã cài đúng version chưa
- Xóa thư mục `node_modules` rồi chạy lại `npm install`

#### Backend không khởi động được
- Kiểm tra Java 17 đã được cài và `mvn -version` hoạt động bình thường
- Kiểm tra file `.env` có tồn tại ở thư mục gốc và có đúng định dạng
- Xem log lỗi của Maven để biết nguyên nhân cụ thể

#### Frontend không mở được trên port 3000
- Port 3000 có thể đã được service khác sử dụng
- Xem output của terminal để biết port mới được Next.js chọn

### 10) Gợi ý chạy nhanh cho developer
Nếu bạn chỉ muốn chạy nhanh để test UI và API, có thể làm theo thứ tự:
1. Chạy Docker services
2. Chạy backend
3. Chạy frontend
4. Truy cập http://localhost:3000 để bắt đầu sử dụng

Nếu bạn muốn, tôi có thể tiếp tục viết thêm một phần “Troubleshooting nâng cao” hoặc “Cách chạy bằng Docker Compose toàn bộ stack” cho README này. 
