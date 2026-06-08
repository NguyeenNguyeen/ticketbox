# Đặc tả luồng Thanh toán & Chống quá tải

## 1. Luồng tương tác tại màn hình Thanh toán
1. Khán giả chọn ghế trên Sơ đồ (tối đa N ghế tuỳ giới hạn hạng vé).
2. Hệ thống đếm ngược 10 phút giữ ghế ảo (Locking).
3. Khán giả bấm "Thanh toán", nút lập tức bị Disable (vô hiệu hoá) và chuyển thành trạng thái "Đang xử lý...".

## 2. Cơ chế Idempotency-Key
- Client tự động sinh UUID v4 khi người dùng bắt đầu vào phiên thanh toán.
- Bắt buộc gán mã này vào HTTP Header `Idempotency-Key` của request `POST /api/tickets/purchase`.
- Backend nhận diện Header này, lưu vào Redis để chặn trùng lặp nếu khán giả lỡ F5 hoặc mạng chập chờn gửi 2 request giống nhau.

## 3. Xử lý Lỗi & Cảnh báo (Graceful Degradation)
- **Hết vé (400 Bad Request)**: Báo "Rất tiếc, vé này vừa được mua mất ở mili-giây cuối".
- **Sập mạng/Offline**: Lắng nghe sự kiện `offline` của Browser, báo lỗi thân thiện "Vui lòng kiểm tra lại đường truyền Internet".
- **Timeout Cổng Thanh toán (5xx / Gateway Error)**: Client dùng Exponential Backoff tự động gọi lại tối đa 3 lần. Nếu vẫn tạch, hiện Banner "Hệ thống đang bảo trì. Vẫn tiếp tục xem sự kiện bình thường...".
