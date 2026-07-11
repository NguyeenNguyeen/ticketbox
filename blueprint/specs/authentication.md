# Đặc tả: Authentication & Authorization (Xác thực và Phân quyền)

## 1. Mô tả

Phân hệ Authentication & Authorization chịu trách nhiệm định danh người dùng và kiểm soát quyền truy cập vào các tài nguyên của hệ thống TicketBox.

**Mục tiêu thiết kế:**
- Đảm bảo an toàn tuyệt đối thông tin tài khoản khán giả và dữ liệu sự kiện của ban tổ chức.
- Cung cấp cơ chế xác thực phi trạng thái (Stateless) để tối ưu hóa hiệu năng, giúp Backend chịu tải dễ dàng khi có hàng chục nghìn khán giả săn vé mà không tiêu tốn bộ nhớ lưu trữ phiên (Session).
- Tích hợp lớp phòng ngự ngay từ vòng ngoài nhằm ngăn chặn các cuộc tấn công dò mật khẩu (Brute-force).
- Phân tách rõ ràng luồng kiểm soát quyền giữa Backend (Bảo vệ dữ liệu/API) và Frontend (Bảo vệ giao diện/Trải nghiệm người dùng).

## 2. Các tác nhân (Actors) và Quyền hạn

Hệ thống sử dụng mô hình RBAC (Role-Based Access Control) cốt lõi với 3 vai trò (Role):

- **CUSTOMER (Khán giả):** Quyền truy cập cơ bản. Được phép xem danh sách sự kiện công khai, tham gia luồng mua vé, nhận e-ticket và xem lịch sử đơn hàng của chính mình.
- **ORGANIZER (Ban tổ chức):** Quyền quản trị hệ thống. Được phép truy cập vào Admin Dashboard để tạo, sửa, xóa, tạm hoãn concert; cấu hình giới hạn hạng vé; tải lên file PDF (cho AI xử lý) và CSV (danh sách VIP); cũng như theo dõi báo cáo doanh thu.
- **CHECKER (Nhân sự soát vé):** Quyền thực thi nghiệp vụ soát vé tại cổng sự kiện. Chỉ được phép truy cập vào ứng dụng Mobile (hoặc API chuyên dụng) để quét mã QR e-ticket và đồng bộ lịch sử soát vé. (Lưu ý: Phân hệ ứng dụng cho Checker hiện chưa cài đặt).

## 3. Luồng chính

Hệ thống TicketBox áp dụng chuẩn JWT (JSON Web Token) cho toàn bộ quá trình xác thực.

![[../image/JWT Authentication Flow.png]]

### 3.1. Luồng Đăng nhập (Login)

1. **Client (Web/Mobile):** Gửi thông tin định danh (`username`, `password`) tới API `POST /api/auth/login`.
2. **Rate Limiter (Lớp bảo vệ trước Auth):** Kiểm tra tần suất gửi request theo địa chỉ IP của Client. Nếu IP này gửi quá 10 requests/phút, request lập tức bị từ chối với lỗi `429 Too Many Requests` nhằm chống rà quét mật khẩu.
3. **Authentication Manager:** Spring Security tra cứu `username` trong cơ sở dữ liệu PostgreSQL. Nếu tồn tại, hệ thống đối chiếu mật khẩu (đã được băm bằng thuật toán BCrypt).
4. **Token Generation:** Nếu hợp lệ, hệ thống tạo ra một chuỗi JWT (`accessToken`). Chuỗi này mang thông tin định danh cơ bản và có thời hạn sống (Expiration) được định cấu hình sẵn (Ví dụ: 24 giờ).
5. **Client Storage:** Client nhận chuỗi `accessToken` và lưu trữ an toàn tại cục bộ để sử dụng cho các request phía sau.

### 3.2. Luồng Đăng ký (Register)

1. **Client:** Gửi payload đăng ký (`username`, `email`, `password`, `fullName`) tới `POST /api/auth/register`. Nếu không truyền Role, hệ thống mặc định gán là `CUSTOMER`.
2. **Validation:** Hệ thống kiểm tra tính hợp lệ của định dạng Email và xác minh chống trùng lặp `username` / `email` trong CSDL.
3. **Lưu trữ an toàn:** Mật khẩu được băm (Hash) một chiều thông qua `BCryptPasswordEncoder` trước khi lưu bản ghi vào bảng `users`.

## 4. Mô hình Phân quyền (Authorization Model)

### 4.1. Phân quyền tại Backend (API Protection)

Bảo vệ tài nguyên API là chốt chặn cuối cùng và quan trọng nhất.

1. **Trích xuất & Giải mã:** Ngoại trừ các route được công khai (`/api/auth/**`, `GET /api/concerts/**`), mọi request đều phải đi qua `JwtAuthenticationFilter`. Lớp này trích xuất token từ HTTP Header (`Authorization: Bearer <token>`).
2. **Xác thực Token:** Chữ ký (Signature) của token được kiểm chứng bằng Secret Key của server. Đồng thời kiểm tra thời gian sống (`exp`). Nếu token bị giả mạo hoặc hết hạn, request bị chặn đứng.
3. **Kiểm tra quyền hạn (Role Check):** Thông tin người dùng được đưa vào `SecurityContext`. Tại tầng Controller, cơ chế Spring Security (định tuyến URL hoặc Annotation) sẽ chặn các yêu cầu trái phép:
   - Các API `/api/admin/**` bắt buộc tài khoản phải có quyền `ORGANIZER`.
   - Các API `/api/checker/**` bắt buộc quyền `CHECKER`.
   - Các API mua vé `/api/tickets/purchase` chỉ yêu cầu đã đăng nhập hợp lệ.

### 4.2. Phân quyền tại Frontend (UI Protection)

Bảo vệ giao diện giúp tối ưu UX, không để người dùng thấy những tính năng họ không có quyền bấm vào.

1. **Next.js Middleware:** Chạy độc lập ở môi trường Edge. Middleware kiểm tra sự tồn tại của JWT trong Cookie mỗi khi có yêu cầu chuyển trang. Nếu khán giả bình thường cố gắng gõ URL `/admin/concerts` vào trình duyệt, Middleware sẽ phát hiện sự không tương thích về quyền và lập tức điều hướng (Redirect) về trang đăng nhập `/auth/login` mà không cần gọi API Backend.
2. **Zustand Client State:** Để tối ưu tốc độ, Frontend của TicketBox tự động giải mã Base64 của JWT Payload trực tiếp tại trình duyệt để trích xuất `username` và `role`. Nhờ đó, UI có thể ngay lập tức quyết định việc ẩn nút "Trang quản trị" trên thanh điều hướng đối với Khán giả, mà không phải tốn thời gian gọi thêm một API `/api/auth/me`.

## 5. Quyết định Thiết kế và Công nghệ (Design Rationale)

**1. Lựa chọn JWT Stateless thay vì Stateful Session**
- *Lý do:* Hệ thống TicketBox sinh ra để giải quyết bài toán tải đột biến (80.000 CCU). Nếu sử dụng Session truyền thống, Server sẽ phải cấp phát bộ nhớ RAM để lưu phiên cho hàng chục nghìn người, hoặc phải duy trì cụm Redis cực lớn để đồng bộ Session. JWT khắc phục hoàn toàn điểm yếu này: Backend hoàn toàn "phi trạng thái" (Stateless), chỉ dùng CPU để giải mã và xác thực chữ ký toán học, cho phép dễ dàng mở rộng nhiều máy chủ (Horizontal Scaling) mà không lo mất phiên đăng nhập.

**2. Cô lập luồng Rate Limiting cho nhóm API Authentication**
- *Lý do:* Mọi hệ thống bán vé đều là mục tiêu của Botnet. Nếu áp dụng chung giới hạn (Ví dụ 300 req/min) cho cả API mua vé và API đăng nhập, kẻ tấn công có thể cấu hình Bot dò mật khẩu 300 lần/phút/IP. Thiết kế của TicketBox cấp cho nhóm `/api/auth/**` một "Tier" (luồng bảo vệ) riêng biệt và nghiêm ngặt (chỉ 10 req/min/IP).

**3. Tiêu chuẩn băm mật khẩu BCrypt**
- *Lý do:* Không sử dụng MD5 hay SHA-256 thông thường. BCrypt kết hợp cơ chế tự sinh muối (Salt) ngẫu nhiên cho mỗi mật khẩu và khả năng tùy chỉnh chi phí thuật toán (Work Factor), khiến cho các cuộc tấn công bằng Rainbow Table hoặc GPU Brute-force trở nên bất thi.

## 6. Kịch bản lỗi

Hệ thống bắt và phản hồi các kịch bản ngoại lệ bảo mật một cách chuẩn hóa:

- **Sai tài khoản/mật khẩu:** Backend trả về `401 Unauthorized`. Frontend báo lỗi thân thiện "Sai tên đăng nhập hoặc mật khẩu".
- **Token hết hạn / Giả mạo:** `JwtAuthenticationFilter` phát hiện và trả về `401 Unauthorized`. Khi nhận mã này ở bất kỳ API nào, Interceptor của Frontend sẽ tự động xóa token ở LocalStorage/Cookie và đẩy khán giả về trang đăng nhập.
- **Vượt quyền (Forbidden Access):** Khi một Customer gọi API của Admin (do dùng công cụ như Postman), Spring Security trả về mã `403 Forbidden`. Hành động này có thể được ghi log cảnh báo bảo mật.
- **Tấn công dò mật khẩu:** Redis Token Bucket cạn kiệt, trả về `429 Too Many Requests`.
- **Đăng ký trùng dữ liệu:** Trả về `400 Bad Request` khi `username` hoặc `email` đã tồn tại trong hệ thống.

## 7. Ràng buộc

- **Bảo mật:** Mật khẩu bắt buộc phải được băm bằng thuật toán BCrypt trước khi lưu. Không bao giờ lưu bản rõ.
- **Tính phi trạng thái (Stateless):** JWT Token tuyệt đối không được lưu trữ trên server session (RAM). Server phải thực hiện giải mã và xác thực tính toàn vẹn của token thuần túy bằng thuật toán chữ ký số HMAC và Secret Key.
- **Thời gian sống (TTL):** JWT Token có thời hạn hợp lệ nghiêm ngặt (ví dụ: 24 giờ). Khi hết hạn, khán giả bắt buộc phải đăng nhập lại.
- **Hiệu năng:** Quá trình giải mã JWT phải diễn ra đủ nhanh (thông thường dưới 5ms) tại lớp Filter trước khi chạm tới Controller để không tạo nút thắt cổ chai.

## 8. Tiêu chí chấp nhận

1. **Đăng nhập thành công:** Người dùng gửi đúng bộ thông tin `username`/`password` nhận được mã HTTP `200 OK` kèm chuỗi JWT hợp lệ chứa đầy đủ Claims.
2. **Đăng nhập thất bại:** Gửi sai mật khẩu hoặc tài khoản không tồn tại, hệ thống phản hồi HTTP `401 Unauthorized` với thông điệp chung chung nhằm không rò rỉ thông tin (không chỉ rõ là sai username hay password).
3. **Phân quyền chính xác:** Token JWT giải mã ra bắt buộc phải chứa đúng Role tương ứng của tài khoản.
4. **Từ chối truy cập (Authorization):** Một tài khoản `CUSTOMER` khi gửi Request gọi API dành cho `ORGANIZER` sẽ bị lớp Security Filter từ chối với mã HTTP `403 Forbidden`.
5. **Chống đăng ký trùng:** Hành vi đăng ký tài khoản mới với email hoặc username đã tồn tại trong Database phải lập tức bị chặn lại với lỗi HTTP `400 Bad Request`.
