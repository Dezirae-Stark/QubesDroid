/*
 * Poly1305 - Authenticator Implementation
 * Based on RFC 8439 and Daniel J. Bernstein's original specification
 *
 * Optimized for ARM and general-purpose CPUs
 */

#include "poly1305.h"
#include <string.h>

/* Constant-time memory comparison */
static int poly1305_constant_time_compare(const uint8_t *a, const uint8_t *b, size_t len) {
    uint8_t diff = 0;
    for (size_t i = 0; i < len; i++) {
        diff |= (a[i] ^ b[i]);
    }
    return diff == 0 ? 0 : -1;
}

/* Convert little-endian bytes to 32-bit word */
static uint32_t load32_le(const uint8_t *src) {
    return (uint32_t)src[0]
        | ((uint32_t)src[1] << 8)
        | ((uint32_t)src[2] << 16)
        | ((uint32_t)src[3] << 24);
}

/* Store 32-bit word as little-endian bytes */
static void store32_le(uint8_t *dst, uint32_t val) {
    dst[0] = (uint8_t)(val);
    dst[1] = (uint8_t)(val >> 8);
    dst[2] = (uint8_t)(val >> 16);
    dst[3] = (uint8_t)(val >> 24);
}

void poly1305_init(poly1305_context *ctx, const uint8_t *key) {
    /* Clear context */
    memset(ctx, 0, sizeof(poly1305_context));

    /* Clamp r (first 16 bytes of key) */
    uint32_t t0 = load32_le(key + 0);
    uint32_t t1 = load32_le(key + 4);
    uint32_t t2 = load32_le(key + 8);
    uint32_t t3 = load32_le(key + 12);

    /* Clamping: clear top 4 bits of bytes 3, 7, 11, 15 */
    /* and bottom 2 bits of bytes 4, 8, 12 */
    ctx->r[0] = t0 & 0x0fffffff;
    ctx->r[1] = ((t0 >> 28) | (t1 << 4)) & 0x0ffffffc;
    ctx->r[2] = ((t1 >> 24) | (t2 << 8)) & 0x0ffffffc;
    ctx->r[3] = ((t2 >> 20) | (t3 << 12)) & 0x0ffffffc;
    ctx->r[4] = (t3 >> 16) & 0x0ffffffc;

    /* Store s (second 16 bytes of key) for final addition */
    ctx->pad[0] = load32_le(key + 16);
    ctx->pad[1] = load32_le(key + 20);
    ctx->pad[2] = load32_le(key + 24);
    ctx->pad[3] = load32_le(key + 28);

    /* Initialize accumulator h to zero */
    ctx->h[0] = 0;
    ctx->h[1] = 0;
    ctx->h[2] = 0;
    ctx->h[3] = 0;
    ctx->h[4] = 0;

    ctx->leftover = 0;
    ctx->final = 0;
}

static void poly1305_blocks(poly1305_context *ctx, const uint8_t *msg, size_t len, uint32_t hibit) {
    uint32_t r0 = ctx->r[0];
    uint32_t r1 = ctx->r[1];
    uint32_t r2 = ctx->r[2];
    uint32_t r3 = ctx->r[3];
    uint32_t r4 = ctx->r[4];

    uint32_t h0 = ctx->h[0];
    uint32_t h1 = ctx->h[1];
    uint32_t h2 = ctx->h[2];
    uint32_t h3 = ctx->h[3];
    uint32_t h4 = ctx->h[4];

    uint32_t s1 = r1 * 5;
    uint32_t s2 = r2 * 5;
    uint32_t s3 = r3 * 5;
    uint32_t s4 = r4 * 5;

    while (len >= 16) {
        /* h += m[i] */
        uint32_t t0 = load32_le(msg + 0);
        uint32_t t1 = load32_le(msg + 4);
        uint32_t t2 = load32_le(msg + 8);
        uint32_t t3 = load32_le(msg + 12);

        h0 += t0 & 0x0fffffff;
        h1 += ((t0 >> 28) | (t1 << 4)) & 0x0fffffff;
        h2 += ((t1 >> 24) | (t2 << 8)) & 0x0fffffff;
        h3 += ((t2 >> 20) | (t3 << 12)) & 0x0fffffff;
        h4 += (t3 >> 16) | hibit;

        /* h *= r (modulo 2^130 - 5) */
        uint64_t d0 = (uint64_t)h0 * r0 + (uint64_t)h1 * s4 + (uint64_t)h2 * s3 + (uint64_t)h3 * s2 + (uint64_t)h4 * s1;
        uint64_t d1 = (uint64_t)h0 * r1 + (uint64_t)h1 * r0 + (uint64_t)h2 * s4 + (uint64_t)h3 * s3 + (uint64_t)h4 * s2;
        uint64_t d2 = (uint64_t)h0 * r2 + (uint64_t)h1 * r1 + (uint64_t)h2 * r0 + (uint64_t)h3 * s4 + (uint64_t)h4 * s3;
        uint64_t d3 = (uint64_t)h0 * r3 + (uint64_t)h1 * r2 + (uint64_t)h2 * r1 + (uint64_t)h3 * r0 + (uint64_t)h4 * s4;
        uint64_t d4 = (uint64_t)h0 * r4 + (uint64_t)h1 * r3 + (uint64_t)h2 * r2 + (uint64_t)h3 * r1 + (uint64_t)h4 * r0;

        /* Partial reduction */
        uint32_t c;
        c = (uint32_t)(d0 >> 28); h0 = (uint32_t)d0 & 0x0fffffff; d1 += c;
        c = (uint32_t)(d1 >> 28); h1 = (uint32_t)d1 & 0x0fffffff; d2 += c;
        c = (uint32_t)(d2 >> 28); h2 = (uint32_t)d2 & 0x0fffffff; d3 += c;
        c = (uint32_t)(d3 >> 28); h3 = (uint32_t)d3 & 0x0fffffff; d4 += c;
        c = (uint32_t)(d4 >> 28); h4 = (uint32_t)d4 & 0x0fffffff;
        h0 += c * 5; c = h0 >> 28; h0 &= 0x0fffffff; h1 += c;

        msg += 16;
        len -= 16;
    }

    ctx->h[0] = h0;
    ctx->h[1] = h1;
    ctx->h[2] = h2;
    ctx->h[3] = h3;
    ctx->h[4] = h4;
}

void poly1305_update(poly1305_context *ctx, const uint8_t *msg, size_t len) {
    if (ctx->leftover) {
        size_t want = 16 - ctx->leftover;
        if (want > len) {
            want = len;
        }
        memcpy(ctx->buffer + ctx->leftover, msg, want);
        len -= want;
        msg += want;
        ctx->leftover += want;
        if (ctx->leftover < 16) {
            return;
        }
        poly1305_blocks(ctx, ctx->buffer, 16, 1 << 24);
        ctx->leftover = 0;
    }

    if (len >= 16) {
        size_t want = len & ~15;
        poly1305_blocks(ctx, msg, want, 1 << 24);
        msg += want;
        len -= want;
    }

    if (len) {
        memcpy(ctx->buffer + ctx->leftover, msg, len);
        ctx->leftover += len;
    }
}

void poly1305_final(poly1305_context *ctx, uint8_t *mac) {
    uint32_t h0, h1, h2, h3, h4, c;
    uint32_t g0, g1, g2, g3, g4;
    uint64_t f;
    uint32_t mask;

    /* Process remaining bytes */
    if (ctx->leftover) {
        size_t i = ctx->leftover;
        ctx->buffer[i++] = 1;
        for (; i < 16; i++) {
            ctx->buffer[i] = 0;
        }
        poly1305_blocks(ctx, ctx->buffer, 16, 0);
    }

    /* Fully reduce h modulo 2^130 - 5 */
    h0 = ctx->h[0];
    h1 = ctx->h[1];
    h2 = ctx->h[2];
    h3 = ctx->h[3];
    h4 = ctx->h[4];

    c = h1 >> 28; h1 &= 0x0fffffff;
    h2 += c; c = h2 >> 28; h2 &= 0x0fffffff;
    h3 += c; c = h3 >> 28; h3 &= 0x0fffffff;
    h4 += c; c = h4 >> 28; h4 &= 0x0fffffff;
    h0 += c * 5; c = h0 >> 28; h0 &= 0x0fffffff;
    h1 += c;

    /* Compute h + -p */
    g0 = h0 + 5; c = g0 >> 28; g0 &= 0x0fffffff;
    g1 = h1 + c; c = g1 >> 28; g1 &= 0x0fffffff;
    g2 = h2 + c; c = g2 >> 28; g2 &= 0x0fffffff;
    g3 = h3 + c; c = g3 >> 28; g3 &= 0x0fffffff;
    g4 = h4 + c - (1 << 28);

    /* Select h if h < p, or h + -p if h >= p */
    mask = (g4 >> 31) - 1;
    g0 &= mask;
    g1 &= mask;
    g2 &= mask;
    g3 &= mask;
    g4 &= mask;
    mask = ~mask;
    h0 = (h0 & mask) | g0;
    h1 = (h1 & mask) | g1;
    h2 = (h2 & mask) | g2;
    h3 = (h3 & mask) | g3;
    h4 = (h4 & mask) | g4;

    /* h = h % (2^128) */
    h0 = ((h0) | (h1 << 28)) & 0xffffffff;
    h1 = ((h1 >> 4) | (h2 << 24)) & 0xffffffff;
    h2 = ((h2 >> 8) | (h3 << 20)) & 0xffffffff;
    h3 = ((h3 >> 12) | (h4 << 16)) & 0xffffffff;

    /* mac = (h + pad) % (2^128) */
    f = (uint64_t)h0 + ctx->pad[0]; h0 = (uint32_t)f;
    f = (uint64_t)h1 + ctx->pad[1] + (f >> 32); h1 = (uint32_t)f;
    f = (uint64_t)h2 + ctx->pad[2] + (f >> 32); h2 = (uint32_t)f;
    f = (uint64_t)h3 + ctx->pad[3] + (f >> 32); h3 = (uint32_t)f;

    store32_le(mac + 0, h0);
    store32_le(mac + 4, h1);
    store32_le(mac + 8, h2);
    store32_le(mac + 12, h3);

    /* Zero out context */
    memset(ctx, 0, sizeof(poly1305_context));
    ctx->final = 1;
}

void poly1305_auth(uint8_t *mac, const uint8_t *msg, size_t len, const uint8_t *key) {
    poly1305_context ctx;
    poly1305_init(&ctx, key);
    poly1305_update(&ctx, msg, len);
    poly1305_final(&ctx, mac);
}

int poly1305_verify(const uint8_t *mac1, const uint8_t *mac2) {
    return poly1305_constant_time_compare(mac1, mac2, POLY1305_TAGLEN);
}
