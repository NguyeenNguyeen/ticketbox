# Đặc tả Phân quyền UI (Access Control)

## 1. Kịch bản kiểm tra quyền
- **Lớp Middleware (Next.js)**: Bảo vệ hoàn toàn tất cả các route bắt đầu bằng `/admin/*`. Đọc cookie `token`, decode JWT payload và chặn truy cập nếu role không phải là `ORGANIZER`.
- **Lớp Client (Zustand/React)**: Decode Base64 JWT Payload trực tiếp từ LocalStorage thay vì phải gọi API `/auth/me` để tối ưu thời gian tải trang.

## 2. Ẩn/hiện tính năng theo Role
- Khán giả (`CUSTOMER`): Không thấy menu "Trang quản trị" trên Header.
- Ban tổ chức (`ORGANIZER`): Thấy nút "Trang quản trị". Có quyền vào Dashboard xem doanh thu, cấu hình sự kiện.
- Kiểm duyệt viên (`CHECKER`): Chỉ thấy giao diện quét mã QR/soát vé (sẽ phát triển sau).
