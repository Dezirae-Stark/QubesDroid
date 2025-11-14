#!/bin/bash

# QubesDroid GitHub Secrets Configuration Helper
# This script helps you prepare the keystore for GitHub Secrets

set -e

KEYSTORE_FILE="android/app/release.keystore"
OUTPUT_FILE="github-secrets.txt"

echo "========================================"
echo "QubesDroid GitHub Secrets Configuration"
echo "========================================"
echo ""

# Check if keystore exists
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "❌ Error: Keystore not found at $KEYSTORE_FILE"
    echo ""
    echo "Generate a keystore first with:"
    echo "  keytool -genkeypair -v \\"
    echo "    -keystore $KEYSTORE_FILE \\"
    echo "    -alias qubesdroid \\"
    echo "    -keyalg RSA \\"
    echo "    -keysize 4096 \\"
    echo "    -validity 10000 \\"
    echo "    -storepass YOUR_SECURE_PASSWORD \\"
    echo "    -keypass YOUR_SECURE_PASSWORD \\"
    echo "    -dname \"CN=QubesDroid, OU=Development, O=QubesDroid Project\""
    exit 1
fi

echo "✅ Found keystore: $KEYSTORE_FILE"
echo ""

# Generate base64
echo "📦 Encoding keystore to base64..."
KEYSTORE_BASE64=$(base64 -w 0 "$KEYSTORE_FILE")
echo "✅ Keystore encoded (${#KEYSTORE_BASE64} characters)"
echo ""

# Create output file
cat > "$OUTPUT_FILE" << EOF
======================================
QubesDroid GitHub Secrets
======================================

Copy these values to GitHub repository settings:
Settings → Secrets and variables → Actions → New repository secret

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Secret Name: KEYSTORE_BASE64
Value:
$KEYSTORE_BASE64

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Secret Name: KEYSTORE_PASSWORD
Value: android
⚠️  CHANGE THIS for production!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Secret Name: KEY_ALIAS
Value: qubesdroid

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Secret Name: KEY_PASSWORD
Value: android
⚠️  CHANGE THIS for production!

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Next Steps:
1. Go to https://github.com/YOUR_USERNAME/QubesDroid/settings/secrets/actions
2. Click "New repository secret" for each secret above
3. Test by creating a release tag:
   git tag v1.0.0-alpha
   git push origin v1.0.0-alpha

Security Notes:
- Keep this file (github-secrets.txt) PRIVATE
- Delete after uploading secrets to GitHub
- Never commit secrets to git
- Rotate keystore periodically

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Generated: $(date)
QubesDroid Release Signing Configuration
EOF

echo "✅ Secrets saved to: $OUTPUT_FILE"
echo ""
echo "⚠️  IMPORTANT:"
echo "   - This file contains sensitive information"
echo "   - Keep it private and secure"
echo "   - Delete after uploading to GitHub"
echo ""
echo "📋 To view secrets:"
echo "   cat $OUTPUT_FILE"
echo ""
echo "🚀 Ready to configure GitHub Secrets!"
