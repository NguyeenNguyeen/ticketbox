# TICKETBOX

## Cấu trúc thư mục
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

## Git Workflow & Merge Policy

### Branch Structure

```text
## Branch Structure
├── main
│
├── develop
│
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

## 🚀 Khởi Động Dự Án (Startup Instructions)

Dự án hiện tại không sử dụng thư viện tự động load `.env` (như `spring-dotenv`), thay vào đó, các biến môi trường sẽ được nạp trực tiếp từ hệ điều hành. Điều này đảm bảo tính nhất quán trên các môi trường CI/CD, Docker, và Server thực tế.

**Lưu ý:** Bạn chỉ cần cấu hình MỘT file `.env` duy nhất ở thư mục gốc của dự án.

### Đối với Linux / macOS

Mở terminal ở thư mục gốc dự án, nạp biến môi trường và chạy Spring Boot:

```bash
set -a
source .env
set +a
cd apps/backend
mvn spring-boot:run
```

*(Hoặc nếu chạy từ thư mục gốc, sử dụng `mvn -pl apps/backend spring-boot:run`)*

### Đối với Windows PowerShell

Mở PowerShell ở thư mục gốc dự án, đọc file `.env` và set biến môi trường cho tiến trình hiện tại:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}
cd apps/backend
mvn spring-boot:run
```
