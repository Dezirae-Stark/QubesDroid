/*
 * ChaCha20-Poly1305 AEAD (Authenticated Encryption with Associated Data)
 * RFC 8439 - ChaCha20 and Poly1305 for IETF Protocols
 *
 * Combines ChaCha20 stream cipher with Poly1305 MAC for authenticated encryption.
 * This is the ONLY encryption algorithm used in QubesDroid for volume encryption.
 *
 * Security: 256-bit key, quantum-resistant when used with Kyber-1024 for key exchange
 */

#ifndef CHACHA20POLY1305_H
#define CHACHA20POLY1305_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Key and nonce sizes */
#define CHACHA20POLY1305_KEYBYTES  32   /* 256-bit key (from Kyber shared secret) */
#define CHACHA20POLY1305_NONCEBYTES 12  /* 96-bit nonce */
#define CHACHA20POLY1305_ABYTES    16   /* Authentication tag size (Poly1305) */

/*
 * ChaCha20-Poly1305 AEAD Encryption
 *
 * Encrypts plaintext and authenticates both plaintext and additional data.
 *
 * Inputs:
 *   - plaintext: Data to encrypt
 *   - plaintext_len: Length of plaintext
 *   - aad: Additional authenticated data (can be NULL)
 *   - aad_len: Length of AAD (0 if NULL)
 *   - key: 32-byte encryption key (from Kyber-1024 shared secret)
 *   - nonce: 12-byte nonce (must be unique for each encryption with same key)
 *
 * Outputs:
 *   - ciphertext: Encrypted data (same length as plaintext)
 *   - tag: 16-byte authentication tag (appended to ciphertext)
 *
 * Returns: 0 on success, -1 on failure
 *
 * Note: ciphertext buffer must be at least plaintext_len bytes
 *       tag buffer must be at least 16 bytes
 */
int chacha20poly1305_encrypt(
    uint8_t *ciphertext,
    uint8_t *tag,
    const uint8_t *plaintext,
    size_t plaintext_len,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
);

/*
 * ChaCha20-Poly1305 AEAD Decryption
 *
 * Decrypts ciphertext and verifies authentication tag.
 *
 * Inputs:
 *   - ciphertext: Encrypted data
 *   - ciphertext_len: Length of ciphertext
 *   - tag: 16-byte authentication tag
 *   - aad: Additional authenticated data (must match encryption)
 *   - aad_len: Length of AAD
 *   - key: 32-byte encryption key (from Kyber-1024 shared secret)
 *   - nonce: 12-byte nonce (must match encryption)
 *
 * Outputs:
 *   - plaintext: Decrypted data (same length as ciphertext)
 *
 * Returns: 0 on success (tag verified), -1 on authentication failure
 *
 * CRITICAL: If this function returns -1, the ciphertext has been tampered with
 *           or the key/nonce is incorrect. DO NOT use the output plaintext.
 */
int chacha20poly1305_decrypt(
    uint8_t *plaintext,
    const uint8_t *ciphertext,
    size_t ciphertext_len,
    const uint8_t *tag,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
);

/*
 * ChaCha20-Poly1305 Encrypt-in-place
 *
 * Encrypts data in-place (overwrites input buffer).
 *
 * Inputs/Outputs:
 *   - data: Buffer containing plaintext (will be overwritten with ciphertext)
 *   - data_len: Length of data
 *
 * Inputs:
 *   - aad: Additional authenticated data
 *   - aad_len: Length of AAD
 *   - key: 32-byte encryption key
 *   - nonce: 12-byte nonce
 *
 * Outputs:
 *   - tag: 16-byte authentication tag
 *
 * Returns: 0 on success, -1 on failure
 */
int chacha20poly1305_encrypt_inplace(
    uint8_t *data,
    size_t data_len,
    const uint8_t *aad,
    size_t aad_len,
    uint8_t *tag,
    const uint8_t *key,
    const uint8_t *nonce
);

/*
 * ChaCha20-Poly1305 Decrypt-in-place
 *
 * Decrypts data in-place (overwrites input buffer).
 *
 * Inputs/Outputs:
 *   - data: Buffer containing ciphertext (will be overwritten with plaintext)
 *   - data_len: Length of data
 *
 * Inputs:
 *   - tag: 16-byte authentication tag
 *   - aad: Additional authenticated data
 *   - aad_len: Length of AAD
 *   - key: 32-byte encryption key
 *   - nonce: 12-byte nonce
 *
 * Returns: 0 on success (tag verified), -1 on authentication failure
 *
 * CRITICAL: If this function returns -1, do NOT use the data buffer contents.
 */
int chacha20poly1305_decrypt_inplace(
    uint8_t *data,
    size_t data_len,
    const uint8_t *tag,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
);

/*
 * Generate random nonce for ChaCha20-Poly1305
 *
 * Uses ChaCha20 RNG for cryptographically secure random nonce.
 *
 * Outputs:
 *   - nonce: 12-byte random nonce
 *
 * Returns: 0 on success, -1 on failure
 */
int chacha20poly1305_random_nonce(uint8_t *nonce);

#ifdef __cplusplus
}
#endif

#endif /* CHACHA20POLY1305_H */
