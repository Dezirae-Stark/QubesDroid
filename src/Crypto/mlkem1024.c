/*
 * ML-KEM-1024 (Kyber-1024) Wrapper Implementation
 *
 * This file provides a simplified API wrapper around PQClean's
 * ML-KEM-1024 implementation for QubesDroid.
 */

#include "mlkem1024.h"
#include "ML-KEM-1024/api.h"

/*
 * Generate ML-KEM-1024 keypair
 */
int mlkem1024_keypair(uint8_t *pk, uint8_t *sk) {
    if (!pk || !sk) {
        return -1;
    }

    return PQCLEAN_MLKEM1024_CLEAN_crypto_kem_keypair(pk, sk);
}

/*
 * ML-KEM-1024 Encapsulation
 */
int mlkem1024_enc(uint8_t *ct, uint8_t *ss, const uint8_t *pk) {
    if (!ct || !ss || !pk) {
        return -1;
    }

    return PQCLEAN_MLKEM1024_CLEAN_crypto_kem_enc(ct, ss, pk);
}

/*
 * ML-KEM-1024 Decapsulation
 */
int mlkem1024_dec(uint8_t *ss, const uint8_t *ct, const uint8_t *sk) {
    if (!ss || !ct || !sk) {
        return -1;
    }

    return PQCLEAN_MLKEM1024_CLEAN_crypto_kem_dec(ss, ct, sk);
}
