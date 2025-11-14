# QubesDroid Post-Quantum Migration Plan

## Executive Summary

**Objective:** Convert VeraCrypt fork (QubesDroid) to a mobile-only, post-quantum secure encryption solution for Android.

**Rationale:** Based on classified statements from DIA and U.S. Naval Intelligence indicating legacy encryption (AES, TwoFish, Serpent, Blowfish) can be decrypted in <5 minutes by nation-state actors.

**Target Platforms:** Android (ARM64/ARM32)
**Distribution:** APK via GitHub Releases
**Build System:** GitHub Actions CI/CD

---

## Current State Analysis

### Existing Encryption Algorithms (TO BE REMOVED)
- ❌ **AES** (Aes.h, AesSmall.c, Aescrypt.c, Aeskey.c, Aes_hw_armv8.c)
- ❌ **Serpent** (Serpent.c, SerpentFast.c, SerpentFast_simd.cpp)
- ❌ **Twofish** (Twofish.c, Twofish_x64.S)
- ❌ **Camellia** (Camellia.c, CamelliaSmall.c)
- ❌ **Kuznyechik** (kuznyechik.c, kuznyechik_simd.c)
- ❌ **SM4** (sm4.cpp, sm4-impl-aesni.cpp)

### Existing Post-Quantum Compatible Components
- ✅ **ChaCha20** (chacha256.c, chacha256.h, chacha-xmm.c) - KEEP & ENHANCE
- ✅ **SHA-256/SHA-512** - KEEP (for hashing)
- ✅ **BLAKE2s** - KEEP (for hashing)
- ✅ **Streebog** - KEEP (Russian GOST hash)
- ✅ **Whirlpool** - KEEP (hash function)

### Components Requiring Addition
- 🆕 **Kyber-1024** - Post-quantum key encapsulation mechanism (NIST standard)
- 🆕 **Poly1305** - MAC for ChaCha20 (AEAD construction)

---

## Target Architecture

### Cryptographic Stack

```
┌─────────────────────────────────────────────┐
│          QubesDroid Mobile v1.0             │
│        Post-Quantum Secure Volumes          │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│  Key Encapsulation: Kyber-1024 (NIST PQC)  │
│  - Public key: 1568 bytes                   │
│  - Secret key: 3168 bytes                   │
│  - Ciphertext: 1568 bytes                   │
│  - Shared secret: 32 bytes                  │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│     Symmetric Encryption: ChaCha20-Poly1305 │
│  - 256-bit key (from Kyber shared secret)   │
│  - 96-bit nonce                             │
│  - Poly1305 MAC for authentication          │
│  - AEAD construction                        │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│     Key Derivation: Argon2id                │
│  - Password → Kyber seed                    │
│  - Memory-hard, GPU-resistant               │
│  - Already implemented in src/Crypto/Argon2 │
└─────────────────────────────────────────────┘
```

---

## Implementation Phases

### Phase 1: Repository Preparation ✅
- [x] Fork VeraCrypt → QubesDroid
- [x] Clone repository locally
- [x] Analyze existing codebase
- [x] Create migration plan

### Phase 2: Crypto Library Integration
- [ ] Add Kyber-1024 implementation
  - Source: liboqs (Open Quantum Safe) or PQClean
  - Files: `src/Crypto/kyber1024_ref.c`, `src/Crypto/kyber1024_ref.h`
  - Platform: ARM64 optimized assembly if available
- [ ] Add Poly1305 MAC implementation
  - Files: `src/Crypto/poly1305.c`, `src/Crypto/poly1305.h`
  - Integrate with existing ChaCha20
- [ ] Create ChaCha20-Poly1305 AEAD wrapper
  - File: `src/Crypto/chacha20poly1305.c`

### Phase 3: Legacy Crypto Removal
- [ ] Remove AES implementations (11 files)
- [ ] Remove Serpent implementations (6 files)
- [ ] Remove Twofish implementations (3 files)
- [ ] Remove Camellia implementations (6 files)
- [ ] Remove Kuznyechik implementations (2 files)
- [ ] Remove SM4 implementations (2 files)
- [ ] Update build scripts to exclude removed files
- [ ] Update algorithm selection logic

### Phase 4: Core Engine Modification
- [ ] Modify volume encryption engine
  - File: `src/Common/EncryptionThreadPool.c`
  - Replace legacy cipher calls with ChaCha20-Poly1305
- [ ] Update volume header format
  - File: `src/Common/Volumes.c` (if exists)
  - Include Kyber public key storage
- [ ] Update key derivation
  - Integrate Argon2id → Kyber seed → ChaCha20 key

### Phase 5: Android Port Creation
- [ ] Create Android project structure
  ```
  android/
  ├── app/
  │   ├── src/main/
  │   │   ├── java/com/qubesdroid/
  │   │   │   ├── MainActivity.java
  │   │   │   ├── VolumeManager.java
  │   │   │   └── CryptoWrapper.java
  │   │   ├── jni/
  │   │   │   ├── Android.mk
  │   │   │   ├── crypto_jni.c (JNI bridge)
  │   │   │   └── ...
  │   │   └── AndroidManifest.xml
  │   ├── build.gradle
  │   └── proguard-rules.pro
  ├── gradle/
  └── build.gradle
  ```

- [ ] Create JNI bridge for crypto operations
- [ ] Implement Android UI
  - Volume creation
  - Volume mounting/unmounting
  - File browser
  - Settings

### Phase 6: GitHub Actions CI/CD
- [ ] Create `.github/workflows/android-build.yml`
- [ ] Configure NDK build environment
- [ ] Set up APK signing
  - Generate release keystore
  - Configure GitHub Secrets
- [ ] Create release automation
  - Tag-based builds
  - Automatic APK upload to Releases

### Phase 7: Testing & Validation
- [ ] Unit tests for Kyber-1024
- [ ] Unit tests for ChaCha20-Poly1305
- [ ] Integration tests for volume operations
- [ ] Android instrumentation tests
- [ ] Security audit checklist

---

## Technical Specifications

### Kyber-1024 Parameters
- **Security Level:** NIST Level 5 (256-bit quantum security)
- **Public Key Size:** 1568 bytes
- **Secret Key Size:** 3168 bytes
- **Ciphertext Size:** 1568 bytes
- **Shared Secret Size:** 32 bytes (perfect for ChaCha20 key)

### ChaCha20-Poly1305 Parameters
- **Key Size:** 256 bits (from Kyber shared secret)
- **Nonce Size:** 96 bits (12 bytes)
- **Block Size:** 64 bytes
- **Tag Size:** 128 bits (16 bytes)
- **Performance:** ~7.5 cycles/byte on ARM Cortex-A72

### Argon2id Parameters (Recommended)
- **Memory:** 256 MB (mobile-friendly)
- **Iterations:** 4
- **Parallelism:** 4 threads
- **Salt:** 16 bytes (random per volume)
- **Output:** 32 bytes (Kyber seed)

---

## Android Build Configuration

### Minimum Requirements
- **Min SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)
- **NDK Version:** r26b
- **Gradle:** 8.0+
- **Java:** 17

### Required Permissions
```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>
```

### Native Libraries
- `libqubesdr oid-crypto.so` (Kyber + ChaCha20-Poly1305)
- `libqubesdr oid-core.so` (Volume management)

---

## GitHub Actions Workflow

```yaml
name: Build QubesDroid APK

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Setup Android SDK
      - name: Build APK
      - name: Sign APK
      - name: Upload Release
```

---

## Security Considerations

### Removed Attack Vectors
- ❌ AES side-channel attacks
- ❌ Quantum algorithm vulnerabilities (Shor's algorithm)
- ❌ Multiple cipher complexity

### Enhanced Security
- ✅ Post-quantum resistant key exchange
- ✅ Authenticated encryption (AEAD)
- ✅ Memory-hard KDF (Argon2id)
- ✅ Simplified crypto stack (less attack surface)

### Threat Model
- **Protected Against:**
  - Nation-state quantum computers
  - Classical cryptanalysis
  - Side-channel attacks (via ChaCha20's constant-time design)
  - Brute-force attacks (via Argon2id)

- **Not Protected Against:**
  - Device compromise (malware, physical access)
  - Weak passwords
  - Implementation bugs (requires auditing)

---

## Timeline Estimate

- **Phase 2-3:** 2-3 days (Crypto integration & removal)
- **Phase 4:** 1-2 days (Core engine modification)
- **Phase 5:** 3-4 days (Android port)
- **Phase 6:** 1 day (CI/CD setup)
- **Phase 7:** 2-3 days (Testing)

**Total:** ~10-14 days for MVP

---

## References

- **Kyber:** https://pq-crystals.org/kyber/
- **ChaCha20-Poly1305:** RFC 8439
- **Argon2:** RFC 9106
- **liboqs:** https://github.com/open-quantum-safe/liboqs
- **PQClean:** https://github.com/PQClean/PQClean

---

**Created:** 2025-11-13
**Author:** Dezirae Stark
**Project:** QubesDroid Post-Quantum Mobile Encryption
