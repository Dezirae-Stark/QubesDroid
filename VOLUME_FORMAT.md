# QubesDroid Volume Format Specification

**Version:** 1.0.0-alpha
**Date:** 2025-11-14
**Status:** Draft

## Overview

QubesDroid volumes are encrypted file containers using post-quantum cryptography. This document defines the binary format for QubesDroid volume files.

## Design Goals

1. **Post-Quantum Security:** Resistant to quantum computer attacks
2. **Authenticated Encryption:** Prevent tampering and detect corruption
3. **Forward Compatibility:** Allow future algorithm upgrades
4. **Simplicity:** Easy to implement and audit
5. **Mobile-Optimized:** Efficient on ARM processors

## Cryptographic Algorithms

- **Key Encapsulation:** ML-KEM-1024 (Kyber-1024, FIPS 203)
- **Encryption:** ChaCha20-Poly1305 (RFC 8439)
- **Key Derivation:** Argon2id (RFC 9106)
- **Hashing:** BLAKE2s-256

## Volume Structure

```
┌─────────────────────────────────────────────────────────┐
│ Volume Header (encrypted)                               │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ Magic Signature (8 bytes)      "QUBESDRD"           │ │
│ │ Version (4 bytes)               0x01000000          │ │
│ │ Header Size (4 bytes)           variable            │ │
│ │ Volume Size (8 bytes)           total bytes         │ │
│ │ Creation Timestamp (8 bytes)    Unix epoch          │ │
│ │ Reserved (32 bytes)             future use          │ │
│ │ ─────────────────────────────────────────────────── │ │
│ │ ML-KEM Public Key (1568 bytes)  encrypted           │ │
│ │ Salt (32 bytes)                 for Argon2id        │ │
│ │ Encrypted Master Key (48 bytes) 32-byte key + tag   │ │
│ │ Header Auth Tag (16 bytes)      Poly1305            │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
│
│ Data Blocks (encrypted with ChaCha20-Poly1305)
│ ┌─────────────────────────────────────────────────────┐
│ │ Block 0: Nonce (12) + Ciphertext (65536) + Tag (16)│
│ │ Block 1: Nonce (12) + Ciphertext (65536) + Tag (16)│
│ │ ...                                                 │
│ │ Block N: Nonce (12) + Ciphertext (<= 65536) + Tag  │
│ └─────────────────────────────────────────────────────┘
```

## Detailed Format

### Volume Header (Encrypted)

Total size: 1712 bytes

| Offset | Size  | Field                  | Description                           |
|--------|-------|------------------------|---------------------------------------|
| 0      | 8     | Magic                  | "QUBESDRD" (ASCII)                    |
| 8      | 4     | Version                | 0x01000000 (v1.0.0)                   |
| 12     | 4     | Header Size            | Total header size in bytes            |
| 16     | 8     | Volume Size            | Total volume size in bytes            |
| 24     | 8     | Creation Timestamp     | Unix timestamp (seconds since epoch)  |
| 32     | 32    | Reserved               | All zeros (for future extensions)     |
| 64     | 1568  | ML-KEM Public Key      | For key recovery/sharing              |
| 1632   | 32    | Salt                   | Random salt for Argon2id              |
| 1664   | 48    | Encrypted Master Key   | 32-byte key + 16-byte Poly1305 tag    |
| 1712   | -     | End of Header          |                                       |

### Encryption Scheme

#### Master Key Derivation

```
User Password
    ↓
Argon2id(password, salt, t=4, m=256MB, p=4)
    ↓
32-byte Password-Derived Key (PDK)
    ↓
ChaCha20-Poly1305.Encrypt(Master Key, PDK, nonce=0)
    ↓
Encrypted Master Key (stored in header)
```

#### Optional: ML-KEM Key Encapsulation

For multi-user volumes or key recovery:

```
ML-KEM-1024.Keypair()
    ↓
Public Key (1568 bytes) → stored in header
Secret Key (3168 bytes) → stored externally or in keychain
    ↓
ML-KEM-1024.Encapsulate(Public Key)
    ↓
Ciphertext (1568 bytes) + Shared Secret (32 bytes)
    ↓
XOR(Master Key, BLAKE2s(Shared Secret))
    ↓
Dual-protected Master Key
```

#### Data Block Encryption

Each 64KB block is encrypted independently:

```
Block N:
    Nonce = BLAKE2s-96(Master Key || Block Index)
    AAD = Block Index (8 bytes)
    Ciphertext + Tag = ChaCha20-Poly1305.Encrypt(
        plaintext=data,
        key=Master Key,
        nonce=Nonce,
        aad=AAD
    )
```

## Security Parameters

| Parameter           | Value      | Rationale                              |
|---------------------|------------|----------------------------------------|
| Argon2id t_cost     | 4          | Mobile-friendly (2-3 seconds)          |
| Argon2id m_cost     | 256 MB     | Balance security/mobile resources      |
| Argon2id parallelism| 4          | Utilize multi-core ARM processors      |
| Block Size          | 64 KB      | Efficient for mobile file I/O          |
| Master Key Size     | 32 bytes   | 256-bit security                       |
| Nonce Size          | 12 bytes   | ChaCha20-Poly1305 standard             |
| Tag Size            | 16 bytes   | Poly1305 authentication                |

## Security Guarantees

1. **Confidentiality:** ChaCha20-Poly1305 provides IND-CCA2 security
2. **Authenticity:** Poly1305 MAC prevents tampering
3. **Post-Quantum:** ML-KEM-1024 provides 128-bit quantum security
4. **Forward Secrecy:** Compromised volume doesn't reveal others
5. **No Key Reuse:** Each block uses unique nonce

## Limitations

1. **Max Volume Size:** 2^63 - 1 bytes (8 exabytes)
2. **Max Blocks:** 2^64 (limited by nonce generation)
3. **Minimum Android:** 8.0 (API 26)

## Implementation Notes

### Creating a Volume

1. Generate random 32-byte Master Key
2. Derive Password-Derived Key using Argon2id
3. Encrypt Master Key with PDK
4. Optionally generate ML-KEM keypair for recovery
5. Write encrypted header
6. Encrypt and write data blocks

### Mounting a Volume

1. Read and parse header
2. Derive PDK from user password
3. Decrypt Master Key
4. Verify header integrity
5. Decrypt data blocks on-demand

### Performance

Estimated on ARM Cortex-A72:
- Volume creation: ~3 seconds (Argon2id dominant)
- Block encryption: ~50 MB/s (ChaCha20-Poly1305)
- Block decryption: ~50 MB/s
- Random access: O(1) per block

## Future Extensions

Reserved header space allows:
- Multiple key slots (multi-user)
- Key derivation function upgrades
- Compression algorithms
- Metadata encryption
- Volume snapshots

## Version History

- **v1.0.0-alpha (2025-11-14):** Initial specification

---

**Author:** Dezirae Stark
**Project:** QubesDroid - Post-Quantum Mobile Encryption
