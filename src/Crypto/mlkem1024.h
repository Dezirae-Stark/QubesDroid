/*
 * ML-KEM-1024 (Kyber-1024) Wrapper for QubesDroid
 *
 * NIST Post-Quantum Cryptography Standard
 * FIPS 203: Module-Lattice-Based Key-Encapsulation Mechanism
 *
 * This is a wrapper around PQClean's ML-KEM-1024 implementation
 * providing a simplified API for QubesDroid volume encryption.
 */

#ifndef QUBESDROID_MLKEM1024_H
#define QUBESDROID_MLKEM1024_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Key and ciphertext sizes (from FIPS 203) */
#define MLKEM1024_PUBLICKEYBYTES  1568  /* Public key size */
#define MLKEM1024_SECRETKEYBYTES  3168  /* Secret key size */
#define MLKEM1024_CIPHERTEXTBYTES 1568  /* Ciphertext size */
#define MLKEM1024_BYTES           32    /* Shared secret size (256 bits) */

/*
 * Generate ML-KEM-1024 keypair
 *
 * Outputs:
 *   - pk: Public key (1568 bytes)
 *   - sk: Secret key (3168 bytes)
 *
 * Returns: 0 on success, -1 on failure
 *
 * Note: Requires secure random number generator (randombytes)
 */
int mlkem1024_keypair(uint8_t *pk, uint8_t *sk);

/*
 * ML-KEM-1024 Encapsulation
 *
 * Generates a shared secret and encapsulates it with the public key.
 *
 * Inputs:
 *   - pk: Public key (1568 bytes)
 *
 * Outputs:
 *   - ct: Ciphertext (1568 bytes)
 *   - ss: Shared secret (32 bytes)
 *
 * Returns: 0 on success, -1 on failure
 *
 * Use the shared secret as a 256-bit key for ChaCha20-Poly1305.
 */
int mlkem1024_enc(uint8_t *ct, uint8_t *ss, const uint8_t *pk);

/*
 * ML-KEM-1024 Decapsulation
 *
 * Recovers the shared secret from the ciphertext using the secret key.
 *
 * Inputs:
 *   - ct: Ciphertext (1568 bytes)
 *   - sk: Secret key (3168 bytes)
 *
 * Outputs:
 *   - ss: Shared secret (32 bytes)
 *
 * Returns: 0 on success, -1 on failure
 *
 * CRITICAL: Always check return value. Failure means the ciphertext
 *           is invalid or has been tampered with.
 */
int mlkem1024_dec(uint8_t *ss, const uint8_t *ct, const uint8_t *sk);

#ifdef __cplusplus
}
#endif

#endif /* QUBESDROID_MLKEM1024_H */
