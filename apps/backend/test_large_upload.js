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
        
        // Create a 2MB file
        const largeFile = new Uint8Array(2 * 1024 * 1024);
        const formData = new FormData();
        formData.append("file", new Blob([largeFile], { type: "application/pdf" }), "large.pdf");
        
        const res3 = await fetch('http://localhost:8080/api/admin/concerts/1/upload-bio', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });
        
        console.log("POST upload status with large file:", res3.status);
        if (res3.status !== 200) {
            console.log("Response text:", await res3.text());
        }
        
    } catch (e) {
        console.error(e);
    }
}
test();
