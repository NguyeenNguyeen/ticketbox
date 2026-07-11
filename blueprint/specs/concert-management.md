# Đặc tả: Quản trị Sự kiện (Concert Management)

## 1. Mô tả

Phân hệ Quản trị Sự kiện cung cấp toàn bộ bộ công cụ nghiệp vụ (Admin Dashboard) để Ban tổ chức có thể thiết lập, theo dõi và cấu hình vòng đời của một Concert. 

**Mục tiêu thiết kế:**
- Cung cấp giao diện trực quan và bộ API tin cậy để quản lý siêu dữ liệu (Metadata) sự kiện.
- Giảm thiểu tối đa sai sót do cấu hình thủ công của con người (ví dụ: quên đóng/mở bán vé).
- Tích hợp liền mạch với các tính năng tự động hóa bên ngoài (AI Artist Bio, CSV VIP Import).
- Quản lý an toàn cấu trúc hạng vé, bảo vệ các thiết lập chống đầu cơ (Max tickets per user).

## 2. Các tác nhân và Điều kiện tiên quyết

- **Tác nhân chính:** Ban tổ chức (ORGANIZER).
- **Điều kiện tiên quyết:** 
  - Người dùng phải được xác thực hợp lệ bằng JWT.
  - Phải mang quyền (Role) là `ORGANIZER`. Mọi nỗ lực truy cập từ tài khoản Khán giả (CUSTOMER) hoặc Soát vé (CHECKER) đều bị từ chối tuyệt đối ở cả cấp độ giao diện lẫn cấp độ Database.

## 3. Luồng chính

Luồng thiết lập một sự kiện chuẩn diễn ra theo trình tự kiến trúc sau:

1. **Khởi tạo thông tin cơ sở:** Ban tổ chức điền các thông tin nền tảng: Tên sự kiện, mô tả, địa điểm, thời gian diễn ra (`startTime`, `endTime`), và thời gian mở cửa đón khách (`doorsTime`).
2. **Cấu hình thời gian mở bán:** Thiết lập `saleStartTime`. Đây là mốc cấu hình quan trọng quyết định việc tự động chuyển đổi trạng thái của sự kiện sang mở bán.
3. **Cấu hình hạng vé (Ticket Categories):** Tạo lập các hạng ghế (Ví dụ: SVIP, VIP, GA). Mỗi hạng vé lưu vào bảng `TicketCategory` với các tham số:
   - Giá vé (`price`).
   - Tổng số lượng phát hành (`totalQuantity`).
   - Giới hạn mua tối đa mỗi người (`maxPerUser`).
4. **Tích hợp AI tạo tiểu sử (Tùy chọn):**
   - Ban tổ chức tải lên file PDF thông cáo báo chí của nghệ sĩ.
   - Luồng chạy nền RabbitMQ tiếp nhận file.
   - AI Worker (giao tiếp với Gemini/OpenAI API) phân tích văn bản PDF, tóm tắt tiểu sử và cập nhật ngầm vào bảng `Artist`.
5. **Phát hành (Publish):** Dữ liệu được lưu trữ. Dựa trên các thông số cấu hình, hệ thống sẽ tự động tính toán trạng thái hiển thị của sự kiện đối với người mua.
6. **Nhập danh sách VIP (VIP Guest Import):** Dựa trên ID của sự kiện vừa tạo, Ban tổ chức có thể nhập file CSV chứa danh sách khách VIP ở các bước tiếp theo (xem đặc tả VIP Guest Import).

## 4. Thiết kế Vòng đời Sự kiện (Concert Lifecycle)

Hệ thống TicketBox KHÔNG lưu trạng thái (Status) dưới dạng văn bản tĩnh cố định trong cơ sở dữ liệu (ví dụ: không có cột `status` chứa chuỗi "UPCOMING" rồi phải gọi lệnh update). Thay vào đó, Trạng thái Thực tế (Effective Status) được **tính toán động (Computed)** dựa trên việc so sánh thời gian thực của máy chủ (Current Time) với các mốc thời gian cấu hình, kết hợp với các cờ ghi đè thủ công.

- **UPCOMING (Sắp diễn ra):** Thời gian thực nhỏ hơn `saleStartTime`. Khán giả được phép vào xem trang thông tin sự kiện, xem sơ đồ chỗ ngồi, nhưng nút Mua vé bị ẩn/vô hiệu hóa.
- **ON_SALE (Đang mở bán):** Thời gian thực lớn hơn hoặc bằng `saleStartTime` và nhỏ hơn `endTime`. Nút Mua vé được bật, API cho phép tạo đơn hàng.
- **ENDED (Đã kết thúc):** Thời gian thực đã vượt qua `endTime`. Sự kiện kết thúc, nút Mua vé bị khóa vĩnh viễn, API chối bỏ giao dịch mới.
- **CANCELLED (Đã hủy):** Trạng thái ghi đè (Manual Override). Bất chấp mọi mốc thời gian, nếu cờ `cancelledStatus` hoặc `forcedStatus` được kích hoạt bởi Ban tổ chức, sự kiện lập tức chuyển trạng thái hủy, khóa mọi giao dịch thanh toán ngay tức thì.

**Lý do thiết kế (Design Rationale):** 
Thiết kế tính toán trạng thái động giúp hệ thống không cần phải viết thêm một "Background Cron Job" chạy mỗi giây quét hàng triệu bản ghi trong Database để cập nhật cột status từ `UPCOMING` sang `ON_SALE`. Điều này loại bỏ hoàn toàn tình trạng Deadlock CSDL và hiện tượng lệch pha dữ liệu (Ví dụ: Đã đến giờ nhưng Job chạy chậm khiến người dùng chưa thể mua vé).

## 5. Tích hợp và Tương tác Component

Quản trị sự kiện là điểm khởi nguồn luồng dữ liệu (Data Source) định hình các logic cốt lõi của toàn hệ thống TicketBox:

- **Tương tác với Ticket Purchase:** Cung cấp thông số cấu hình tối quan trọng là `maxPerUser` (chống đầu cơ) và `availableQuantity` (tồn kho). Luồng Mua vé hoàn toàn phụ thuộc vào việc tính toán Effective Status từ module này để cho phép hay từ chối giao dịch.
- **Tương tác với AI Worker:** Chuyển tải luồng trích xuất dữ liệu chậm (PDF Parsing) sang RabbitMQ. Component quản trị không bị block khi chờ AI phản hồi.
- **Tương tác với Hệ thống Cache (Redis Cache Invalidation):** 
  - Giao diện của khán giả là giao diện có lượng truy cập (Read) cực lớn nên được Cache trên Redis (TTL dài).
  - Bất cứ khi nào Ban tổ chức ấn nút **Lưu/Cập nhật** sự kiện (Ví dụ: Sửa tên, thay đổi giờ), Component Quản trị có trách nhiệm gọi lệnh **Cache Evict** để ép hệ thống xóa bỏ Cache cũ. Lần truy cập tiếp theo của khán giả sẽ tự động nạp dữ liệu mới nhất, đảm bảo tính nhất quán (Consistency).

## 6. Kịch bản lỗi

- **Dữ liệu đầu vào sai (Validation Exception):** Nếu nhập sai trình tự thời gian (VD: Ngày đóng cửa trước ngày mở cửa), Backend Controller sẽ bắt lỗi Validation ngay lập tức, trả về `400 Bad Request` và không tạo kết nối tới Database.
- **Ngoại lệ Xử lý AI (Graceful Degradation):** Nếu API Gemini bị Time-out hoặc file PDF quá phức tạp, quá trình thiết lập sự kiện vẫn **Thành công**. Tác vụ AI thất bại sẽ rơi vào RabbitMQ Dead Letter Queue (DLQ). Ban tổ chức không bị chặn luồng làm việc, họ có thể nhập tiểu sử bằng tay hoặc yêu cầu thử lại tiến trình xử lý AI (Retry) ở phần quản trị riêng biệt.

## 7. Ràng buộc

- **Logic thời gian cơ bản:** `saleStartTime` bắt buộc phải diễn ra trước `startTime` (Giờ bắt đầu bán phải trước giờ diễn ra concert). `startTime` phải trước `endTime`.
- **Ràng buộc an toàn tồn kho (Inventory Safety):** Khi sự kiện đã ở trạng thái `ON_SALE` và đã có giao dịch mua, Ban tổ chức không được phép sửa giảm `totalQuantity` của hạng vé xuống mức thấp hơn số vé đã phát hành thực tế.
- **Cách ly dữ liệu vé ảo:** Luồng quản trị tự động giấu đi (Hide) loại vé `GUEST` khỏi giao diện báo cáo doanh thu công khai, vì đây là loại vé sinh ra từ luồng VIP Import có giá trị bằng 0 VND.

## 8. Tiêu chí chấp nhận

1. **Bảo mật truy cập:** API tạo/sửa sự kiện trả về `403 Forbidden` nếu người gọi không có quyền `ORGANIZER`.
2. **Logic thời gian:** Hệ thống từ chối lưu sự kiện nếu `saleStartTime` lớn hơn `startTime`, hoặc `startTime` lớn hơn `endTime` (trả về 400 Bad Request).
3. **Cập nhật Cache:** Ngay sau khi lưu thông tin sự kiện thành công, Redis Cache tương ứng lập tức bị xóa (Evicted) để khán giả nhận được thông tin mới nhất.
4. **Tích hợp AI dự phòng:** Khi upload file PDF để sinh tiểu sử, nếu API AI (Gemini) bị lỗi hoặc quá tải, sự kiện vẫn được tạo thành công, chỉ có tác vụ sinh tiểu sử được chuyển vào Dead Letter Queue.
5. **Trạng thái động:** Endpoint lấy thông tin sự kiện phải tự động trả về đúng trạng thái (UPCOMING, ON_SALE, ENDED) dựa trên thời gian máy chủ hiện tại so với các mốc thời gian sự kiện.
