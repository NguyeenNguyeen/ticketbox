# TỔNG KẾT TÌM HIỂU CÔNG NGHỆ, KĨ THUẬT

## 1. Phân hệ Backend Core & Security (Người 1)

### Công nghệ chính

* Ngôn ngữ & Framework: Java và Spring Boot.
* Cơ sở dữ liệu chính: PostgreSQL.
* Giải quyết bài toán cần tính ACID cao, transaction phức tạp và tính toàn vẹn dữ liệu.

### Kiểm soát tranh chấp vé (Concurrency)

* Kết hợp Redis làm bộ giảm chấn và Pessimistic Locking (`SELECT FOR UPDATE`) ở tầng Database để khóa dòng dữ liệu.
* Đảm bảo xếp hàng mua vé an toàn tuyệt đối.
* Chống bán lố (oversell).

### Giới hạn vé mỗi tài khoản

* Sử dụng Distributed Lock kết hợp với Redis.
* Khóa theo định dạng:

```text
lock:purchase:{user_id}:{category_id}
```

* Chặn các request song song từ cùng một người dùng.

### Chống trừ tiền hai lần (Idempotency)

* Sử dụng hàm `setIfAbsent()` (lệnh `SETNX`) của Redis để lưu `Idempotency-Key`.
* Nếu Key đã tồn tại:

  * Trả lại kết quả cũ.
  * Không gọi lại hàm tạo đơn hàng.
  * Không thực hiện trừ tiền lần nữa.

### Bảo mật & Phân quyền

* Sử dụng Spring Security.
* JWT (Stateless Authentication).
* Áp dụng RBAC.

#### Các Role

* CUSTOMER
* ORGANIZER
* CHECKER

#### Cơ chế bảo vệ

* Sử dụng Filter để bảo vệ API.

### Kiến trúc Code (Design Patterns)

#### State Pattern

Quản lý vòng đời đơn hàng:

* Pending
* Paying
* Completed

#### Strategy Pattern

Quản lý logic tính giá:

* Giá gốc
* Mã giảm giá
* Thuế

#### Abstract Factory

Xử lý luồng sinh các loại vé khác nhau.

### Caching

* Áp dụng mô hình Cache-aside với Redis cho dữ liệu concert.

#### Dữ liệu tĩnh

* TTL dài (30 phút).

#### Dữ liệu động

* Trạng thái vé.
* TTL cực ngắn hoặc tự động invalidate khi có giao dịch.

---

## 2. Phân hệ Web Frontend & UX (Người 2)

### Tech Stack chính

* Next.js 14 (App Router)
* Tailwind CSS
* Zustand (Global State)
* TanStack Query (Data Fetching)

### Render Sơ đồ ghế (SVG Map)

* Sử dụng Inline SVG.
* Áp dụng `React.memo`.
* Áp dụng Zustand selector cho từng ghế độc lập.

#### Mục tiêu

* Chỉ component ghế đó re-render khi đổi trạng thái.
* Tránh làm giật toàn bộ sơ đồ.

### Cập nhật dữ liệu Realtime

* Cập nhật trạng thái ghế bằng SSE (Server-Sent Events).
* Server push một chiều.
* Nhẹ hơn WebSocket.

Kết hợp:

* Polling 5–10 giây cho số vé tổng.

### Chống quá tải & Double-submit từ Client

#### Lớp UX

* Disable button ngay lập tức khi click.

#### Lớp kỹ thuật

* Sinh UUID.
* Truyền vào header làm Idempotency Key.

#### Với lỗi có thể retry (timeout)

* Tự động retry bằng Exponential Backoff.
* Hoặc hiển thị banner:

```text
Giao dịch đang xử lý
```

### Phân quyền UI

* Decode JWT trực tiếp ở client để ẩn/hiện nút.
* Kết hợp Next.js Middleware để bảo vệ route:

```text
/admin/*
```

---

## 3. Phân hệ Mobile & Offline Check-in (Người 3)

> Chưa xác định công nghệ và kỹ thuật chi tiết.

---

## 4. Phân hệ Infrastructure, Async & AI (Người 4)

### Mô hình Hạ tầng

Kiến trúc Hybrid:

#### Docker Local

Dùng cho các thành phần lõi:

* PostgreSQL
* Redis
* RabbitMQ

Mục tiêu:

* Benchmark chính xác.
* Mô phỏng môi trường triển khai.

#### Cloud APIs

Dùng cho các dịch vụ phụ trợ:

##### AI

* Gemini

##### Email

* Resend

Mục tiêu:

* Setup nhanh.
* Không phải tự vận hành hạ tầng.

### Kiểm soát tải đột biến (Rate Limiting)

* Cài đặt thuật toán Token Bucket.
* Sử dụng Bucket4j + Redis.

#### Chiến lược

##### API Public

* Giới hạn theo IP.

##### API Private

* Giới hạn theo User ID.

### Bảo vệ cổng thanh toán (Circuit Breaker)

* Sử dụng Resilience4j.

#### Cấu hình

* Count-based Sliding Window.

#### Bảo vệ luồng

* Kết hợp Bulkhead Pattern.
* Giới hạn số thread được phép gọi cổng thanh toán.

#### Xử lý khi ngắt mạch

Áp dụng Graceful Degradation.

Trả về mã lỗi nghiệp vụ để UI hiển thị:

```text
Thử lại sau
```

Đồng thời:

* Giữ nguyên trạng thái RESERVED của đơn hàng.

### Xử lý bất đồng bộ (Message Queue)

* Sử dụng RabbitMQ.

#### Luồng CSV Import

Kỹ thuật:

* Streaming đọc file.
* Fault Tolerant Processing.

Cơ chế:

* Catch exception tại dòng lỗi.
* Tiếp tục xử lý các dòng hợp lệ.
* Kiểm tra Idempotency chống import trùng.

#### Luồng AI & Email

* Sử dụng Async Worker.
* Điều tiết tốc độ gọi API.
* Tránh nghẽn thread của hệ thống.

### Cơ chế an toàn

#### Dead Letter Queue (DLQ)

* Hứng các message lỗi.

#### Retry Mechanism

* Tự động retry các message thất bại.

#### Mục tiêu

* Không làm tắc queue chính.
* Tăng độ tin cậy cho hệ thống.

