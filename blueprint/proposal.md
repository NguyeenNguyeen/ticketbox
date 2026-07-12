# TicketBox — Project Proposal

## 1. Vấn đề

### 1.1. Bối cảnh thị trường

Thị trường tổ chức concert âm nhạc quy mô lớn tại Việt Nam đang phát triển mạnh mẽ với các chương trình đình đám như Anh Trai Say Hi, Anh Trai Vượt Ngàn Chông Gai, Em Xinh Say Hi, Chị Đẹp Đạp Gió Rẽ Sóng — mỗi sự kiện thu hút hàng chục nghìn khán giả. Tuy nhiên, quy trình bán vé hiện tại còn nhiều bất cập nghiêm trọng ảnh hưởng trực tiếp đến trải nghiệm người dùng và uy tín của ban tổ chức.

### 1.2. Các vấn đề cốt lõi

**Sập hệ thống khi mở bán:**  
Khi một concert lớn mở bán vé, lượng truy cập đồng thời có thể lên đến 80.000 người trong 5 phút đầu, với 70% tập trung vào phút đầu tiên. Các hệ thống bán vé hiện tại không được thiết kế để chịu tải đột biến như vậy, dẫn đến tình trạng website sập trong vài phút đầu.

**Trừ tiền không nhận được vé (Double-Charging):**  
Do thiếu cơ chế đảm bảo tính idempotent trong giao dịch, khán giả có thể bị trừ tiền nhiều lần khi mạng chập chờn hoặc khi bấm nút thanh toán lại. Đây là vấn đề nghiêm trọng ảnh hưởng trực tiếp đến niềm tin của người dùng.

**Scalper bot vét hết vé:**  
Scalper sử dụng bot tự động gửi hàng nghìn request mua vé trong vài giây, vét hết toàn bộ vé trước khi khán giả thật kịp thao tác. Sau đó, vé được bán lại trên thị trường chợ đen với giá gấp nhiều lần, gây thiệt hại cho cả khán giả lẫn ban tổ chức.

**Quy trình bán vé thủ công, rời rạc:**  
Nhiều sự kiện vẫn bán vé qua các kênh rời rạc: Zalo OA, Google Form, chuyển khoản thủ công. Cách tiếp cận này không đảm bảo tính công bằng, dễ xảy ra gian lận, khó kiểm soát tồn kho vé, và không có khả năng truy vết giao dịch.

**Soát vé tại sự kiện bị gián đoạn:**  
Các địa điểm tổ chức concert lớn như sân vận động và nhà thi đấu thường có vùng phủ sóng không ổn định khi hàng chục nghìn người tập trung. Nếu hệ thống soát vé phụ thuộc hoàn toàn vào kết nối mạng, nhân viên tại cổng không thể xác minh vé khi mất sóng, gây ùn tắc và ảnh hưởng trải nghiệm.

### 1.3. Tác động

| Bên liên quan | Tác động |
|---------------|----------|
| Khán giả | Mất tiền không nhận vé; không mua được vé do bot; trải nghiệm mua vé tiêu cực |
| Ban tổ chức | Mất uy tín; doanh thu bị ảnh hưởng; chi phí xử lý khiếu nại tăng |
| Nhãn hàng tài trợ | Không có hệ thống quản lý khách mời VIP tích hợp; phụ thuộc trao đổi CSV thủ công |

---

## 2. Mục tiêu

### 2.1. Mục tiêu tổng quát

Xây dựng hệ thống **TicketBox** — nền tảng bán vé concert trực tuyến — số hóa toàn bộ quy trình từ lúc mở bán đến khi khán giả vào cổng sự kiện, với các đặc tính:

- **Chịu tải cao:** Hỗ trợ 80.000 người truy cập đồng thời trong 5 phút đầu mở bán mà không sập.
- **Công bằng:** Áp dụng cơ chế rate limiting và giới hạn vé per-user để đảm bảo phân phối vé công bằng.
- **An toàn tài chính:** Không bao giờ trừ tiền hai lần cho cùng một giao dịch; không bao giờ bán vượt số lượng vé.
- **Khả năng chịu lỗi:** Hệ thống tiếp tục hoạt động bình thường khi cổng thanh toán gặp sự cố.
- **Offline-ready:** Soát vé tại cổng hoạt động ngay cả khi mất kết nối mạng.

### 2.2. Mục tiêu định lượng

| Chỉ số | Mục tiêu |
|--------|----------|
| Concurrent Users (CCU) | ≥ 80.000 trong 5 phút đầu |
| Ticket Overselling | Tuyệt đối bằng 0 |
| Double-Charging | Tuyệt đối bằng 0 |
| System Availability khi Payment Gateway lỗi | 100% cho tính năng non-payment |
| Offline Check-in | Hoạt động đầy đủ khi mất kết nối |

---

## 3. Người dùng và nhu cầu

### 3.1. Khán giả (CUSTOMER)

**Nhu cầu chính:**
- Xem danh sách concert sắp diễn ra với thông tin chi tiết (nghệ sĩ, địa điểm, thời gian, sơ đồ chỗ ngồi)
- Xem số vé còn lại theo thời gian thực cho từng loại vé (GA, SVIP, VIP, CAT1, CAT2)
- Chọn loại vé, số lượng và thanh toán an toàn qua VNPAY hoặc MoMo
- Nhận e-ticket dưới dạng mã QR qua email sau khi thanh toán thành công
- Xem lịch sử đơn hàng và vé đã mua

**Điều quan trọng nhất:** Trải nghiệm mua vé nhanh, công bằng, không bị lỗi hoặc mất tiền.

### 3.2. Ban tổ chức (ORGANIZER)

**Nhu cầu chính:**
- Tạo và quản lý concert mới (thông tin, lịch trình, nghệ sĩ, sơ đồ chỗ ngồi SVG)
- Cấu hình loại vé (tên, giá, số lượng, giới hạn per-user, thời điểm mở bán)
- Theo dõi doanh thu và thống kê bán vé theo thời gian thực
- Tải lên PDF press kit nghệ sĩ để AI tự động tạo tiểu sử
- Nhập danh sách khách mời VIP từ file CSV của nhãn hàng tài trợ
- Hủy hoặc tạm hoãn concert khi cần

**Điều quan trọng nhất:** Công cụ quản trị trực quan, chính xác, với khả năng kiểm soát toàn diện.

### 3.3. Nhân sự soát vé (CHECKER)

**Nhu cầu chính:**
- Quét mã QR trên e-ticket bằng mobile app tại cổng sự kiện
- Soát vé ngay cả khi mất kết nối mạng (offline check-in)
- Đồng bộ tự động dữ liệu soát vé khi kết nối được khôi phục
- Ngăn chặn vé bị sử dụng hai lần

**Điều quan trọng nhất:** App soát vé phải hoạt động nhanh và ổn định bất kể điều kiện mạng.

### 3.4. Nhãn hàng tài trợ (Actor ngoài)

**Nhu cầu:** Gửi danh sách khách mời VIP dưới dạng file CSV để hệ thống tự động nhập và quản lý.

---

## 4. Phạm vi

### 4.1. Thuộc phạm vi đồ án

| Tính năng | Mô tả |
|-----------|-------|
| Xem concert & mua vé | Trang công khai với sơ đồ chỗ ngồi SVG tương tác, chọn vé, thanh toán |
| Quản trị concert | Dashboard quản lý sự kiện, vé, doanh thu |
| E-ticket & QR | Sinh mã QR, gửi email xác nhận kèm PDF vé điện tử |
| Phân quyền RBAC | Ba vai trò (CUSTOMER, ORGANIZER, CHECKER) với kiểm soát truy cập chặt chẽ |
| Rate Limiting | Token Bucket phân tán qua Redis |
| Circuit Breaker | Bảo vệ cổng thanh toán với Resilience4j |
| Idempotency | Chống trừ tiền hai lần qua Redis SETNX |
| Caching | Cache-aside với Redis cho dữ liệu concert |
| Async Processing | RabbitMQ cho email, CSV import, AI bio |
| AI Artist Bio | Tự động tạo tiểu sử nghệ sĩ từ PDF |
| CSV VIP Import | Nhập danh sách khách mời từ file CSV |
| Dữ liệu mẫu | Seed data bốn concert mẫu có đầy đủ loại vé |

### 4.2. KHÔNG thuộc phạm vi

| Hạng mục | Lý do |
|----------|-------|
| Tích hợp payment gateway thật (VNPAY/MoMo) | Sử dụng sandbox/mock cho mục đích học thuật |
| Hạ tầng production (Kubernetes, CI/CD pipeline) | Đồ án tập trung vào thiết kế phần mềm, không phải vận hành |
| Tính năng social (bình luận, chia sẻ) | Nằm ngoài yêu cầu nghiệp vụ cốt lõi |
| Tích hợp SMS/Zalo OA thực tế | Kiến trúc hỗ trợ mở rộng, nhưng chỉ cài đặt email |
| Load testing production-grade | Thiết kế hướng đến khả năng chịu tải, kiểm chứng qua unit test và thiết kế |

---

## 5. Rủi ro và ràng buộc

### 5.1. Rủi ro kỹ thuật đã nhận diện

| Rủi ro | Mức độ | Giải pháp thiết kế |
|--------|--------|-------------------|
| **Tranh chấp vé** — 200 vé SVIP, hàng chục nghìn người mua đồng thời | Cao | Pessimistic Locking (SELECT FOR UPDATE) + Redis distributed lock |
| **Tải đột biến** — 80.000 CCU trong 5 phút, 70% ở phút đầu | Cao | Token Bucket rate limiting (Bucket4j + Redis) |
| **Cổng thanh toán không ổn định** — VNPAY/MoMo lỗi kéo dài | Cao | Circuit Breaker + Bulkhead (Resilience4j) + Graceful Degradation |
| **Trừ tiền hai lần** — mạng chập chờn, F5 lặp lại | Cao | Idempotency Key (Redis SETNX, TTL 10 phút) |
| **Soát vé offline** — sóng yếu tại sân vận động | Trung bình | SQLite local + RSA offline verification + background sync |
| **Tích hợp một chiều** — CSV nhãn hàng, không có API | Trung bình | RabbitMQ worker + fault-tolerant line-by-line processing |
| **Giới hạn vé per-user dưới tải cao** | Trung bình | Redis distributed lock theo user_id + category_id |
| **Quá tải database cho trang đọc nhiều** | Trung bình | Cache-aside (Redis) với TTL phân tầng |

### 5.2. Ràng buộc

| Ràng buộc | Chi tiết |
|-----------|----------|
| Hạ tầng phát triển | Windows 11, cấu hình vừa phải — chọn Docker image nhẹ (alpine) |
| Đồ án học thuật | Yêu cầu cài đặt đầy đủ, không chỉ stub hoặc mock |
| Nhóm 4 thành viên | Phân công rõ ràng theo domain, mỗi người chịu trách nhiệm một phân hệ |
| Dữ liệu mẫu bắt buộc | Bốn concert mẫu với đầy đủ loại vé, giá cả, sơ đồ chỗ ngồi |
| Tài liệu Blueprint | Theo cấu trúc OpenSpec |

### 5.3. Phân công nhóm

| Thành viên | Phân hệ | Trách nhiệm chính | Đóng góp | Đánh giá hoàn thành |
|------------|---------|-------------------|----------|---------------------|
| 23120107 - Nguyễn Phạm Trí Viễn | Web Frontend & UX | Next.js, Tailwind CSS, Zustand, TanStack Query, SVG Seat Map | 30% | 100% |
| 23120306 - Đào Nguyễn Nguyên | Infrastructure, Async & AI, Báo cáo & Phân công | Docker, Rate Limiting, Circuit Breaker, RabbitMQ, AI Worker, Email Worker | 30% | 100% |
| 23120307 - Trần Nguyễn Nguyên | Backend Core & Security | Entity, Repository, Service, Security, Design Patterns | 30% | 100% |
| 21120597 - Mai Huy Vũ | Mobile & Offline Check-in | Mobile app soát vé (Trạng thái hiện tại: Chưa cài đặt) | 0% | 0% |
