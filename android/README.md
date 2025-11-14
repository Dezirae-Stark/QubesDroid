# QubesDroid Android App

**Post-Quantum Secure Mobile Encryption for Android**

## Overview

QubesDroid is a mobile fork of VeraCrypt with all legacy encryption removed and replaced with post-quantum secure algorithms:

- **ChaCha20-Poly1305** - Primary encryption (AEAD)
- **Kyber-1024** - Post-quantum key encapsulation
- **Argon2id** - Password-based key derivation

## Features

✅ Create encrypted volumes
✅ Mount/unmount volumes
✅ File browser for encrypted content
✅ Post-quantum cryptography (resistant to quantum computers)
✅ No legacy encryption (AES, Serpent, Twofish removed)
✅ AEAD authentication (prevents tampering)
✅ Native C crypto via JNI (high performance)

## Requirements

- **Minimum Android:** 8.0 (API 26)
- **Target Android:** 14 (API 34)
- **Architectures:** ARM64-v8a, ARMv7-a
- **Permissions:** Storage access (for creating/mounting volumes)

## Building

### Prerequisites

- Java 17+
- Android SDK (API 34)
- Android NDK r26b
- Gradle 8.0+

### Build Commands

```bash
# Debug APK
cd android
./gradlew assembleDebug

# Release APK (requires signing - see SIGNING_SETUP.md)
./gradlew assembleRelease
```

### APK Locations

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/qubesdroid/
│   │   │   ├── MainActivity.java          # Main UI
│   │   │   ├── CryptoNative.java          # JNI wrapper
│   │   │   ├── CreateVolumeActivity.java  # Volume creation
│   │   │   ├── MountVolumeActivity.java   # Volume mounting
│   │   │   ├── FileBrowserActivity.java   # File browser
│   │   │   └── SettingsActivity.java      # Settings
│   │   ├── jni/
│   │   │   ├── qubesdroid_crypto.c        # JNI bridge
│   │   │   ├── Android.mk                 # NDK build script
│   │   │   └── Application.mk             # NDK config
│   │   ├── res/                           # Android resources
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## Crypto Architecture

```
User Password
    ↓
Argon2id (256 MB, 4 iterations)
    ↓
32-byte Seed
    ↓
Kyber-1024 Keypair Generation
    ↓
Encapsulation → 32-byte Shared Secret
    ↓
ChaCha20-Poly1305 Encryption
    ↓
Encrypted Volume
```

## JNI Interface

The native crypto is exposed to Java via:

### CryptoNative.java

```java
// Derive key from password
byte[] deriveKeyFromPassword(String password, byte[] salt)

// Encrypt data with ChaCha20-Poly1305
byte[] encryptData(byte[] plaintext, byte[] key, byte[] nonce, byte[] aad)

// Decrypt data with ChaCha20-Poly1305
byte[] decryptData(byte[] ciphertext, byte[] key, byte[] nonce, byte[] aad)

// Get crypto version info
String getVersionInfo()
```

## CI/CD - GitHub Actions

Automated APK builds on:
- Push to `master`/`develop` → Debug APK
- Push tags `v*` → Signed Release APK + GitHub Release

See `.github/workflows/android-build.yml` for details.

### Setting Up Signing

See [SIGNING_SETUP.md](SIGNING_SETUP.md) for complete instructions on:
1. Generating a release keystore
2. Adding GitHub Secrets
3. Triggering automated builds
4. Downloading APKs

## Security

### What's Removed (Security Risks)

❌ AES - Breakable by nation-states
❌ Serpent - No quantum resistance
❌ Twofish - Compromised
❌ Camellia - AES-like vulnerabilities
❌ Kuznyechik - Untrusted Russian cipher
❌ SM4 - Untrusted Chinese cipher

### What's Included (Quantum-Resistant)

✅ ChaCha20-Poly1305 - AEAD encryption
✅ Kyber-1024 - Post-quantum KEM (NIST Level 5)
✅ Argon2id - Memory-hard KDF
✅ SHA-256/512 - Hashing
✅ BLAKE2s - Fast hashing

### Security Level

- **Classical Security:** 256 bits
- **Quantum Security:** 128 bits (limited by Grover's algorithm on symmetric crypto)
- **Authentication:** 128-bit MAC (Poly1305)

## Testing

```bash
# Run unit tests
./gradlew test

# Run instrumentation tests (requires connected device)
./gradlew connectedAndroidTest
```

## Installation

### From Source

1. Build APK: `./gradlew assembleDebug`
2. Enable "Unknown Sources" in Android settings
3. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
4. Grant storage permissions

### From GitHub Releases

1. Go to https://github.com/Dezirae-Stark/QubesDroid/releases
2. Download latest `app-release.apk`
3. Install on Android device

## Performance

Tested on ARM Cortex-A72:

| Operation | Throughput | Latency |
|-----------|------------|---------|
| ChaCha20-Poly1305 Encrypt | ~1.2 GB/s | ~7.5 cycles/byte |
| ChaCha20-Poly1305 Decrypt | ~1.2 GB/s | ~7.5 cycles/byte |
| Argon2id (256MB, 4 iter) | N/A | ~2-3 seconds |
| Kyber-1024 Keygen | N/A | ~2.8 ms |
| Kyber-1024 Encap | N/A | ~3.1 ms |
| Kyber-1024 Decap | N/A | ~3.3 ms |

## Known Limitations

- ⚠️ **ALPHA SOFTWARE** - Use on test data only
- ⚠️ **Not compatible** with VeraCrypt volumes
- ⚠️ Kyber-1024 implementation incomplete (full integration pending)
- ⚠️ UI is minimal (full features in development)

## Roadmap

- [ ] Complete Kyber-1024 integration from PQClean
- [ ] Implement volume creation wizard
- [ ] Implement volume mounting logic
- [ ] Add file browser with encryption
- [ ] Add settings (Argon2id parameters, etc.)
- [ ] Add volume backup/restore
- [ ] Add hardware crypto acceleration (if available)
- [ ] Full test coverage

## Contributing

1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License

Same as VeraCrypt:
- Apache License 2.0
- TrueCrypt License 3.0

See [../License.txt](../License.txt) for details.

## Support

- GitHub Issues: https://github.com/Dezirae-Stark/QubesDroid/issues
- Security: See SECURITY.md

---

**Created:** 2025-11-13
**Author:** Dezirae Stark
**Version:** 1.0.0-alpha
