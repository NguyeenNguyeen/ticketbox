// Hàm giả lập chữ ký: ghép qr_code + một chuỗi bí mật rồi băm ra
// Trong thực tế Backend sẽ dùng Private Key để ký
export const generateMockTickets = () => {
    const tickets = [];
    const names = [
      'Nguyễn Văn An', 'Trần Thị Bình', 'Mai Huy Vũ', 'Lê Văn Cam', 'Phạm Thị Dung',
      'Trần Hoàng Nam', 'Lê Minh Tú', 'Phạm Thanh Thảo', 'Đặng Anh Tuấn', 'Nguyễn Phương Anh',
      'Vũ Hải Đăng', 'Lưu Diệc Phi', 'Trịnh Công Sơn', 'Nguyễn Thị Minh', 'Hoàng Anh Quân'
    ];
    
    for (let i = 1; i <= 30; i++) {
      const id = `TICKET_${i.toString().padStart(3, '0')}`;
      const name = names[i % names.length];
      const status = i <= 5 ? 'USED' : (i <= 7 ? 'REVOKED' : 'ACTIVE');
    
      const signature = btoa(`SIGN_${id}_SECRET_2026`); 
      
      tickets.push({
        qr_code: id,
        customer_name: name,
        status: status,
        is_synced: status === 'USED' ? 1 : 0,
        signature: signature
      });
    }
    return tickets;
  };