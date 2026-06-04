# **1\. Yêu cầu cần làm**

Vai trò Frontend đảm nhận 3 nhóm chức năng chính trong đồ án:

## **A. Xem & Mua vé (phía Khán giả)**

- Trang danh sách concert: thông tin nghệ sĩ, địa điểm, số vé còn lại gần đúng realtime
- Trang chi tiết concert với sơ đồ SVG tương tác (GA, SVIP, VIP, CAT1, CAT2): tô màu theo zone, cho phép click chọn ghế, cập nhật realtime
- Form thanh toán tích hợp VNPAY/MoMo
- Hiển thị e-ticket dạng QR sau khi mua thành công

## **B. Trang Admin (Ban tổ chức)**

- Tạo/sửa/hủy concert
- Cấu hình loại vé: tên, giá, số lượng, thời điểm mở bán, giới hạn vé/tài khoản
- Thống kê doanh thu realtime
- Phân quyền UI: ẩn/hiện nút theo role (khán giả / ban tổ chức / nhân sự soát vé)

## **C. Chống quá tải từ phía client**

- Disable nút ngay khi click - chặn double submit
- Countdown timer khi đang giữ ghế (seat hold timeout)
- Xử lý hiển thị lỗi: hết vé, rớt mạng, timeout thanh toán

# **2\. Trade-off Chính Cần Phân Tích**

## **Trade-off 1: Cập nhật trạng thái ghế - Realtime hay Polling?**

Sơ đồ chỗ ngồi không chỉ là hình ảnh - nó là một widget trạng thái động phản ánh realtime ai đang giữ ghế nào. Mỗi ghế có 4 trạng thái: available → held → taken → selected.

| **Phương án**                | **Ʈu điểm**                              | **Nhược điểm**                            |
| ---------------------------- | ---------------------------------------- | ----------------------------------------- |
| **Polling (3-5s)**           | Đơn giản, không cần infra thêm           | Không thực sự realtime; tăng tải API      |
| ---                          | ---                                      | ---                                       |
| **WebSocket**                | Hai chiều, latency thấp nhất             | Phức tạp, tốn tài nguyên server, overkill |
| ---                          | ---                                      | ---                                       |
| **SSE (Server-Sent Events)** | Server push một chiều, nhẹ hơn WebSocket | Chỉ server → client, không gửi ngược lại  |
| ---                          | ---                                      | ---                                       |

**→ Khuyến nghị:** SSE cho seat map updates (server push khi ghế bị taken/released), kết hợp polling 5-10s cho số vé tổng trên trang danh sách. SSE phù hợp vì client chỉ cần lắng nghe, không cần gửi ngược.

## **Trade-off 2: SVG Seat Map - Render & Tương tác**

Với concert quy mô Việt Nam (thường dưới 2000 chỗ ngồi có số), thách thức là chọn cách render SVG đảm bảo có thể tương tác mà không làm giật UI.

| **Phương án**            | **Ʈu điểm**                         | **Nhược điểm**                        |
| ------------------------ | ----------------------------------- | ------------------------------------- |
| **Inline SVG trong DOM** | Click event trực tiếp, dễ style     | DOM nặng nếu >2000 element            |
| ---                      | ---                                 | ---                                   |
| **Canvas (react-konva)** | Hiệu năng cao với hàng nghìn object | Phức tạp, khó debug, không accessible |
| ---                      | ---                                 | ---                                   |
| **SVG as &lt;img&gt;**   | Load nhanh                          | Không thể tương tác                   |
| ---                      | ---                                 | ---                                   |

**→ Khuyến nghị:** Inline SVG là lựa chọn phù hợp cho concert quy mô Việt Nam. Cần dùng React.memo cho từng ghế để tránh re-render toàn bộ sơ đồ khi một ghế thay đổi trạng thái.

## **Trade-off 3: Phân quyền UI - Decode JWT phía client hay fetch từ server?**

3 nhóm người dùng truy cập cùng một hệ thống web nhưng thấy giao diện khác nhau - không chỉ ẩn/hiện nút mà còn bảo vệ route.

| **Phương án**                       | **Ʈu điểm**                           | **Nhược điểm**                        |
| ----------------------------------- | ------------------------------------- | ------------------------------------- |
| **Decode JWT ở client**             | Nhanh, không cần roundtrip            | Có thể bị bypass qua devtools         |
| ---                                 | ---                                   | ---                                   |
| **Fetch permissions từ API**        | Luôn chính xác                        | Chậm hơn, thêm request                |
| ---                                 | ---                                   | ---                                   |
| **Next.js Middleware bảo vệ route** | Check ở server trước khi render trang | Cần cấu hình, không thay thế API auth |
| ---                                 | ---                                   | ---                                   |

**→ Khuyến nghị:** Hai lớp kết hợp: decode JWT để ẩn/hiện UI (trải nghiệm người dùng), Next.js Middleware để chặn route /admin/\* không cho truy cập trực tiếp. Backend vẫn phải kiểm tra quyền độc lập ở mỗi API call - frontend chỉ là lớp UX, không phải lớp bảo mật.

## **Trade-off 4: Chống double-submit**

Khi 80.000 người cùng bấm "Mua vé", phía client là tuyến phòng thủ đầu tiên. Hai ngưy cơ chính: double submit (bấm 2 lần, trừ tiền 2 lần) và bão request (nút không disable, user hoảng loạn bấm liên tục).

| **Phương án**                           | **Ʈu điểm**                                 | **Nhược điểm**                            |
| --------------------------------------- | ------------------------------------------- | ----------------------------------------- |
| **Disable button ngay khi click**       | Đơn giản, ngăn bão request tức thì          | Nếu request fail, phải re-enable cẩn thận |
| ---                                     | ---                                         | ---                                       |
| **Idempotency key (uuid trong header)** | Backend nhận diện request trùng lập, bỏ qua | Phải phối hợp với backend                 |
| ---                                     | ---                                         | ---                                       |

**→ Khuyến nghị:** Dùng cả hai lớp: disable button là lớp UX ngay lập tức, idempotency key là lớp đảm bảo kỹ thuật khi mạng chập chửn. Chỉ re-enable button khi nhận được lỗi có thể retry (4xx validation), không re-enable khi đang chờ response.

## **Trade-off 5: State Management SVG Map**

| **Phương án**       | **Ʈu điểm**                         | **Nhược điểm**                           |
| ------------------- | ----------------------------------- | ---------------------------------------- |
| **useState thuần**  | Đơn giản                            | Khó share giữa seat map và form checkout |
| ---                 | ---                                 | ---                                      |
| **Zustand / Jotai** | Nhẹ, share state dễ, ít boilerplate | Thêm dependency                          |
| ---                 | ---                                 | ---                                      |
| **Redux Toolkit**   | Predictable, devtools tốt           | Overkill cho scope này                   |
| ---                 | ---                                 | ---                                      |

**→ Khuyến nghị:** Zustand - đủ nhẹ, không cần Provider wrapper, dễ tạo selector riêng cho từng ghế để tránh re-render thừa.

# **3\. Công nghệ Triển khai**

| **Mục đích**                 | **Công nghệ**                |
| ---------------------------- | ---------------------------- |
| **Framework chính**          | Next.js 14 (App Router)      |
| ---                          | ---                          |
| **UI Components**            | Tailwind CSS + shadcn/ui     |
| ---                          | ---                          |
| **Global State**             | Zustand                      |
| ---                          | ---                          |
| **Data Fetching & Caching**  | TanStack Query (React Query) |
| ---                          | ---                          |
| **SVG Seat Map**             | Inline SVG + React.memo      |
| ---                          | ---                          |
| **Realtime seat updates**    | SSE (EventSource API)        |
| ---                          | ---                          |
| **Auth & Middleware**        | next-auth + JWT              |
| ---                          | ---                          |
| **Form Validation**          | react-hook-form + zod        |
| ---                          | ---                          |
| **Charts (Admin dashboard)** | Recharts                     |
| ---                          | ---                          |

# **4\. Hướng Giải Quyết Chi Tiết**

## **4.1 SVG Seat Map**

Backend trả về JSON mô tả từng ghế (id, zone, status). Frontend map JSON này vào SVG template, tô màu theo zone (GA/SVIP/VIP), và đánh dấu trạng thái:

- available → có thể click
- taken/held → mờ, không click được
- selected → highlight viền

Khi SSE nhận event ghế thay đổi, chỉ update đúng ghế đó trong Zustand store - React chỉ re-render component ghế tương ứng, không re-render toàn bộ sơ đồ.

## **4.2 Seat Hold Countdown Timer**

Khi user xác nhận chọn ghế, backend trả về holdExpiresAt (timestamp hết hạn giữ chỗ, thường 10 phút). Frontend chạy interval đếm ngược và hiển thị. Khi hết giờ, xóa cart và hiện modal "Không còn thời gian giữ ghế - Bạn có muốn chọn lại không?" Backend sẽ tự động release ghế sau khi hết hold.

## **4.3 Xử lý lỗi UX**

Phân biệt rõ hai loại lỗi:

- Lỗi terminal (hết vé, session hết hạn): hiện modal, không cho retry, hướng user sang hành động khác
- Lỗi có thể retry (timeout mạng, 5xx server): hiện toast "Đang thử lại...", tự động retry tối đa 3 lần với exponential backoff

Đặc biệt với thanh toán timeout: không bao giờ re-enable nút thanh toán khi chưa biết kết quả - phải hiện banner "Giao dịch đang xử lý, vui lòng không đóng trang" và polling trạng thái đơn hàng.

## **4.4 Admin Dashboard**

TanStack Query tự động refetch thống kê doanh thu mỗi 30 giây. Form tạo/sửa concert validate chặt bằng zod trước khi submit (tránh giá âm, số lượng = 0, thời điểm mở bán đã qua). Dùng optimistic update khi hủy concert - hiện trạng thái "Đang hủy" ngay, rollback nếu API lỗi.

# **5\. Phối hợp với Backend**

Những điểm cần thống nhất với Backend API - nên được đặc tả rõ trong specs/ticket-purchase.md của nhóm:

| **Hạng mục**              | **Cần thống nhất**                                            |
| ------------------------- | ------------------------------------------------------------- |
| **Contract API seat map** | Định nghĩa rõ JSON schema trả về cho sơ đồ ghế                |
| ---                       | ---                                                           |
| **SSE endpoint format**   | Event name, payload structure khi ghế thay đổi                |
| ---                       | ---                                                           |
| **Idempotency key**       | Thống nhất tên header (Idempotency-Key) và cách backend xử lý |
| ---                       | ---                                                           |
| **JWT payload**           | Role claim có giá trị cố định: AUDIENCE / ORGANIZER / STAFF   |
| ---                       | ---                                                           |
| **Seat hold flow**        | Backend hold bao nhiêu phút, trả về holdExpiresAt như thế nào |
| ---                       | ---                                                           |

# **6\. Tóm tắt**

| **Câu hỏi**                                                      | **Giải pháp đã chọn**                                        |
| ---------------------------------------------------------------- | ------------------------------------------------------------ |
| **Seat map cập nhật realtime như thế nào mà không làm giật UI?** | SSE + React.memo + Zustand selector per-seat                 |
| ---                                                              | ---                                                          |
| **Luồng mua vé bảo vệ tính nhất quán từ phía client ra sao?**    | Disable button + idempotency key + retry logic phân loại lỗi |
| ---                                                              | ---                                                          |
| **Phân quyền UI được enforce như thế nào?**                      | JWT decode cho component + Next.js Middleware cho route      |
| ---                                                              | ---                                                          |