const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

// Đọc Private Key (đảm bảo file nằm ở thư mục admin-tools hoặc cùng cấp với script)
const privateKey = fs.readFileSync('private_new.pem', 'utf8');

const names = [
    'Nguyễn Văn An', 'Trần Thị Bình', 'Mai Huy Vũ', 'Lê Văn Cam', 'Phạm Thị Dung',
    'Trần Hoàng Nam', 'Lê Minh Tú', 'Phạm Thanh Thảo', 'Đặng Anh Tuấn', 'Nguyễn Phương Anh',
    'Vũ Hải Đăng', 'Lưu Diệc Phi', 'Trịnh Công Sơn', 'Nguyễn Thị Minh', 'Hoàng Anh Quân'
];
const tickets = [];

for (let i = 1; i <= 30; i++) {
    const id = `TICKET_${i.toString().padStart(3, '0')}`;
    
    // Ký số thực thụ
    const signer = crypto.createSign('SHA256');
    signer.update(id);
    const signature = signer.sign(privateKey, 'base64')
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, ''); // URL-safe base64

    tickets.push({
        qr_code: id,
        customer_name: names[i % names.length],
        status: 'ACTIVE',
        is_synced: 0,
        signature: signature
    });
}

// Ghi vào file
fs.writeFileSync('./tickets_data.json', JSON.stringify(tickets, null, 2));
console.log('✅ Đã tạo xong 30 vé với chữ ký chuẩn!');