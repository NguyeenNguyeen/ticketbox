const fs = require('fs');

async function test() {
    try {
        const res1 = await fetch('http://localhost:8080/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin1', password: 'password' })
        });
        const data1 = await res1.json();
        const token = data1.accessToken;
        console.log("Got token");
        
        const res2 = await fetch('http://localhost:8080/api/admin/concerts/1', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({
                title: "Anh Trai Say Hi - Live Concert",
                description: "Concert quy tụ 30 anh trai đình đám nhất hiện nay.",
                venue: "Sân vận động Mỹ Đình, Hà Nội",
                date: "2026-12-20",
                showTime: "19:00",
                saleStartTime: "2026-06-01"
            })
        });
        
        console.log("PUT status:", res2.status);
        if (res2.status === 403) {
            console.log("PUT error:", await res2.text());
        }
        
        const formData = new FormData();
        formData.append("file", new Blob(["test"], { type: "application/pdf" }), "test.pdf");
        
        const res3 = await fetch('http://localhost:8080/api/admin/concerts/1/upload-bio', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });
        
        console.log("POST upload status:", res3.status);
        if (res3.status === 403) {
            console.log("POST upload error:", await res3.text());
        }
        
    } catch (e) {
        console.error(e);
    }
}
test();
