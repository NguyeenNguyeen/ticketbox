# Đặc tả: Luồng Soát vé và Đồng bộ Ngoại tuyến (Ticket Check-in)

**Trạng thái cài đặt hiện tại:**
Chưa cài đặt (Not implemented).
*(Phân hệ ứng dụng di động dành cho nhân sự soát vé và luồng đồng bộ ngoại tuyến hiện đang nằm trong giai đoạn thiết kế nguyên lý kiến trúc, chưa có mã nguồn cài đặt thực tế trong codebase hiện hành của TicketBox).*

## 1. Mô tả

Phân hệ Soát vé (Ticket Check-in) được thiết kế để giải quyết bài toán kiểm soát ra vào tại các sự kiện quy mô lớn (như sân vận động, nhà thi đấu, trung tâm hội nghị). 

**Mục tiêu thiết kế cốt lõi:**
- Đảm bảo tốc độ quét mã QR e-ticket cực nhanh nhằm tránh hiện tượng ùn tắc, thắt cổ chai tại cổng an ninh.
- Hệ thống phải tiếp tục hoạt động trơn tru ngay cả khi mạng viễn thông tại địa điểm tổ chức bị nghẽn do quá tải sóng hoặc mất hoàn toàn (Nguyên lý Offline-first).
- Ngăn chặn triệt để hành vi sao chép vé (chụp ảnh màn hình mã QR) để nhiều người sử dụng chung một vé (Double-entry).
- Tự động hóa quá trình đồng bộ lịch sử soát vé về Trung tâm dữ liệu (Backend) để đối soát doanh thu.

## 2. Các tác nhân (Actors)

- **Tác nhân chính:** Nhân sự soát vé (CHECKER).
- **Thiết bị:** Thiết bị di động (Mobile Device / Scanner) được cài đặt ứng dụng soát vé nội bộ.
- **Tương tác Backend:** Phân hệ sẽ tương tác với các Endpoint API của Backend được bảo vệ bởi quyền `ROLE_CHECKER`.

## 3. Kiến trúc Đề xuất (Planned Architecture Concept)

Dựa trên yêu cầu chịu lỗi mạng nghiêm ngặt (Network Fault Tolerance) tại hiện trường, phân hệ được thiết kế hoàn toàn theo mô hình **Offline-first**.

![[../image/Planned Scan App Architecture.png]]

Thay vì mỗi lần quét QR thiết bị phải gọi một HTTP Request về Backend để kiểm tra (phương thức này sẽ gây đóng băng luồng người nếu mạng chậm), ứng dụng di động sẽ hoạt động theo 3 trụ cột nguyên lý sau:

1. **Đồng bộ chủ động (Pre-fetch):** Trước khi sự kiện mở cửa (khi thiết bị còn kết nối Wi-Fi ổn định), ứng dụng tải trước (download) toàn bộ danh sách mã vé hợp lệ của sự kiện đó về bộ nhớ cục bộ (Local Database - ví dụ SQLite hoặc Realm) trên thiết bị.
2. **Xác thực nội bộ (Local Validation):** Khi thực hiện quét QR, thuật toán tra cứu dữ liệu hoàn toàn diễn ra trên RAM hoặc ổ đĩa cục bộ của thiết bị di động. Độ trễ xác thực gần như bằng không (tính bằng mili-giây).
3. **Đồng bộ lùi (Background Sync):** Các lượt quét vé thành công được lưu lại thành các bản ghi nhật ký (Scan Records) trên máy. Một tiến trình chạy nền (Background Worker) sẽ liên tục thăm dò trạng thái kết nối Internet. Ngay khi có sóng, tiến trình này tự động đẩy (push) danh sách log lên Backend.

## 4. Luồng chính

### 4.1. Luồng Soát vé Ngoại tuyến (Offline QR Validation Flow)

![[../image/QR Validation Flow.png]]

1. **Quét mã:** Nhân sự soát vé mở ứng dụng, kích hoạt camera quét mã QR trên điện thoại của Khán giả.
2. **Tra cứu cục bộ:** Ứng dụng trích xuất chuỗi định danh từ QR, tiến hành truy vấn vào Database cục bộ.
3. **Đánh giá Trạng thái (Validation Concept):**
   - *Trường hợp 1:* Mã vé không tồn tại trong tập dữ liệu sự kiện đã tải -> Cảnh báo màn hình đỏ: **"Vé không hợp lệ"**.
   - *Trường hợp 2:* Mã vé hợp lệ nhưng cờ trạng thái cục bộ đã đánh dấu là đã dùng -> Cảnh báo màn hình vàng: **"Vé đã được sử dụng"** (Chống Double-entry).
   - *Trường hợp 3:* Mã vé hợp lệ và chưa sử dụng -> Hiển thị màn hình xanh: **"Cho phép qua cổng"**.
4. **Ghi nhận nội bộ:** Ngay lập tức, ứng dụng đảo trạng thái của vé này thành "Đã sử dụng" ngay trên Local Database. Đồng thời, sinh ra một bản ghi log `ScanRecord` (gồm ID vé, Timestamp, Device ID) và xếp vào hàng đợi chờ đồng bộ.

### 4.2. Luồng Đồng bộ Dữ liệu (Synchronization Concept)

1. **Phát hiện mạng:** Background Service trên Mobile App ghi nhận sự kiện hệ điều hành báo có kết nối Internet trở lại.
2. **Đẩy dữ liệu (Batching):** Ứng dụng gom các `ScanRecord` chưa đồng bộ thành một gói dữ liệu (Batch Payload) để tối ưu băng thông mạng, rồi gửi lệnh `POST /api/checker/sync` lên TicketBox Backend.
3. **Cập nhật Backend:** Backend tiếp nhận gói dữ liệu, giải nén và cập nhật trạng thái các vé (Ticket) tương ứng sang `USED` trong PostgreSQL để thống nhất dữ liệu hệ thống.
4. **Xác nhận (ACK):** Backend phản hồi mã `200 OK`. Ứng dụng di động nhận được phản hồi sẽ tiến hành xóa hoặc đánh dấu (flag) các log nội bộ đã được đồng bộ, tránh gửi trùng lặp ở vòng lặp sau.

[IMAGE PLACEHOLDER - Ticket Check-in Activity Diagram]

## 5. Quyết định Thiết kế và Bảo mật (Design Rationale & Security)

### 5.1. Bảo vệ tính toàn vẹn của mã QR (QR Verification Concept)
- *Vấn đề:* Kẻ gian có thể tự tạo ra hàng loạt mã QR chứa các chuỗi ngẫu nhiên (UUID) hy vọng qua mặt được hệ thống.
- *Giải pháp thiết kế:* Backend dự kiến sẽ mã hóa hoặc ký điện tử (Digital Signature - ví dụ sử dụng hệ mật mã bất đối xứng RSA Public/Private Key) nội dung vé trước khi vẽ ra ảnh QR. Ứng dụng Mobile chỉ chứa Public Key để xác minh tính nguyên bản (Authenticity) của chữ ký ngay khi offline, đảm bảo tuyệt đối không nhận nhầm vé tự chế.

### 5.2. Giải quyết Xung đột Dữ liệu Cục diện (Distributed Conflict Resolution)
- *Kịch bản:* Sự kiện có 10 cổng soát vé. Kẻ gian mua 1 vé hợp lệ, sao chép ảnh mã QR ra 2 bản, đưa cho 2 người xếp hàng ở 2 cổng khác nhau (Cổng A và Cổng B). Cả hai cổng đều đang mất mạng hoàn toàn. Lúc này, do dữ liệu cục bộ chưa thể trao đổi với nhau, cả 2 thiết bị đều báo "Hợp lệ" và cho 2 người vào sân.
- *Lý do đánh đổi (CAP Theorem Trade-off):* Trong môi trường thực tế, việc đảm bảo luồng người khổng lồ lưu thông nhanh chóng (Availability) được ưu tiên cao hơn việc phải xác minh trùng lặp chéo với máy chủ (Consistency) khi đứt mạng. TicketBox chấp nhận rủi ro cực nhỏ này. 
- *Hậu kiểm:* Khi có mạng trở lại, hệ thống đồng bộ sẽ phát hiện 1 mã vé có 2 timestamp quét từ 2 thiết bị khác nhau. Backend sẽ kích hoạt một Cảnh báo An ninh (Security Alert) ghi nhận vào hệ thống để Ban tổ chức giám sát và xử lý (ví dụ: mời bộ phận an ninh kiểm tra tại khán đài).

### 5.3. Tương tác với hệ thống hiện tại
- Kiến trúc phân hệ này được thiết kế để lắp ráp hoàn hảo vào hạ tầng Backend hiện hữu. Endpoint `/api/checker/**` đã được chuẩn bị sẵn trong `SecurityConfig`. Mọi luồng dữ liệu trao đổi (Pre-fetch và Sync) đều yêu cầu đính kèm JWT (JSON Web Token) trong Header, tuân thủ nghiêm ngặt cơ chế Stateless Auth của hệ thống.

## 6. Kịch bản lỗi

- **Khán giả đưa vé giả (Sai chữ ký):** Ứng dụng soát vé sẽ kiểm tra chữ ký điện tử trên QR bằng Public Key nội bộ. Nếu chữ ký không khớp, ứng dụng từ chối vé ngay cả khi mất mạng.
- **Vé bị dùng nhiều lần ở cùng một cổng:** Hệ thống cảnh báo "Vé đã được sử dụng" ngay trên Local Database.
- **Lỗi kết nối khi đang đồng bộ:** Tiến trình Background Sync sẽ dừng lại, không xóa các log `ScanRecord` trên máy, và sẽ tự động thử lại khi hệ điều hành báo có mạng trở lại.

## 7. Ràng buộc

- **Offline-First:** Ứng dụng phải có khả năng ra quyết định cho phép vào cổng chỉ dựa trên dữ liệu nội bộ (Local DB) trong vòng vài mili-giây.
- **Dung lượng mạng:** Quá trình tải trước (Pre-fetch) dữ liệu vé phải đủ gọn nhẹ để thực hiện xong trước giờ mở cửa.
- **Bảo vệ khóa bí mật (Private Key):** Khóa dùng để mã hóa mã QR chỉ nằm trên server (Backend), tuyệt đối không nhúng vào Mobile App để tránh bị dịch ngược (Reverse Engineering).

## 8. Tiêu chí chấp nhận

1. **Xác thực ngoại tuyến:** Thiết bị khi bị ngắt kết nối Wi-Fi/4G vẫn phải quét mã QR và xác định đúng vé hợp lệ hoặc không hợp lệ dựa trên Local DB.
2. **Đồng bộ tự động:** Khi thiết bị kết nối lại Internet, các log soát vé phải được tự động gửi về Backend và cập nhật thành công vào cơ sở dữ liệu chính.
3. **Phát hiện xung đột sau đồng bộ:** Nếu cùng một mã vé được quét ở 2 cổng offline khác nhau, sau khi đồng bộ, Backend phải ghi nhận sự cố quét trùng (Security Alert) để đối soát.
4. **Kiểm duyệt quyền:** API `/api/checker/sync` phải từ chối mọi yêu cầu nếu JWT không mang role `CHECKER`.
