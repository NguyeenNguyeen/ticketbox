import forge from 'node-forge';

export const PUBLIC_KEY = `-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2qPy/oqWkoZjjG5Ph9oU
PGGw1FxvEQh3Ei0KdsLwdF1el4SSE87j97GOi23zFgNY9Tg+D/c19VDVXa4B2k9N
sHbvXnZnIXHt+GD4DMcXWj6x0Fx1H0ELUWhsf80V9FVkWR2qtPmrvs8L7wBine2+
zurDh1T7gOZRmM7bbqttb0UOVhqC5GB2m1oCjrKPhUoRHRzkN8795cTXOma6eoXa
4bM3jYp594rSALwTyfD3uMVSY0T8Mlkdx/QHUjc5JkxF8v1xG1M92VJkyxn+4x5Z
VBj3TFbq/ctG9QhLrbdejEJxw7sV3pBe25vJxN3KLATPEiRtLd2hYCfpt0JOWMQo
qQIDAQAB
-----END PUBLIC KEY-----`.trim();

export const verifyTicketSignature = (qrCode: string, signatureFromQR: string): boolean => {
  try {
    let base64Sig = signatureFromQR.replace(/-/g, '+').replace(/_/g, '/');
    while (base64Sig.length % 4 !== 0) base64Sig += '=';

    const publicKey = forge.pki.publicKeyFromPem(PUBLIC_KEY);
    const signatureBytes = forge.util.decode64(base64Sig);

    const md = forge.md.sha256.create();
    md.update(qrCode, 'utf8');

    // Sửa lại dòng này:
    // forge tự động nhận diện thuật toán nếu bạn cung trí md.digest()
    return publicKey.verify(md.digest().getBytes(), signatureBytes);
  } catch (error) {
    console.error("Lỗi xác thực:", error);
    return false;
  }
};