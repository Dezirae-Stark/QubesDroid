# QubesDroid Release Signing Configuration

## Overview

QubesDroid uses Android app signing to ensure authenticity and integrity of releases. This document explains how to configure GitHub Secrets for automated release builds.

## Keystore Information

- **Algorithm:** RSA 4096-bit
- **Validity:** 10,000 days (~27 years)
- **Alias:** qubesdroid
- **Location:** `android/app/release.keystore` (NOT committed to git)

## GitHub Secrets Configuration

To enable automated release builds, configure the following secrets in your GitHub repository:

### Required Secrets

1. **KEYSTORE_BASE64**
   - Base64-encoded keystore file
   - Generate with:
     ```bash
     base64 -w 0 android/app/release.keystore > keystore.b64
     ```
   - Copy the contents of `keystore.b64` to this secret

2. **KEYSTORE_PASSWORD**
   - Password for the keystore
   - Default: `android` (CHANGE THIS for production!)

3. **KEY_ALIAS**
   - Alias of the key within the keystore
   - Value: `qubesdroid`

4. **KEY_PASSWORD**
   - Password for the key alias
   - Default: `android` (CHANGE THIS for production!)

### Setting Secrets in GitHub

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Add each secret listed above

## Updating the Keystore

To generate a new production keystore with secure passwords:

```bash
keytool -genkeypair -v \
  -keystore android/app/release.keystore \
  -alias qubesdroid \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass YOUR_SECURE_STOREPASS \
  -keypass YOUR_SECURE_KEYPASS \
  -dname "CN=QubesDroid, OU=Development, O=QubesDroid Project, L=Unknown, ST=Unknown, C=US"
```

**IMPORTANT:**
- Replace `YOUR_SECURE_STOREPASS` and `YOUR_SECURE_KEYPASS` with strong passwords
- Store the keystore and passwords in a secure location
- NEVER commit the keystore to git
- Update GitHub Secrets with the new passwords

## Verifying Keystore

To verify the keystore contents:

```bash
keytool -list -v -keystore android/app/release.keystore -storepass android
```

## GitHub Actions Workflow

The `.github/workflows/android-build.yml` workflow automatically:

1. Decodes `KEYSTORE_BASE64` to a temporary keystore file
2. Uses the secrets to sign release APKs
3. Uploads signed APKs as artifacts
4. Creates GitHub releases for version tags

## Security Best Practices

1. **Never commit keystores** - They are in `.gitignore`
2. **Use strong passwords** - Change default `android` password
3. **Backup the keystore** - Store securely offline
4. **Rotate keys periodically** - Generate new keystore every few years
5. **Limit secret access** - Only authorized collaborators
6. **Enable 2FA** - On GitHub account managing secrets

## Troubleshooting

### Build fails with "Keystore file not found"
- Ensure `KEYSTORE_BASE64` is set correctly
- Verify base64 encoding: `cat keystore.b64 | base64 -d > test.keystore`

### Build fails with "Invalid keystore format"
- Re-generate base64: `base64 -w 0 android/app/release.keystore`
- Ensure no newlines in the base64 secret

### Build fails with "Incorrect password"
- Verify `KEYSTORE_PASSWORD` and `KEY_PASSWORD` match your keystore
- Test locally: `keytool -list -keystore android/app/release.keystore`

## Development vs Production

### Development (current)
- **Keystore:** `android/app/release.keystore`
- **Passwords:** `android` (default)
- **Purpose:** Testing release builds

### Production (recommended)
- **Keystore:** Generate new with secure passwords
- **Passwords:** Strong, unique passwords
- **Purpose:** Public releases
- **Storage:** Secure offline backup + GitHub Secrets

## Contact

For security issues related to keystore or signing, please contact the project maintainers privately.

---

**Last Updated:** 2025-11-14
**QubesDroid Version:** 1.0.0-alpha
