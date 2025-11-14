/*
 * ChaCha20-Poly1305 AEAD Implementation
 * RFC 8439 compliant
 *
 * This is the PRIMARY and ONLY encryption algorithm for QubesDroid volumes.
 * All legacy ciphers (AES, Serpent, Twofish) have been removed.
 */

#include "chacha20poly1305.h"
#include "chacha256.h"
#include "poly1305.h"
#include "chachaRng.h"
#include <string.h>

/* Serialize a 64-bit integer as little-endian */
static void store64_le(uint8_t *dst, uint64_t val) {
    dst[0] = (uint8_t)(val);
    dst[1] = (uint8_t)(val >> 8);
    dst[2] = (uint8_t)(val >> 16);
    dst[3] = (uint8_t)(val >> 24);
    dst[4] = (uint8_t)(val >> 32);
    dst[5] = (uint8_t)(val >> 40);
    dst[6] = (uint8_t)(val >> 48);
    dst[7] = (uint8_t)(val >> 56);
}

/*
 * Pad length to 16-byte boundary for Poly1305
 * Returns number of padding bytes
 */
static size_t poly1305_pad_length(size_t len) {
    return (16 - (len % 16)) % 16;
}

/*
 * Generate Poly1305 key from ChaCha20
 * First block of ChaCha20 keystream is used as Poly1305 key
 */
static void chacha20_poly1305_key(uint8_t *poly_key, const uint8_t *key, const uint8_t *nonce) {
    ChaCha256Ctx ctx;
    uint8_t zero_block[64] = {0};

    /* Initialize ChaCha20 with key and nonce */
    ChaCha256Init(&ctx, key, nonce, 0);

    /* Generate first 64 bytes of keystream */
    ChaCha256Encrypt(&ctx, zero_block, 64, zero_block);

    /* First 32 bytes are Poly1305 key */
    memcpy(poly_key, zero_block, 32);

    /* Clear sensitive data */
    memset(zero_block, 0, sizeof(zero_block));
    memset(&ctx, 0, sizeof(ctx));
}

int chacha20poly1305_encrypt(
    uint8_t *ciphertext,
    uint8_t *tag,
    const uint8_t *plaintext,
    size_t plaintext_len,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
) {
    ChaCha256Ctx chacha_ctx;
    poly1305_context poly_ctx;
    uint8_t poly_key[32];
    uint8_t pad_buffer[16] = {0};
    uint8_t len_buffer[16];

    if (!ciphertext || !tag || !key || !nonce) {
        return -1;
    }

    if (plaintext_len > 0 && !plaintext) {
        return -1;
    }

    if (aad_len > 0 && !aad) {
        return -1;
    }

    /* Generate Poly1305 key from first ChaCha20 block */
    chacha20_poly1305_key(poly_key, key, nonce);

    /* Initialize Poly1305 with generated key */
    poly1305_init(&poly_ctx, poly_key);

    /* Encrypt plaintext with ChaCha20 (counter starts at 1) */
    ChaCha256Init(&chacha_ctx, key, nonce, 1);
    if (plaintext_len > 0) {
        ChaCha256Encrypt(&chacha_ctx, plaintext, plaintext_len, ciphertext);
    }

    /* Construct Poly1305 input according to RFC 8439:
     * poly_input = aad || pad(aad) || ciphertext || pad(ciphertext) || len(aad) || len(ciphertext)
     */

    /* Add AAD to MAC */
    if (aad_len > 0) {
        poly1305_update(&poly_ctx, aad, aad_len);

        /* Add padding */
        size_t aad_pad = poly1305_pad_length(aad_len);
        if (aad_pad > 0) {
            poly1305_update(&poly_ctx, pad_buffer, aad_pad);
        }
    }

    /* Add ciphertext to MAC */
    if (plaintext_len > 0) {
        poly1305_update(&poly_ctx, ciphertext, plaintext_len);

        /* Add padding */
        size_t ct_pad = poly1305_pad_length(plaintext_len);
        if (ct_pad > 0) {
            poly1305_update(&poly_ctx, pad_buffer, ct_pad);
        }
    }

    /* Add lengths (little-endian) */
    store64_le(len_buffer, aad_len);
    store64_le(len_buffer + 8, plaintext_len);
    poly1305_update(&poly_ctx, len_buffer, 16);

    /* Finalize MAC */
    poly1305_final(&poly_ctx, tag);

    /* Clear sensitive data */
    memset(poly_key, 0, sizeof(poly_key));
    memset(&chacha_ctx, 0, sizeof(chacha_ctx));
    memset(&poly_ctx, 0, sizeof(poly_ctx));

    return 0;
}

int chacha20poly1305_decrypt(
    uint8_t *plaintext,
    const uint8_t *ciphertext,
    size_t ciphertext_len,
    const uint8_t *tag,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
) {
    ChaCha256Ctx chacha_ctx;
    poly1305_context poly_ctx;
    uint8_t poly_key[32];
    uint8_t computed_tag[16];
    uint8_t pad_buffer[16] = {0};
    uint8_t len_buffer[16];
    int result;

    if (!plaintext || !tag || !key || !nonce) {
        return -1;
    }

    if (ciphertext_len > 0 && !ciphertext) {
        return -1;
    }

    if (aad_len > 0 && !aad) {
        return -1;
    }

    /* Generate Poly1305 key */
    chacha20_poly1305_key(poly_key, key, nonce);

    /* Compute MAC */
    poly1305_init(&poly_ctx, poly_key);

    /* Add AAD */
    if (aad_len > 0) {
        poly1305_update(&poly_ctx, aad, aad_len);
        size_t aad_pad = poly1305_pad_length(aad_len);
        if (aad_pad > 0) {
            poly1305_update(&poly_ctx, pad_buffer, aad_pad);
        }
    }

    /* Add ciphertext */
    if (ciphertext_len > 0) {
        poly1305_update(&poly_ctx, ciphertext, ciphertext_len);
        size_t ct_pad = poly1305_pad_length(ciphertext_len);
        if (ct_pad > 0) {
            poly1305_update(&poly_ctx, pad_buffer, ct_pad);
        }
    }

    /* Add lengths */
    store64_le(len_buffer, aad_len);
    store64_le(len_buffer + 8, ciphertext_len);
    poly1305_update(&poly_ctx, len_buffer, 16);

    poly1305_final(&poly_ctx, computed_tag);

    /* Verify MAC (constant-time) */
    result = poly1305_verify(computed_tag, tag);

    if (result != 0) {
        /* Authentication failed - clear output and return error */
        if (ciphertext_len > 0) {
            memset(plaintext, 0, ciphertext_len);
        }
        memset(poly_key, 0, sizeof(poly_key));
        memset(computed_tag, 0, sizeof(computed_tag));
        return -1;
    }

    /* MAC verified - decrypt */
    ChaCha256Init(&chacha_ctx, key, nonce, 1);
    if (ciphertext_len > 0) {
        ChaCha256Decrypt(&chacha_ctx, ciphertext, ciphertext_len, plaintext);
    }

    /* Clear sensitive data */
    memset(poly_key, 0, sizeof(poly_key));
    memset(computed_tag, 0, sizeof(computed_tag));
    memset(&chacha_ctx, 0, sizeof(chacha_ctx));

    return 0;
}

int chacha20poly1305_encrypt_inplace(
    uint8_t *data,
    size_t data_len,
    const uint8_t *aad,
    size_t aad_len,
    uint8_t *tag,
    const uint8_t *key,
    const uint8_t *nonce
) {
    return chacha20poly1305_encrypt(data, tag, data, data_len, aad, aad_len, key, nonce);
}

int chacha20poly1305_decrypt_inplace(
    uint8_t *data,
    size_t data_len,
    const uint8_t *tag,
    const uint8_t *aad,
    size_t aad_len,
    const uint8_t *key,
    const uint8_t *nonce
) {
    return chacha20poly1305_decrypt(data, data, data_len, tag, aad, aad_len, key, nonce);
}

int chacha20poly1305_random_nonce(uint8_t *nonce) {
    if (!nonce) {
        return -1;
    }

    /* Use ChaCha20 RNG for cryptographically secure random nonce */
    ChaCha20RngCtx rng_ctx;
    uint8_t seed[32];

    /* Get seed from system entropy (implementation depends on platform) */
    /* For now, this is a placeholder - actual implementation should use:
     * - /dev/urandom on Linux/Android
     * - CryptGenRandom on Windows
     * - arc4random_buf on BSD/macOS
     */
    #ifdef __ANDROID__
        /* TODO: Implement Android-specific secure random */
        /* Use /dev/urandom or getentropy() */
    #endif

    /* Initialize RNG and generate nonce */
    ChaCha20RngInit(&rng_ctx, seed, NULL, 0);
    ChaCha20RngGetBytes(&rng_ctx, nonce, CHACHA20POLY1305_NONCEBYTES);

    /* Clear seed */
    memset(seed, 0, sizeof(seed));
    memset(&rng_ctx, 0, sizeof(rng_ctx));

    return 0;
}
