const express = require('express');
const fs = require('fs');
const app = express();
app.use(express.json());

app.post('/api/sync', (req, res) => {
    console.log("📨 Nhận được request từ mobile!");
    console.log("Dữ liệu nhận được:", req.body); // Xem thử nó có nhận được gì không
    const newTickets = req.body;
    console.log("📥 Đã nhận được dữ liệu, số lượng:", newTickets.length);
    
    // Ghi dữ liệu vào file
    fs.appendFileSync('data.json', JSON.stringify(newTickets) + '\n');
    
    res.status(200).json({ status: "success" });
});

app.listen(3000, () => console.log('✅ Server đang lắng nghe tại cổng 3000'));