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
