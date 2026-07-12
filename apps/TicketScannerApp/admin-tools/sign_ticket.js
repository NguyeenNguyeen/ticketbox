const fs = require('fs');
const crypto = require('crypto');

const privateKey = fs.readFileSync('./private.pem', 'utf8');

for (let i = 1; i <= 30; i++) {
    const id = `TICKET_${i.toString().padStart(3, '0')}`;
    const signer = crypto.createSign('SHA256');
    signer.update(id);
    const signature = signer.sign(privateKey, 'base64');

    const qrObject = { qr_code: id, signature: signature };
    console.log(`Vé ${id}:`);
    console.log(JSON.stringify(qrObject));
    console.log("-------------------");
}