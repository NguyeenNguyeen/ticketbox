const { KJUR } = require('jsrsasign');
const fs = require('fs');

const publicKey = fs.readFileSync('public.pem', 'utf8');
const ticketData = "TICKET_001"; 
const signatureBase64 = "kM2lRqChFIhW45bxn78mvRMd56tKIJQ8FFMBZlH+UaL4pQqAP1muvsiFIlHBl57wxJAaF9mZbb+rHgKAxnz0Kedv4YKjpA45evJQvYYLQ+o8IAzwU+yahGlEH1k3mV4+lny+zdcrIjEV/nwcqUg7TEo8Jj+asqZE1BpERCDGV6JhHY52BrKxxss4JZ0tOPwPwrciQs8E/Q3+KiTyfMLU04ol5e7Sd7Z57baxKS8TaDqyJixZ/BGH7opkOtlMCVMhdUUT9lS1+Q9LALU2HsLSzbHVG9poBUb3No1tYZNWoW+CBX7nHE1FchIkHCnY5yzsDIN5xPRlUAey4VJlymURfQ==";

const sig = new KJUR.crypto.Signature({ alg: "SHA256withRSA", md: "sha256" });
sig.init(publicKey);
sig.updateString(ticketData);

const isValid = sig.verify(signatureBase64);
console.log("Kết quả verify sau khi sửa md:", isValid);