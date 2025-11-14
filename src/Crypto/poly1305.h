/*
 * Poly1305 - Authenticator for ChaCha20-Poly1305 AEAD
 * RFC 8439 - ChaCha20 and Poly1305 for IETF Protocols
 *
 * Poly1305 is a cryptographic message authentication code (MAC)
 * created by Daniel J. Bernstein.
 *
 * Used in combination with ChaCha20 for authenticated encryption.
 */

#ifndef POLY1305_H
#define POLY1305_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Poly1305 key and tag sizes */
#define POLY1305_KEYLEN 32   /* 256-bit key */
#define POLY1305_TAGLEN 16   /* 128-bit authentication tag */

/* Poly1305 context for streaming operations */
typedef struct {
    uint32_t r[5];           /* Clamped key r */
    uint32_t h[5];           /* Accumulator */
    uint32_t pad[4];         /* Key s (for final addition) */
    size_t leftover;         /* Bytes in buffer */
    uint8_t buffer[16];      /* Input buffer */
    uint8_t final;           /* Set to 1 after final() */
} poly1305_context;

/*
 * Poly1305 one-shot MAC computation
 *
 * Inputs:
 *   - msg: Message to authenticate
 *   - len: Message length in bytes
 *   - key: 32-byte key (usually from ChaCha20)
 *
 * Outputs:
 *   - mac: 16-byte authentication tag
 */
void poly1305_auth(uint8_t *mac, const uint8_t *msg, size_t len, const uint8_t *key);

/*
 * Initialize Poly1305 context for streaming
 *
 * Inputs:
 *   - ctx: Poly1305 context to initialize
 *   - key: 32-byte key
 */
void poly1305_init(poly1305_context *ctx, const uint8_t *key);

/*
 * Update Poly1305 context with message data
 *
 * Can be called multiple times for streaming operation.
 *
 * Inputs:
 *   - ctx: Poly1305 context
 *   - msg: Message data
 *   - len: Data length
 */
void poly1305_update(poly1305_context *ctx, const uint8_t *msg, size_t len);

/*
 * Finalize Poly1305 and output authentication tag
 *
 * Inputs:
 *   - ctx: Poly1305 context
 *
 * Outputs:
 *   - mac: 16-byte authentication tag
 */
void poly1305_final(poly1305_context *ctx, uint8_t *mac);

/*
 * Verify Poly1305 MAC
 *
 * Constant-time comparison to prevent timing attacks.
 *
 * Inputs:
 *   - mac1: First MAC (16 bytes)
 *   - mac2: Second MAC (16 bytes)
 *
 * Returns: 0 if MACs match, -1 otherwise
 */
int poly1305_verify(const uint8_t *mac1, const uint8_t *mac2);

#ifdef __cplusplus
}
#endif

#endif /* POLY1305_H */
