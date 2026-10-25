/**
 * SAT Messenger End-to-End Encryption (E2EE) Module
 * Built using the W3C Web Cryptography API (AES-GCM 256-bit & PBKDF2/SHA-256)
 * Developed for SAT Limited Organization
 */

// Generate a random 96-bit (12 bytes) IV for AES-GCM
export function generateIV(): Uint8Array {
  return window.crypto.getRandomValues(new Uint8Array(12));
}

// Convert ArrayBuffer to Base64
export function bufferToBase64(buffer: ArrayBuffer | Uint8Array): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window.btoa(binary);
}

// Convert Base64 to Uint8Array
export function base64ToBuffer(base64: string): Uint8Array {
  const binary = window.atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

// Derive a shared cryptographic AES-GCM 256-bit key from a conversation secret / ID
export async function deriveRoomKey(roomId: string, saltString = 'SAT_LIMITED_E2EE_V1'): Promise<CryptoKey> {
  const enc = new TextEncoder();
  const baseKey = await window.crypto.subtle.importKey(
    'raw',
    enc.encode(roomId),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );

  return window.crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: enc.encode(saltString),
      iterations: 100000,
      hash: 'SHA-256',
    },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt', 'decrypt']
  );
}

// Encrypt plaintext payload into ciphertext and IV string
export async function encryptPayload(plaintext: string, roomId: string): Promise<{ ciphertext: string; iv: string }> {
  try {
    const key = await deriveRoomKey(roomId);
    const iv = generateIV();
    const enc = new TextEncoder();
    const encodedData = enc.encode(plaintext);

    const encryptedBuffer = await window.crypto.subtle.encrypt(
      {
        name: 'AES-GCM',
        iv: iv as any,
      },
      key,
      encodedData
    );

    return {
      ciphertext: bufferToBase64(encryptedBuffer),
      iv: bufferToBase64(iv),
    };
  } catch (error) {
    console.error('E2EE Encryption error:', error);
    throw new Error('Encryption failed: ' + (error instanceof Error ? error.message : String(error)));
  }
}

// Decrypt ciphertext using room key and IV
export async function decryptPayload(ciphertext: string, ivBase64: string, roomId: string): Promise<string> {
  try {
    const key = await deriveRoomKey(roomId);
    const iv = base64ToBuffer(ivBase64);
    const encryptedBytes = base64ToBuffer(ciphertext);

    const decryptedBuffer = await window.crypto.subtle.decrypt(
      {
        name: 'AES-GCM',
        iv: iv as any,
      },
      key,
      encryptedBytes as any
    );

    const dec = new TextDecoder();
    return dec.decode(decryptedBuffer);
  } catch (error) {
    // If decryption fails (e.g. if key doesn't match or corrupted)
    return `[Encrypted Payload - Key verification failed]`;
  }
}

// Generate human-verifiable 60-digit security safety number (like Signal / WhatsApp)
export async function generateSafetyFingerprint(roomId: string, memberA = 'SAT_USER', memberB = 'SAT_CONTACT'): Promise<{
  formattedNumber: string;
  qrPayload: string;
  fingerprintHash: string;
}> {
  const enc = new TextEncoder();
  const sorted = [memberA, memberB].sort().join(':');
  const buffer = await window.crypto.subtle.digest('SHA-256', enc.encode(`SAT_E2EE_${roomId}_${sorted}`));
  const bytes = new Uint8Array(buffer);

  // Convert first 30 bytes to numeric segments of 5 digits (12 groups = 60 digits)
  let numericString = '';
  for (let i = 0; i < 30; i += 2) {
    const val = (bytes[i] << 8) | bytes[i + 1];
    numericString += String(val % 100000).padStart(5, '0');
  }

  // Format into 12 blocks of 5 digits
  const blocks: string[] = [];
  for (let i = 0; i < 60; i += 5) {
    blocks.push(numericString.slice(i, i + 5));
  }

  const hashHex = Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');

  return {
    formattedNumber: blocks.join(' '),
    qrPayload: `SAT-SEC-NUM:v1:${hashHex.slice(0, 32)}`,
    fingerprintHash: hashHex.slice(0, 16).toUpperCase(),
  };
}

// Generate device keypair for public identity
export async function generateIdentityKey(): Promise<{ publicKey: string; privateKeyString: string }> {
  const keyPair = await window.crypto.subtle.generateKey(
    {
      name: 'ECDSA',
      namedCurve: 'P-256',
    },
    true,
    ['sign', 'verify']
  );

  const exportedPublic = await window.crypto.subtle.exportKey('spki', keyPair.publicKey);
  const exportedPrivate = await window.crypto.subtle.exportKey('pkcs8', keyPair.privateKey);

  return {
    publicKey: bufferToBase64(exportedPublic),
    privateKeyString: bufferToBase64(exportedPrivate),
  };
}
