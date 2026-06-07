# Đặc tả Kiến trúc Web App

## 1. Phân chia Container
- **Public Web App**: Phục vụ khán giả mua vé. Tối ưu SEO, SSR (Next.js App Router).
- **Web Admin**: Dành riêng cho Ban tổ chức. Tối ưu SPA (Client-side Rendering) để xử lý Form và báo cáo phức tạp.

## 2. Giao tiếp với Backend API
- Sử dụng TanStack Query để Data Fetching, Caching và Invalidation.
- Kết nối Realtime qua Server-Sent Events (SSE) để nhận trạng thái ghế từ RabbitMQ/Backend thay vì WebSocket để giảm tải.
- Axios/Fetch wrapper với cấu hình Authorization Header (Bearer Token) và Idempotency-Key.

## 3. Quản lý Trạng thái (Global State)
- Sử dụng Zustand thay vì Redux để cấu hình nhẹ gọn.
- Trạng thái Giỏ hàng (Cart) và Ghế ngồi (Seat) được tách biệt. Sử dụng `React.memo` và các selector độc lập để tránh re-render giật lag Sơ đồ ghế SVG.
