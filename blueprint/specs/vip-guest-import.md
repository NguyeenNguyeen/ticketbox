# Đặc tả: Luồng Nhập danh sách Khách mời VIP (VIP Guest Import)

## 1. Mô tả

Phân hệ VIP Guest Import chịu trách nhiệm tự động hóa quy trình phát hành vé đặc biệt cho khách mời, đối tác, hoặc nhà tài trợ (Sponsors) của sự kiện. Thay vì Ban tổ chức phải tạo từng vé thủ công, hệ thống cung cấp tính năng nhập danh sách hàng loạt thông qua tệp CSV.

**Mục tiêu thiết kế:**
- Xử lý mượt mà và an toàn danh sách hàng nghìn khách mời mà không làm treo hay tràn bộ nhớ (OOM) hệ thống.
- Nhận diện và xử lý các bản ghi trùng lặp (Duplicate Detection) một cách thông minh để tuyệt đối không phát hành dư vé cho cùng một người.
- Đảm bảo tính toàn vẹn dữ liệu (Fault Tolerance): hệ thống tự động loại bỏ các dòng dữ liệu hỏng nhưng vẫn tiếp tục hoàn thành quá trình import cho các dòng hợp lệ.

## 2. Động lực Nghiệp vụ (Business Motivation)

Trong thực tế tổ chức sự kiện, nhà tài trợ thường cung cấp danh sách khách mời dưới dạng file Excel/CSV sát ngày diễn ra concert. Danh sách này thường xuyên thay đổi, bổ sung và có chất lượng dữ liệu không đồng đều (thiếu thông tin, gõ sai email). 

Ban tổ chức cần một công cụ import có khả năng:
- Bỏ qua các dữ liệu lỗi.
- Cập nhật thông tin nếu có thay đổi mà không tạo ra vé mới.
- Tự động sinh mã QR và gửi e-ticket trực tiếp đến email của khách VIP mà không bắt buộc họ phải đăng ký tài khoản và thao tác mua vé trên website.

## 3. Các tác nhân và Điều kiện tiên quyết

- **Tác nhân chính:** Ban tổ chức (ORGANIZER).
- **Tác nhân phụ:** Dịch vụ gửi Email (RabbitMQ Worker).
- **Điều kiện tiên quyết:** 
  - Người dùng có quyền truy cập Admin Dashboard.
  - File CSV được định dạng theo chuẩn (bắt buộc có cột Tên và Email).
  - Sự kiện (Concert) phải tồn tại trên hệ thống.

## 4. Luồng chính

![[../image/Guest Import Sequence Diagram.png]]

Quá trình import được thực thi tuần tự theo các bước kiến trúc sau:

1. **Tải lên CSV (Admin UI):** Ban tổ chức chọn sự kiện mục tiêu và tải lên file CSV danh sách khách mời tại trang Quản trị.
2. **Tiếp nhận & Phân tích (Streaming Parser):** 
   - Hệ thống tự động nhận diện ký tự phân cách (Dấu phẩy `,` hoặc Chấm phẩy `;`).
   - Sử dụng cơ chế Streaming (`BufferedReader` kết hợp `Apache Commons CSV`) để duyệt qua từng dòng dữ liệu thay vì nạp toàn bộ file vào bộ nhớ RAM.
3. **Kiểm tra tính hợp lệ (Validation):** 
   - Lớp `CsvRowValidator` đánh giá từng dòng. Một bản ghi hợp lệ bắt buộc phải có `FullName` và địa chỉ `Email` tuân thủ đúng định dạng Regex (`^[A-Za-z0-9+_.-]+@(.+)$`).
4. **Xử lý trùng lặp (Duplicate Detection):**
   - Định danh duy nhất (Identification Strategy) của một khách mời trong một sự kiện được xác định bằng cặp khóa: `(concert_id, email)`.
   - Nếu `email` đã tồn tại trong sự kiện, hệ thống áp dụng cơ chế **Cập nhật (Upsert)**: ghi đè các thông tin mới (số điện thoại, tên nhà tài trợ) vào bản ghi cũ thay vì tạo một khách mời mới.
5. **Khởi tạo Hạng vé Đặc biệt (GUEST Ticket Generation):**
   - Hệ thống tự động tra cứu, nếu chưa có sẽ tự động khởi tạo một hạng vé ẩn mang tên `GUEST` (giá 0 VND, số lượng vô hạn, max_per_user = 1) dành riêng cho sự kiện.
   - Nếu khách mời chưa được cấp vé, hệ thống phát hành một bản ghi `Ticket` có thuộc tính `ticketType="GUEST"`, đính kèm một mã `qr_code` (UUID) định danh duy nhất.
6. **Bất đồng bộ Gửi Email (Data Flow to RabbitMQ):**
   - Ngay sau khi lưu vé thành công, hệ thống đóng gói lệnh thành một `EmailTaskMessage` và đẩy lên Message Broker (Exchange: `ticketbox.commands`, Routing Key: `email`).
7. **Phát hành E-Ticket (Async Email Worker):**
   - Worker chạy ngầm tiêu thụ message, render file PDF E-ticket sang trọng và tự động gọi API Email (Resend/Brevo/SMTP tùy cấu hình) để gửi email đến tay khách mời VIP.

## 5. Quyết định Thiết kế Kiến trúc (Design Rationale)

### 5.1. Chiến lược Định danh bằng Email (Guest Identification Strategy)
- *Lý do:* Trong bối cảnh sự kiện, khách VIP hiếm khi cung cấp số CMND/CCCD hoặc có sẵn tài khoản trên hệ thống từ trước. Địa chỉ Email là phương tiện định danh độc bản duy nhất và cũng là phương tiện để giao vé. Việc nhóm `(concert_id, email)` thành cụm khóa nhận diện cho phép một cá nhân VIP tham dự nhiều Concert khác nhau dưới tư cách khách mời, mà vẫn đảm bảo tính duy nhất trong khuôn khổ một sự kiện.

### 5.2. Áp dụng cơ chế Upsert thay vì Báo lỗi Trùng lặp
- *Lý do:* Trong quy trình vận hành sự kiện, việc danh sách khách mời thay đổi thông tin (ví dụ: bổ sung thêm số điện thoại) là rất phổ biến. Thiết kế hệ thống chọn cách Upsert (Cập nhật nếu đã tồn tại) thay vì ném lỗi `DuplicateKeyException` và dừng quá trình. Điều này mang lại trải nghiệm phần mềm (UX) trơn tru cho Ban tổ chức, họ chỉ cần upload lại file mới nhất mà không cần tự ngồi lọc ra ai cũ ai mới.

### 5.3. Tái sử dụng Hạ tầng Email Bất đồng bộ (Component Reusability)
- *Lý do:* Luồng Import CSV không tự viết lại các hàm tạo PDF hay cấu hình kết nối Provider Email. Thay vào đó, nó tái sử dụng 100% kiến trúc RabbitMQ và Provider Abstraction của phân hệ *Ticket Purchase*. Bằng cách tuân thủ đúng định dạng hợp đồng dữ liệu `EmailTaskMessage`, luồng CSV tự động thừa hưởng toàn bộ cơ chế bảo vệ của RabbitMQ: Tự động Retry khi lỗi mạng, Exponential Backoff, và gom thư rác vào Dead Letter Queue (DLQ). Kiến trúc này giúp mã nguồn Backend đạt độ kết dính cao (High Cohesion) và giảm trùng lặp.

### 5.4. Ẩn Hạng vé GUEST (Data Segregation)
- *Lý do:* Hạng vé `GUEST` là một hạng vé ảo, không mang giá trị thanh toán, được thiết kế chuyên biệt để phân tách rạch ròi với dòng vé thương mại (VIP, GA). Thuộc tính `ticketType="GUEST"` giúp các câu truy vấn (Query) của Admin Dashboard dễ dàng bóc tách dữ liệu: không cộng gộp số lượng vé GUEST vào Tổng doanh thu, và không hiển thị hạng vé này lên giao diện mua vé công khai của Khán giả thông thường.


## 6. Kịch bản lỗi

Kiến trúc luồng xử lý CSV được xây dựng dựa trên nguyên lý **Chịu lỗi cục bộ (Fault-Tolerant)**.

- **Kịch bản: Dòng dữ liệu bị lỗi (Invalid Rows)**
  - *Nguyên nhân:* Tên bị trống, email sai định dạng (ví dụ: `nguyenvana.gmail.com`).
  - *Xử lý:* Lớp Validator bắt lỗi và hệ thống quyết định **Bỏ qua (Skip)** dòng đó. Một Warning Log được ghi nhận vào hệ thống để kỹ sư theo dõi, nhưng vòng lặp import vẫn tiếp tục chạy cho các dòng tiếp theo. Tiến trình không bị hủy bỏ (Abort) toàn cục.

- **Kịch bản: Import đè danh sách nhiều lần**
  - *Nguyên nhân:* Nhà tài trợ gửi danh sách bản V2 có chứa 80% người cũ và 20% người mới.
  - *Xử lý:* Nhờ chiến lược Duplicate Detection bằng `(concert_id, email)`, 80% người cũ chỉ được cập nhật thông tin và hệ thống nhận diện họ đã có vé (Ticket tồn tại) nên không sinh vé mới, không gửi lại email báo vé. 20% người mới sẽ được cấp vé và gửi email bình thường.

## 7. Ràng buộc

- **Tính toàn vẹn (Data Integrity):** File CSV tải lên bắt buộc phải chứa các cột dữ liệu theo định dạng chuẩn (nhận diện qua header hoặc thứ tự cột).
- **Bộ nhớ (Memory):** Quá trình đọc CSV lớn phải thực hiện theo cơ chế streaming (đọc từng dòng) thay vì tải toàn bộ file vào RAM để tránh lỗi Out Of Memory (OOM).
- **Sự độc lập luồng (Isolation):** Việc một dòng lỗi định dạng không được làm sập hay gián đoạn quá trình xử lý của các dòng hợp lệ còn lại.

## 8. Tiêu chí chấp nhận

1. **Import thành công:** Các bản ghi hợp lệ được tạo tài khoản Guest, tạo Ticket dạng GUEST và đẩy thông điệp Email vào RabbitMQ thành công.
2. **Bỏ qua lỗi:** Hệ thống ghi nhận và bỏ qua các bản ghi không có email hoặc email sai định dạng, đồng thời tiếp tục import các bản ghi khác bình thường.
3. **Upsert khách mời cũ:** Khi import lại file CSV có chứa email cũ, hệ thống cập nhật thông tin mới của khách mời đó mà không sinh ra một vé thứ hai.
4. **Ẩn doanh thu:** Hạng vé GUEST tự động sinh ra có giá trị 0 VND và không được cộng gộp vào biểu đồ doanh thu thương mại.
