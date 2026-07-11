# TicketBox — Technical Design

## Kiến trúc tổng thể

Hệ thống TicketBox được thiết kế theo kiến trúc **Monolithic** (Nguyên khối) kết hợp với các dịch vụ phụ trợ phân tán (Redis, RabbitMQ) vận hành trong môi trường container hóa (Docker).

**Lý do lựa chọn kiến trúc:**
- **Sự đơn giản và nhất quán:** Phù hợp với quy mô nhóm phát triển (4 thành viên) và đảm bảo tính thống nhất về mặt dữ liệu, transaction (ACID) cho luồng mua vé cốt lõi thay vì đối mặt với sự phức tạp của Distributed Transactions trong Microservices.
- **Dễ dàng bảo trì và triển khai:** Toàn bộ backend được đóng gói và vận hành qua Docker Compose, giảm thiểu rủi ro sai lệch môi trường.
- **Khả năng chịu tải linh hoạt:** Thay vì chia nhỏ thành microservices ngay từ đầu, hệ thống sử dụng kiến trúc lai (Hybrid) bằng cách tách bạch các tác vụ nặng/chậm (gửi email, xử lý AI, import CSV) sang xử lý bất đồng bộ (Message Queue) thông qua RabbitMQ, kết hợp với Redis làm bộ đệm giảm tải trực tiếp cho Database.

**Các thành phần chính và cách giao tiếp:**
- **Web Frontend (Next.js):** Phục vụ khán giả mua vé và ban tổ chức quản trị. Giao tiếp với Backend qua REST API và nhận luồng dữ liệu cập nhật trạng thái ghế thời gian thực qua Server-Sent Events (SSE).
- **Mobile Check-in App:** Dành cho nhân sự soát vé. Giao tiếp qua REST API để đồng bộ dữ liệu ban đầu và gửi log soát vé. (Trạng thái cài đặt hiện tại: Chưa cài đặt (Not implemented)).
- **Backend API (Spring Boot):** Đóng vai trò là trung tâm xử lý logic nghiệp vụ, quản lý trạng thái đơn hàng và kiểm soát đồng thời (concurrency).
- **Cơ sở dữ liệu (PostgreSQL):** Lưu trữ toàn bộ dữ liệu nghiệp vụ cốt lõi với tính toàn vẹn cao.
- **Bộ nhớ đệm (Redis):** Xử lý Rate Limiting, Idempotency keys, Distributed Locks và Caching (Cache-aside).
- **Message Broker (RabbitMQ):** Tiếp nhận các tác vụ bất đồng bộ từ Backend và chuyển tiếp cho các Worker xử lý (Email, AI, CSV).
- **External Services:** Backend giao tiếp với cổng thanh toán (VNPAY/MoMo), Dịch vụ Email đa nền tảng (Resend, Brevo, Gmail SMTP) và API AI (Gemini/OpenAI) thông qua HTTP REST.

---

## C4 Diagram

### Level 1 — System Context

Bức tranh toàn cảnh về cách các tác nhân (Actors) và hệ thống bên ngoài tương tác với TicketBox.

![[image/System Context Diagram.svg]]

**Thành phần:**
- **Khán giả (Customer):** Sử dụng hệ thống để xem thông tin và mua vé.
- **Ban tổ chức (Organizer):** Quản lý sự kiện, tải lên PDF/CSV, xem báo cáo.
- **Nhân sự soát vé (Checker):** Sử dụng Mobile App để quét QR.
- **TicketBox System:** Hệ thống trung tâm.
- **Cổng thanh toán (VNPAY / MoMo):** Xử lý giao dịch tài chính.
- **Hệ thống AI (Gemini):** Xử lý văn bản, tóm tắt tiểu sử nghệ sĩ từ PDF.
- **Dịch vụ Email (Resend / Brevo / SMTP):** Gửi e-ticket và thông báo.

### Level 2 — Container

Phân rã hệ thống TicketBox thành các container độc lập và cách chúng giao tiếp.

![[image/Container Diagram.svg]]

**Thành phần:**
- **Web Application (Next.js):** Cung cấp giao diện người dùng (SSR/SPA).
- **Mobile Check-in App (React Native/Flutter):** Ứng dụng soát vé offline-first (SQLite cục bộ).
- **Backend API (Spring Boot):** Xử lý REST request, xác thực JWT, tương tác với CSDL.
- **Async Workers (Spring Boot):** Nằm trong cùng khối Backend nhưng hoạt động đa luồng độc lập, lắng nghe từ Message Queue.
- **PostgreSQL Database:** Lưu trữ dữ liệu cấu trúc (Users, Concerts, Orders, Tickets, Guests).
- **Redis Cache:** Lưu trữ session, tokens, lock, cache dữ liệu.
- **RabbitMQ:** Hàng đợi thông điệp cho các tác vụ nền.

---

## High-Level Architecture Diagram

Sơ đồ thể hiện luồng dữ liệu chính và sự phối hợp giữa các thành phần hạ tầng bảo vệ, đặc biệt tại điểm tích hợp và soát vé.

![[image/High-Level Architecture Diagram.png]]

**Luồng dữ liệu đặc tả:**
1. **Bảo vệ hệ thống:** Mọi request đi qua lớp Rate Limiter (Token Bucket trên Redis) trước khi vào Controller.
2. **Xử lý đơn hàng:** Giao dịch thanh toán được bảo vệ bởi Circuit Breaker (Resilience4j). Nếu thanh toán thành công, Backend lưu trạng thái vào PostgreSQL.
3. **Luồng bất đồng bộ:** Sau khi mua thành công, Backend gửi message chứa ID vé vào RabbitMQ (Exchange: ticketbox.commands). Email Worker tiêu thụ message, gọi API/Dịch vụ Email (Resend/Brevo/SMTP) để gửi e-ticket đính kèm mã QR. Các tác vụ thất bại (quá số lần Retry) được đẩy vào Dead Letter Queue (DLQ).
4. **Luồng soát vé offline (Thiết kế):** Mobile app tải danh sách mã QR băm (hashed) từ Backend. Khi mất mạng, app quét QR, xác thực cục bộ bằng mã băm hoặc public key, lưu log vào bộ nhớ nội bộ. Khi có mạng, Worker đồng bộ đẩy log về Backend. (Chưa cài đặt).

---

## Thiết kế cơ sở dữ liệu

**Lựa chọn:** Relational Database (SQL) — cụ thể là **PostgreSQL**.
**Lý do:** Hệ thống bán vé là một hệ thống thương mại điện tử đặc thù. Các nghiệp vụ như khóa ghế, tạo đơn hàng, thanh toán yêu cầu tính ACID (Atomicity, Consistency, Isolation, Durability) cực kỳ khắt khe để tránh sai lệch dữ liệu tài chính.

### Schema các Entity chính

![[image/Database ER Diagram.png]]

- **User:** Lưu trữ thông tin tài khoản, `username`, `password` (bcrypt hashing), `role` (CUSTOMER, ORGANIZER, CHECKER).
- **Concert & Artist:** Lưu trữ thông tin sự kiện và tiểu sử nghệ sĩ (được AI generate). Bảng trung gian `concert_artists` thể hiện quan hệ n-n.
- **TicketCategory:** Phân loại vé (VIP, GA...), `price`, `total_quantity`, `available_quantity` (quan trọng nhất để chống oversell), `max_per_user`.
- **Order & OrderItem:** Đơn hàng và chi tiết vé. `Order` có trường trạng thái (PENDING, PAYING, COMPLETED, CANCELLED) và `idempotency_key`.
- **Ticket:** Vé điện tử đã phát hành, chứa `qr_code` (định danh duy nhất) và `status` (ACTIVE, USED, REVOKED). Tham chiếu ngược về `Order` và `TicketCategory`.
- **Guest:** Khách mời VIP (được worker import từ file CSV của nhà tài trợ), liên kết 1-1 với `Ticket` khi phát hành vé.

---

## Thiết kế kiểm soát truy cập

Hệ thống sử dụng cơ chế **Stateless Authentication** thông qua **JSON Web Token (JWT)** và kiểm soát quyền truy cập dựa trên vai trò (RBAC - Role-Based Access Control).

**Các nhóm người dùng:**
- **CUSTOMER (Khán giả):** Chỉ được phép truy cập các API public (`GET /api/concerts/**`), gọi API thanh toán của bản thân (`POST /api/tickets/purchase`) và xem lịch sử đơn hàng cá nhân.
- **ORGANIZER (Ban tổ chức):** Có toàn quyền (CRUD) quản trị sự kiện (`/api/admin/**`), tạo vé, tải lên file PDF/CSV và xem thống kê doanh thu.
- **CHECKER (Nhân sự soát vé):** Chỉ được phép thao tác các endpoint nghiệp vụ soát vé (`/api/checker/**`).

**Kiểm tra quyền tại từng điểm truy cập:**
1. **Lớp Filter (Backend - Spring Security):** `JwtAuthenticationFilter` giải mã token từ header `Authorization`, xác thực định danh. Sau đó, cấu hình Spring Security `SecurityFilterChain` kết hợp method security bảo vệ chặn từng endpoint cụ thể theo Role.
2. **Lớp Middleware (Next.js - Frontend):** Middleware kiểm tra cookie chứa JWT; chặn tất cả truy cập vào các route nội bộ `/admin/*` nếu người dùng không phải là quản trị viên, tự động điều hướng (redirect) về trang đăng nhập.
3. **Lớp UI (Frontend State - Zustand):** Tự động giải mã (decode) base64 JWT payload trực tiếp tại client thay vì phải gọi API verify. Từ đó quyết định việc ẩn hoặc vô hiệu hóa các nút chức năng trong giao diện (VD: ẩn menu Quản trị đối với khán giả thông thường).

---

## Thiết kế các cơ chế bảo vệ hệ thống

### 1. Kiểm soát tải đột biến (Rate Limiting)

Nhằm bảo vệ Backend API khỏi sự cố kiệt quệ tài nguyên (Resource Exhaustion) do bot hoặc lượng truy cập khổng lồ, hệ thống cài đặt thuật toán **Token Bucket** bằng thư viện **Bucket4j** kết hợp với **Redis** để lưu trữ trạng thái phân tán.

- **Pre-auth Gate:** Một lớp khiên bảo vệ đầu tiên dùng bộ nhớ RAM cục bộ (Caffeine Cache). Chặn các cuộc tấn công DDoS ở mức network/application cơ bản trước khi request chạm đến bộ xử lý JWT hay cơ sở dữ liệu.
- **Public Tier:** Giới hạn theo địa chỉ IP đối với các request nặc danh (VD: 300 request / phút).
- **Auth Tier:** Một "Bucket" riêng biệt, khắt khe hơn dành riêng cho các endpoint đăng nhập/đăng ký (`/api/auth/**`), ngăn chặn triệt để tấn công Brute-force (VD: 10 request / phút / IP).
- **Private Tier:** Giới hạn theo định danh người dùng `username` đọc từ JWT đối với các request đã đăng nhập. Cơ chế này đảm bảo chia sẻ băng thông công bằng giữa các khán giả trong cuộc chiến săn vé (VD: 300 request / phút / user).
- **Dung sai sự cố:** Nếu Redis gặp sự cố kết nối, lớp Rate Limiting được cấu hình theo cơ chế Fail-open, cho phép request đi qua nhưng ghi lại cảnh báo lỗi (Warn log) để hệ thống không bị gián đoạn cục bộ.

### 2. Xử lý cổng thanh toán không ổn định (Circuit Breaker)

Các cổng thanh toán ngoại vi (VNPAY/MoMo) có thể gặp sự cố nghẽn mạng nội bộ làm phản hồi cực chậm. Tình trạng này khiến các thread của hệ thống phải chờ đợi (block), làm cạn kiệt Connection Pool và kéo sập Backend TicketBox.

- **Giải pháp:** Sử dụng mẫu thiết kế **Circuit Breaker** (Cầu dao ngắt mạch) và **Bulkhead** (Vách ngăn cách ly) thông qua thư viện **Resilience4j**.
- **Cấu hình Thresholds:** Áp dụng Count-based Sliding Window. Nếu tỷ lệ thất bại (Failure Rate) vượt qua 50%, hoặc tỷ lệ phản hồi chậm (> 3s) vượt qua 80%, "cầu dao" sẽ chuyển sang trạng thái Open. Bulkhead giới hạn cứng số lượng thread đồng thời tương tác với cổng thanh toán.
- **Graceful Degradation (Suy giảm duyên dáng):** Khi mạch mở (Open) hoặc vách ngăn đã đầy, bất kỳ request mua vé nào mới đều lập tức bị từ chối thay vì bắt hệ thống phải chờ Timeout. Backend bắt exception `PaymentDeclinedException`, trạng thái đơn hàng chuyển ngay về `CANCELLED`, hoàn trả vé về kho và UI hiển thị banner lịch sự: *"Hệ thống thanh toán đang quá tải, vui lòng thử lại sau"*. Cùng lúc đó, tất cả tính năng khác như duyệt sự kiện, xem tin tức vẫn hoạt động mượt mà bình thường.

### 3. Chống trừ tiền hai lần (Idempotency)

Bảo vệ rủi ro người dùng vô tình mua trùng vé hoặc bị trừ tiền hai lần do mất kết nối mạng và bấm nút "Thanh toán" lặp đi lặp lại.

- **Cơ chế Idempotency Key:** Frontend tự động sinh ra một UUID v4 (Key) và đính kèm vào Header `Idempotency-Key` của request.
- **Xử lý cấp Backend:** Hệ thống dùng câu lệnh nguyên tử `setIfAbsent` (`SETNX`) của Redis để ghi nhận Key này, kèm một thời gian sống (TTL) là 10 phút. Nếu một request thứ hai có cùng Key đến (do người dùng retry), Redis sẽ từ chối.
- **Luồng ngoại lệ:** Khi phát hiện giao dịch trùng, Backend lập tức ngắt quy trình, trả về phản hồi *"Giao dịch đang được xử lý"*, cấm gọi hàm tạo đơn hàng và trừ tiền mới.

### 4. Caching và Tối ưu Data Fetching

Giảm tải lên PostgreSQL đối với các truy vấn tần suất cực cao (trang chủ, trang chi tiết sự kiện).

- **Chiến lược:** Áp dụng mô hình **Cache-aside** sử dụng `RedisCacheManager` của Spring Boot.
- **Dữ liệu tĩnh:** (Thông tin sự kiện, danh sách nghệ sĩ). TTL được thiết lập dài (30 phút).
- **Invalidation:** Cache sẽ bị tự động xóa bỏ (Evict) lập tức nếu ban tổ chức thực hiện thao tác tạo, sửa, xóa, hủy sự kiện, hoặc khi AI hoàn tất việc sinh Bio. Điều này đảm bảo khán giả luôn thấy thông tin mới nhất.
- **Trạng thái Realtime:** Đối với dữ liệu biến thiên liên tục như số lượng vé còn lại hoặc ghế đang khóa, hệ thống bỏ qua Cache thông thường và truyền tải trực tiếp thông qua cơ chế Server-Sent Events (SSE) để giảm thiểu lag và tránh dữ liệu ảo.

---

## Các quyết định kỹ thuật quan trọng (ADR)

### ADR-1: Sử dụng Pessimistic Locking thay vì Optimistic Locking tại Data Layer
- **Quyết định:** Áp dụng `SELECT FOR UPDATE` trên Entity `TicketCategory` thay vì `@Version` ở tầng JPA.
- **Lý do:** Ở bài toán bán vé, tỷ lệ tranh chấp là cực đoan (Ví dụ 80.000 khán giả giành 200 vé). Nếu dùng Optimistic Locking, hệ thống sẽ văng exception liên tục, dẫn đến số lượng Retry lớn và gây hiệu ứng bão lỗi (Cascade Failure). Pessimistic Locking khóa luồng ở cấp độ dòng (row-level) của cơ sở dữ liệu, đảm bảo việc kiểm tra và trừ tồn kho vé là tuần tự (FIFO), ngăn chặn 100% hiện tượng bán lố (oversell) dù hiệu năng đánh đổi một phần (được bù đắp nhờ giới hạn Rate Limiting bên ngoài).

### ADR-2: Sử dụng RabbitMQ và Mô hình Event-Driven nội bộ thay vì Kafka
- **Quyết định:** Chọn RabbitMQ làm Message Broker phân tán tác vụ.
- **Lý do:** Hệ thống yêu cầu kiểm soát luồng xử lý chặt chẽ theo tính chất giao dịch nhỏ (Gửi email, Import CSV), không phải streaming hàng tỷ sự kiện logs Big Data. RabbitMQ cung cấp khả năng định tuyến rõ ràng (Direct/Topic Routing), hỗ trợ tính năng Acknowledgment (Chấp nhận tin nhắn) đáng tin cậy. Đặc biệt, kiến trúc RabbitMQ hỗ trợ thiết lập Dead Letter Queue (DLQ) đơn giản để bắt các email lỗi hoặc dòng CSV hỏng mà không làm đứng hàng đợi chính.

### ADR-3: Client-side Global State và Idempotency qua Next.js
- **Quyết định:** Sử dụng Zustand để quản lý State nhẹ thay vì Redux. Đẩy mạnh việc xử lý logic UI và Idempotency Key về phía Next.js App Router Client.
- **Lý do:** Sơ đồ SVG của một sân vận động với hàng nghìn SVG path (ghế) rất dễ làm giật lag giao diện nếu dùng Redux kích hoạt re-render toàn cục. Sử dụng Zustand với các hook Selector tách biệt giúp tối ưu hóa render: chỉ duy nhất cái ghế được bấm chuột mới thay đổi màu sắc. Next.js đóng vai trò sinh Idempotency key, kiểm soát offline mode và retry thông minh (Exponential Backoff) tự động nhằm giấu đi sự chập chờn của mạng, mang lại UX mượt mà nhất.
