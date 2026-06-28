# Ticket Display Implementation

## Objective
Thêm hiển thị vé điện tử (E-Ticket) sau khi thanh toán thành công và bổ sung trang chi tiết lịch sử mua hàng cho phép xem lại các vé đã mua.

## Decisions Made
- Cập nhật `payment/callback/page.tsx` để lấy `orderId` từ URL parameters (hỗ trợ cả VNPAY `vnp_TxnRef` và MOMO `orderId`) và gọi API `GET /api/tickets/order/{orderId}` để lấy danh sách vé.
- Sử dụng component `ETicket` hiện có của frontend để hiển thị vé điện tử trực quan.
- Thêm trang `orders/[id]/page.tsx` để hiển thị chi tiết đơn hàng (lấy từ dữ liệu lịch sử) cùng với danh sách vé điện tử (gọi từ API).
- Thêm link "Xem chi tiết" vào danh sách đơn hàng trên trang `orders/page.tsx`.

## Files Created
- `.agents/conversation/40_ticket_display_implementation.md`
- `apps/frontend/src/app/orders/[id]/page.tsx`

## Files Modified
- `apps/frontend/src/app/payment/callback/page.tsx`
- `apps/frontend/src/app/orders/page.tsx`

## Dependencies
- Phụ thuộc vào Backend API `/api/tickets/order/{orderId}` (Đã tồn tại và hoạt động tốt).
- Phụ thuộc vào component `ETicket` (Đã tồn tại trong thư mục `src/components/ticket`).

## Remaining Work
- Không có. (Tính năng đã hoàn tất)

## Open Questions
- Không có.
