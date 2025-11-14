# APK Signing Setup for QubesDroid

This guide explains how to set up APK signing for automated builds via GitHub Actions.

## Overview

QubesDroid uses GitHub Actions to automatically build and sign APKs when you push tags (releases). The APKs are signed using a keystore that you create and store securely in GitHub Secrets.

---

## Step 1: Generate Release Keystore

Run this command on your local machine (requires Java keytool):

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias qubesdroid \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storepass YOUR_STRONG_PASSWORD \
  -keypass YOUR_STRONG_PASSWORD
```

**Important:**
- Replace `YOUR_STRONG_PASSWORD` with a strong password (save it!)
- You'll be prompted for details (name, organization, etc.)
- This generates `release.keystore` file
- **Keep this file SECRET and BACKED UP!**

---

## Step 2: Convert Keystore to Base64

```bash
base64 release.keystore > release.keystore.base64
```

Or on macOS:
```bash
base64 -i release.keystore -o release.keystore.base64
```

This creates a text file with the Base64-encoded keystore.

---

## Step 3: Add GitHub Secrets

1. Go to your GitHub repository
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"** and add these secrets:

| Secret Name | Value | Example |
|-------------|-------|---------|
| `KEYSTORE_BASE64` | Contents of `release.keystore.base64` | (long base64 string) |
| `KEYSTORE_PASSWORD` | Password you used in Step 1 | `MyStr0ngP@ssw0rd!` |
| `KEY_ALIAS` | Key alias from Step 1 | `qubesdroid` |
| `KEY_PASSWORD` | Same as KEYSTORE_PASSWORD | `MyStr0ngP@ssw0rd!` |

**Security Notes:**
- Never commit the keystore or passwords to git
- GitHub Secrets are encrypted and only accessible to workflows
- Anyone with write access to the repo can modify workflows to access secrets

---

## Step 4: Trigger a Build

### Manual Build
```bash
# Push to master to build debug APK
git push origin master

# Create and push tag to build release APK
git tag v1.0.0-alpha
git push origin v1.0.0-alpha
```

### Automated Build
The workflow runs automatically on:
- Push to `master` or `develop` branches (builds debug APK)
- Push to tags starting with `v` (builds signed release APK)
- Pull requests (builds debug APK)

---

## Step 5: Download APK

### From GitHub Actions
1. Go to **Actions** tab
2. Click on the workflow run
3. Scroll to **Artifacts** section
4. Download the APK

### From GitHub Releases (tags only)
1. Go to **Releases** tab
2. Click on the release
3. Download `app-release.apk` from Assets

---

## Testing Local Builds

To build locally without the keystore:

```bash
cd android
./gradlew assembleDebug
```

Debug APK will be at:
```
android/app/build/outputs/apk/debug/app-debug.apk
```

To build release APK locally:
```bash
export KEYSTORE_FILE=/path/to/release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=qubesdroid
export KEY_PASSWORD=your_password

./gradlew assembleRelease
```

---

## Verifying APK Signature

```bash
# Check if APK is signed
jarsigner -verify -verbose -certs app-release.apk

# View signature details
apksigner verify --print-certs app-release.apk
```

---

## Security Best Practices

1. **Backup Keystore:**
   - Store `release.keystore` in a password manager
   - Keep offline backup in secure location
   - If lost, you can't update the app!

2. **Rotate Secrets:**
   - Consider rotating keystore every 2-3 years
   - Update GitHub Secrets when rotating

3. **Limit Access:**
   - Only repository admins should access secrets
   - Review workflow changes carefully

4. **Use Strong Passwords:**
   - Minimum 20 characters
   - Mix of letters, numbers, symbols
   - Don't reuse passwords

---

## Troubleshooting

### Build fails with "Keystore not found"
- Check that `KEYSTORE_BASE64` secret is set correctly
- Verify the base64 encoding is correct

### Build fails with "Invalid keystore format"
- Re-generate the base64: `base64 release.keystore | tr -d '\n' > release.keystore.base64`
- Ensure no line breaks in the secret value

### APK installs but says "App not installed"
- Uninstall previous version first
- Check APK signature matches previous installs
- Enable "Unknown Sources" in Android settings

### Signature verification fails
- Keystore password is incorrect
- Key alias doesn't match
- Corrupted keystore file

---

## Release Checklist

Before creating a new release:

- [ ] Update `versionCode` and `versionName` in `app/build.gradle`
- [ ] Update `CHANGELOG.md` with new features/fixes
- [ ] Test debug APK on real device
- [ ] Create git tag: `git tag v1.0.0`
- [ ] Push tag: `git push origin v1.0.0`
- [ ] Wait for GitHub Actions to build release APK
- [ ] Download and test release APK
- [ ] Update release notes on GitHub
- [ ] Announce release

---

**Created:** 2025-11-13
**Author:** Dezirae Stark
**Project:** QubesDroid
