/*
 * Kyber-1024 - Post-Quantum Key Encapsulation Mechanism
 * NIST PQC Standard (FIPS 203)
 *
 * Security Level: NIST Level 5 (256-bit quantum security)
 *
 * Based on the CRYSTALS-Kyber specification
 * Optimized for ARM64/ARM32 mobile platforms
 */

#ifndef KYBER1024_H
#define KYBER1024_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Kyber-1024 parameters */
#define KYBER1024_K 4                    /* Module rank */
#define KYBER1024_N 256                  /* Polynomial degree */
#define KYBER1024_Q 3329                 /* Modulus */
#define KYBER1024_ETA1 2                 /* Noise parameter eta1 */
#define KYBER1024_ETA2 2                 /* Noise parameter eta2 */
#define KYBER1024_DU 11                  /* Compression parameter du */
#define KYBER1024_DV 5                   /* Compression parameter dv */

/* Key sizes */
#define KYBER1024_PUBLICKEYBYTES  1568   /* Public key size */
#define KYBER1024_SECRETKEYBYTES  3168   /* Secret key size */
#define KYBER1024_CIPHERTEXTBYTES 1568   /* Ciphertext size */
#define KYBER1024_SHAREDSECRETBYTES 32   /* Shared secret size (256 bits for ChaCha20) */
#define KYBER1024_SEEDBYTES 32           /* Seed size for key generation */

/* Symmetric primitives */
#define KYBER1024_SYMBYTES 32            /* Size of hashes and seeds */

/*
 * Generate a Kyber-1024 keypair
 *
 * Inputs:
 *   - seed: 32-byte random seed (from Argon2id output)
 *
 * Outputs:
 *   - pk: Public key (1568 bytes)
 *   - sk: Secret key (3168 bytes)
 *
 * Returns: 0 on success, -1 on failure
 */
int kyber1024_keypair(uint8_t *pk, uint8_t *sk, const uint8_t *seed);

/*
 * Generate a Kyber-1024 keypair from random bytes
 *
 * Uses system randomness (ChaCha20 RNG)
 *
 * Outputs:
 *   - pk: Public key (1568 bytes)
 *   - sk: Secret key (3168 bytes)
 *
 * Returns: 0 on success, -1 on failure
 */
int kyber1024_keypair_random(uint8_t *pk, uint8_t *sk);

/*
 * Encapsulation: Generate shared secret and ciphertext
 *
 * Given a public key, generate a random shared secret and encrypt it.
 *
 * Inputs:
 *   - pk: Public key (1568 bytes)
 *
 * Outputs:
 *   - ct: Ciphertext (1568 bytes)
 *   - ss: Shared secret (32 bytes) - Use this as ChaCha20 key
 *
 * Returns: 0 on success, -1 on failure
 */
int kyber1024_enc(uint8_t *ct, uint8_t *ss, const uint8_t *pk);

/*
 * Decapsulation: Recover shared secret from ciphertext
 *
 * Given a secret key and ciphertext, recover the shared secret.
 *
 * Inputs:
 *   - ct: Ciphertext (1568 bytes)
 *   - sk: Secret key (3168 bytes)
 *
 * Outputs:
 *   - ss: Shared secret (32 bytes) - Use this as ChaCha20 key
 *
 * Returns: 0 on success, -1 on failure
 */
int kyber1024_dec(uint8_t *ss, const uint8_t *ct, const uint8_t *sk);

/*
 * Verify Kyber-1024 implementation
 * Runs self-tests to ensure correctness
 *
 * Returns: 0 if all tests pass, -1 otherwise
 */
int kyber1024_selftest(void);

#ifdef __cplusplus
}
#endif

#endif /* KYBER1024_H */
