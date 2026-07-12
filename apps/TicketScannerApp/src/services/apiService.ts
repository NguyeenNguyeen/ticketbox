// ĐỊA CHỈ API CỦA HỆ THỐNG
const API_BASE_URL = "http://localhost:3000";
// ĐỊNH DẠNG DỮ LIỆU GỬI LÊN KHỚP VỚI 'SyncTicketRequest' TRONG BACKEND JAVA
export interface SyncTicketPayload {
  ticketId: string; 
  status: string;     // 'USED'
  scannedAt: string;  // 'scannedAt' theo quy ước camelCase
}

export const apiService = {
  /**
   * Đẩy danh sách vé đã quét lên Backend
   * @param tickets Danh sách các vé cần đồng bộ
   * @param token JWT Token lấy được sau khi đăng nhập thành công
   */
  syncTicketsToServer: async (tickets: SyncTicketPayload[], token: string): Promise<boolean> => {
    try {
      console.log(`[API] Đang gửi ${tickets.length} vé lên server...`);
      
      const response = await fetch("http://localhost:3000/api/sync", {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // ĐÍNH KÈM TOKEN BẢO MẬT ĐỂ BACKEND XÁC THỰC QUYỀN SOÁT VÉ
          'Authorization': `Bearer ${token}` 
        },
        body: JSON.stringify(tickets), // Backend nhận List nên gửi trực tiếp mảng
      });

      if (!response.ok) {
        throw new Error('Server từ chối yêu cầu (Lỗi xác thực hoặc lỗi dữ liệu)');
      }

      return true;
      
    } catch (error) {
      console.error("❌ API ERROR:", error); // Log lỗi chi tiết ra xem nó đang bị gì
      return false; // Trả về false để App biết là CHƯA đồng bộ thành công
    }
  }
};





// // ĐỊA CHỈ API CỦA HỆ THỐNG
// const API_BASE_URL = "http://10.0.150.60:3000";
// // ĐỊNH DẠNG DỮ LIỆU GỬI LÊN KHỚP VỚI 'SyncTicketRequest' TRONG BACKEND JAVA
// export interface SyncTicketPayload {
//   ticketId: string;   // Chú ý: Backend Java thường dùng camelCase là ticketId
//   status: string;     // 'USED'
//   scannedAt: string;  // 'scannedAt' theo quy ước camelCase
// }

// export const apiService = {
//   /**
//    * Đẩy danh sách vé đã quét lên Backend
//    * @param tickets Danh sách các vé cần đồng bộ
//    * @param token JWT Token lấy được sau khi đăng nhập thành công
//    */
//   syncTicketsToServer: async (tickets: SyncTicketPayload[], token: string): Promise<boolean> => {
//     try {
//       console.log(`[API] Đang gửi ${tickets.length} vé lên server...`);
      
//       const response = await fetch(`${API_BASE_URL}/api/sync`, {
//         method: 'POST',
//         headers: {
//           'Content-Type': 'application/json',
//           // ĐÍNH KÈM TOKEN BẢO MẬT ĐỂ BACKEND XÁC THỰC QUYỀN SOÁT VÉ
//           'Authorization': `Bearer ${token}` 
//         },
//         body: JSON.stringify(tickets), // Backend nhận List nên mình gửi trực tiếp mảng
//       });

//       if (!response.ok) {
//         throw new Error('Server từ chối yêu cầu (Lỗi xác thực hoặc lỗi dữ liệu)');
//       }

//       return true;
      
//     } catch (error) {
//       console.warn("[API] Lỗi kết nối server, đang chạy giả lập test offline...");
      
//       // Giả lập thành công để Vũ test giao diện mượt mà
//       await new Promise((resolve) => setTimeout(resolve, 1500));
//       return true; 
//     }
//   }
// };
