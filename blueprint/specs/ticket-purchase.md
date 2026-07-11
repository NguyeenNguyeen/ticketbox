# Đặc tả: Luồng Mua vé và Thanh toán (Ticket Purchase)

## 1. Mô tả

Phân hệ Mua vé (Ticket Purchase) là trái tim của hệ thống TicketBox. Nó đảm nhận toàn bộ vòng đời của một giao dịch: từ khi khán giả chọn ghế, giữ chỗ, thanh toán, cho đến khi phát hành vé điện tử (E-ticket) và gửi email thông báo.

**Mục tiêu thiết kế của luồng này là giải quyết ba bài toán hóc búa nhất của một hệ thống bán vé quy mô lớn:**
- Ngăn chặn hoàn toàn việc bán lố vé (Overselling) trong điều kiện cạnh tranh và tranh chấp cực cao.
- Bảo vệ giao dịch tài chính, đảm bảo tính nguyên tử (Idempotent), không bao giờ trừ tiền hai lần (Double-charging).
- Đảm bảo hệ thống vẫn đứng vững, không bị hiệu ứng sập dây chuyền (Cascade Failure) khi cổng thanh toán ngoại vi bị nghẽn mạng.

## 2. Các tác nhân và Điều kiện tiên quyết

- **Tác nhân chính:** Khán giả (CUSTOMER).
- **Tác nhân phụ:** Cổng thanh toán (VNPAY/MoMo), Dịch vụ Email qua Provider Abstraction (Resend/Brevo/SMTP), Message Broker (RabbitMQ).
- **Điều kiện tiên quyết:** Khán giả phải được xác thực hợp lệ (có JWT). Sự kiện (Concert) phải đang ở trạng thái "Đang mở bán" (`ON_SALE`).

## 3. Luồng chính

Luồng mua vé thành công được diễn ra theo các bước kiến trúc sau, thể hiện sự phối hợp chặt chẽ giữa Frontend, Backend và Hạ tầng:

![[../image/Ticket Purchase Sequence Diagram.png]]

1. **Chọn vé (Frontend):** Khán giả tương tác với Sơ đồ ghế SVG trên giao diện. Frontend sử dụng trạng thái toàn cục (Zustand) để lưu giỏ hàng tạm thời và hiển thị giao diện thanh toán.
   
2. **Khởi tạo giao dịch (Frontend):** Khi khán giả bấm "Thanh toán", Frontend tự động sinh một mã duy nhất `Idempotency-Key` (UUID v4) và đính kèm vào Header của request API gửi lên Backend. Nút thanh toán lập tức bị vô hiệu hóa (disabled) để tránh double-click.

3. **Kiểm soát trùng lặp (Backend/Redis):** Khi nhận request, Backend sử dụng lệnh `SETNX` trên Redis với `Idempotency-Key` vừa nhận (TTL: 10 phút). Nếu key này đã tồn tại, Backend từ chối xử lý tiếp, trả về phản hồi giao dịch đang được thực hiện.

4. **Khóa chống vượt ngưỡng (Backend/Redis):** Backend thiết lập một Distributed Lock trên Redis theo ID của người dùng. Điều này ngăn chặn hành vi mở nhiều trình duyệt gửi request song song nhằm qua mặt rào cản giới hạn số lượng vé tối đa được phép mua.

5. **Kiểm tra và Khóa tồn kho (Database):** Backend mở một Transaction và truy vấn bảng `TicketCategory` với cơ chế Pessimistic Locking (`SELECT FOR UPDATE`). Nếu số vé thực tế (available quantity) đáp ứng đủ, Backend trừ thẳng tồn kho để chốt giữ chỗ. 

6. **Tạo đơn hàng (Database):** Một bản ghi `Order` được tạo với trạng thái ban đầu là `PENDING`. Sau đó, State Pattern tự động chuyển đơn hàng sang trạng thái `PAYING` trước khi hệ thống kết nối với cổng thanh toán.

7. **Thanh toán (External Integration):** Backend gọi `PaymentGatewayService` giao tiếp với VNPAY/MoMo. Toàn bộ bước này được bọc bởi Circuit Breaker và Bulkhead (Resilience4j) để cách ly lỗi mạng.

8. **Phát hành vé (Backend):** Khi thanh toán thành công, đơn hàng chuyển sang `COMPLETED`. Pattern Abstract Factory (`TicketFactory`) được gọi để sinh ra các bản ghi `Ticket` tương ứng với mã `qr_code` định danh duy nhất cho từng vé. Lệnh commit Transaction CSDL được thực thi.

9. **Bất đồng bộ Email (RabbitMQ):** Backend KHÔNG gọi API gửi email ngay lập tức. Thay vào đó, nó xuất bản (publish) một `EmailTaskMessage` chứa thông tin người dùng và danh sách ID vé vào Exchange của RabbitMQ. API mua vé ngay lập tức trả kết quả thành công cho Frontend để hiển thị vé trên màn hình.

10. **Xử lý nền & Gửi Email (Async Worker):**
    - Worker lắng nghe queue `email_queue` của RabbitMQ.
    - Truy xuất dữ liệu vé, tạo file PDF E-ticket đính kèm chứa mã QR.
    - Gọi dịch vụ Email theo cấu hình (Resend mặc định, hỗ trợ Brevo hoặc SMTP) để gửi email. 

## 4. Thiết kế vòng đời đơn hàng (Order Lifecycle)

Hệ thống sử dụng **State Pattern** để quản lý trạng thái đơn hàng một cách chặt chẽ, ngăn chặn các thao tác chuyển đổi trạng thái phi logic (ví dụ: đang CANCELLED lại nhảy sang COMPLETED).

- **PENDING:** Đơn hàng vừa được khởi tạo, đã chốt trừ tồn kho tại Database nhưng chưa kết nối ra mạng bên ngoài.
- **PAYING:** Đơn hàng đang được chờ kết quả phản hồi từ cổng thanh toán VNPAY/MoMo.
- **COMPLETED:** Giao dịch tài chính thành công, vé E-ticket đã được phát hành và thuộc sở hữu của khán giả.
- **CANCELLED:** Thanh toán thất bại, hết hạn thời gian giữ chỗ (10 phút) hoặc giao dịch bị từ chối do lỗi hệ thống/gateway. Tồn kho vé được tự động hoàn trả lại cho `TicketCategory`.


## 5. Quyết định Thiết kế Kiến trúc (Design Rationale)

### 5.1. Tại sao lại dùng Abstract Factory cho việc sinh vé?
- *Lý do:* Mỗi hạng vé (Ví dụ: VIP, GA, SVIP) có thể yêu cầu một quy trình in ấn mã QR, gán chính sách quà tặng (benefits) hoặc quy định vào cổng khác nhau. Việc áp dụng Abstract Factory Pattern (qua các implementation như `VIPTicketFactory`, `StandardTicketFactory`) giúp hệ thống đáp ứng nguyên tắc Open/Closed Principle (OCP). Nếu tương lai Ban tổ chức bán thêm loại vé "VVIP kèm ăn tối", lập trình viên chỉ cần thêm mới một lớp Factory tương ứng mà không phải sửa lại luồng xử lý mua vé cốt lõi dài hàng trăm dòng.

### 5.2. Tại sao tách việc gửi Email sang luồng Asynchronous (RabbitMQ)?
- *Lý do:* Việc gọi một API ngoại vi để render PDF rồi đính kèm gửi Email thường tốn khoảng 2-4 giây. Nếu đặt tác vụ này chạy đồng bộ (Synchronous) ngay bên trong luồng mua vé HTTP, request của khán giả sẽ bị treo 4 giây. Nhân với hàng chục nghìn khán giả, Connection Pool của Web Server (Tomcat) sẽ cạn kiệt lập tức, kéo sập toàn bộ hệ thống. Bằng cách ủy thác tác vụ này cho RabbitMQ, API mua vé chỉ mất chưa tới 1ms để đẩy message vào Queue và phản hồi ngay kết quả thành công cho người dùng.

### 5.3. Tại sao chọn Pessimistic Locking thay vì Message Queue cho việc chốt tồn kho vé?
- *Lý do:* Mặc dù có thể thiết kế một hệ thống "Ticket Queue" hoàn toàn bất đồng bộ bằng Kafka (theo mô hình phòng chờ ảo - Virtual Waiting Room), nhưng kiến trúc đó đòi hỏi Frontend phải xử lý WebSocket phức tạp, gây trải nghiệm người dùng khó lường khi phải chờ đợi kết quả mua vé trả về sau. Pessimistic Locking với PostgreSQL cung cấp sự đơn giản về mặt kiến trúc, đảm bảo tính nguyên tử tuyệt đối (ACID) và trả kết quả tức thì. Kết hợp với sức mạnh của Token Bucket (Rate Limiting) ở vòng ngoài bảo vệ số lượng request đập vào Database, PostgreSQL dư sức chịu tải một cách an toàn.

## 6. Kịch bản lỗi

Thiết kế kiến trúc luôn giả định mọi thành phần (Network, Database, 3rd-party) đều có thể thất bại.

- **Kịch bản: Cháy vé (Cạnh tranh tranh chấp cực lớn)**
  - Hàng nghìn người cùng mua 200 vé SVIP trong một giây. 
  - *Cứu vãn:* Pessimistic Locking buộc các yêu cầu xếp hàng tuần tự tại CSDL. Khi tồn kho rớt xuống 0, các giao dịch đang xếp hàng phía sau sẽ lập tức bị ném ngoại lệ (Exception), Transaction bị rollback nguyên vẹn và người dùng nhận thông báo lỗi 400 thân thiện: *"Rất tiếc, vé này vừa được mua mất ở mili-giây cuối"*. Không một vé nào bị bán âm.

- **Kịch bản: Cổng thanh toán sập / Nghẽn mạng**
  - VNPAY không phản hồi do đứt cáp hoặc bảo trì.
  - *Cứu vãn:* Bulkhead giới hạn tối đa 10 thread được phép chờ cổng thanh toán. Khi vách ngăn đầy hoặc tỷ lệ lỗi cao, Circuit Breaker ngắt mạch (Open). Các yêu cầu mua vé tiếp theo bị từ chối ngay lập tức (Graceful Degradation). Trạng thái đơn hàng chuyển sang `CANCELLED`, hoàn tồn kho vé và báo lỗi: *"Hệ thống thanh toán đang bảo trì, vui lòng thử lại sau"*. Việc này cứu cho toàn bộ Backend không bị treo cứng.

- **Kịch bản: Gửi Email thất bại**
  - Provider API (VD: Resend/SMTP) bị lỗi 500 hoặc hết quota giới hạn.
  - *Cứu vãn:* Email Worker trên RabbitMQ sẽ tự động retry (thử lại) tối đa 3 lần theo chiến lược Exponential Backoff (thời gian chờ tăng dần). Nếu vẫn thất bại, message được định tuyến sang Dead Letter Queue (DLQ). Lỗi được cô lập, quản trị viên có thể xem log DLQ, cấu hình chuyển đổi sang Provider khác (vd: từ Resend sang Gmail SMTP) và tái thực thi thủ công. Khán giả hoàn toàn không bị ảnh hưởng vì vẫn có thể xem mã QR trực tiếp trên website/app.

## 7.Ràng buộc

- **Giới hạn số vé tối đa (Max per user):** Mỗi tài khoản bị giới hạn tổng số lượng vé tối đa có thể mua cho một hạng ghế cụ thể của một sự kiện (Ví dụ: Tối đa 2 vé SVIP/người). Thuật toán phải cộng gộp cả số vé trong các đơn hàng `PENDING` (đang giữ chỗ) và `COMPLETED` để đối chiếu với quy định.
- **Tính hợp lệ của Sự kiện:** Chỉ được phép tạo giao dịch khi sự kiện có trạng thái logic (Effective Status) là `ON_SALE`. Mọi nỗ lực mua vé khi sự kiện đang ở trạng thái `UPCOMING` (chưa đến giờ mở bán), `ENDED` (đã diễn ra) hoặc `CANCELLED` (bị hủy) sẽ bị chối bỏ.

## 8. Tiêu chí chấp nhận

1. **Ngăn chặn Double-Charging:** Khi người dùng gửi 2 yêu cầu mua vé giống hệt nhau trong thời gian ngắn (cùng Idempotency Key), yêu cầu thứ 2 bị hệ thống từ chối mà không trừ tiền hay sinh thêm vé.
2. **Chống Overselling:** Số lượng vé phát hành thực tế tuyệt đối không bao giờ vượt qua `totalQuantity` của hạng vé đó, kể cả dưới điều kiện tải cao.
3. **Xử lý ngắt mạch (Circuit Breaker):** Khi cổng thanh toán phản hồi chậm vượt ngưỡng cấu hình, hệ thống phải tự động ngắt mạch và từ chối các yêu cầu thanh toán mới, đảm bảo các chức năng khác không bị treo.
4. **Email bất đồng bộ:** Việc mua vé thành công phải kích hoạt thông điệp gửi Email vào RabbitMQ thành công, thời gian API phản hồi cho client không bị nghẽn bởi thời gian chờ email.
5. **Giới hạn số vé:** Khán giả không thể mua vượt quá `maxPerUser` quy định cho mỗi hạng vé.
