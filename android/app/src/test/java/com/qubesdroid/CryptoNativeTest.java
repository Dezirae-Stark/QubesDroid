package com.qubesdroid;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for CryptoNative cryptographic operations
 *
 * Tests:
 * - ChaCha20-Poly1305 encryption/decryption
 * - Argon2id key derivation
 * - ML-KEM-1024 key encapsulation
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CryptoNativeTest {

    private CryptoNative crypto;

    @Before
    public void setUp() {
        crypto = new CryptoNative();
    }

    @Test
    public void testVersionInfo() {
        String version = crypto.getVersionInfo();
        assertNotNull("Version info should not be null", version);
        assertTrue("Version should contain ChaCha20", version.contains("ChaCha20"));
    }

    @Test
    public void testGenerateSalt() {
        byte[] salt = CryptoNative.generateSalt();
        assertNotNull("Salt should not be null", salt);
        assertEquals("Salt should be 16 bytes", 16, salt.length);

        // Verify randomness - two salts should be different
        byte[] salt2 = CryptoNative.generateSalt();
        assertFalse("Two generated salts should be different",
            java.util.Arrays.equals(salt, salt2));
    }

    @Test
    public void testGenerateNonce() {
        byte[] nonce = CryptoNative.generateNonce();
        assertNotNull("Nonce should not be null", nonce);
        assertEquals("Nonce should be 12 bytes", 12, nonce.length);

        // Verify randomness
        byte[] nonce2 = CryptoNative.generateNonce();
        assertFalse("Two generated nonces should be different",
            java.util.Arrays.equals(nonce, nonce2));
    }

    @Test
    public void testArgon2idKeyDerivation() {
        String password = "test_password_123";
        byte[] salt = CryptoNative.generateSalt();

        byte[] key = crypto.deriveKeyFromPassword(password, salt);
        assertNotNull("Derived key should not be null", key);
        assertEquals("Derived key should be 32 bytes", 32, key.length);

        // Same password + salt should produce same key
        byte[] key2 = crypto.deriveKeyFromPassword(password, salt);
        assertArrayEquals("Same password/salt should produce same key", key, key2);

        // Different password should produce different key
        byte[] key3 = crypto.deriveKeyFromPassword("different_password", salt);
        assertFalse("Different password should produce different key",
            java.util.Arrays.equals(key, key3));
    }

    @Test
    public void testChaCha20Poly1305Encryption() {
        byte[] plaintext = "Hello, QubesDroid!".getBytes();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        byte[] nonce = CryptoNative.generateNonce();

        byte[] ciphertext = crypto.encryptData(plaintext, key, nonce, null);
        assertNotNull("Ciphertext should not be null", ciphertext);
        assertEquals("Ciphertext should be plaintext + 16-byte tag",
            plaintext.length + 16, ciphertext.length);

        // Verify ciphertext is different from plaintext
        assertFalse("Ciphertext should differ from plaintext",
            java.util.Arrays.equals(plaintext, java.util.Arrays.copyOf(ciphertext, plaintext.length)));
    }

    @Test
    public void testChaCha20Poly1305Decryption() {
        byte[] plaintext = "Post-Quantum Security!".getBytes();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        byte[] nonce = CryptoNative.generateNonce();

        // Encrypt
        byte[] ciphertext = crypto.encryptData(plaintext, key, nonce, null);
        assertNotNull("Encryption should succeed", ciphertext);

        // Decrypt
        byte[] decrypted = crypto.decryptData(ciphertext, key, nonce, null);
        assertNotNull("Decryption should succeed", decrypted);
        assertArrayEquals("Decrypted text should match original", plaintext, decrypted);
    }

    @Test
    public void testChaCha20Poly1305WithAAD() {
        byte[] plaintext = "Authenticated data".getBytes();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        byte[] nonce = CryptoNative.generateNonce();
        byte[] aad = "Additional data".getBytes();

        // Encrypt with AAD
        byte[] ciphertext = crypto.encryptData(plaintext, key, nonce, aad);
        assertNotNull("Encryption with AAD should succeed", ciphertext);

        // Decrypt with same AAD
        byte[] decrypted = crypto.decryptData(ciphertext, key, nonce, aad);
        assertNotNull("Decryption with correct AAD should succeed", decrypted);
        assertArrayEquals("Decrypted text should match", plaintext, decrypted);

        // Decrypt with wrong AAD should fail
        byte[] wrongAad = "Wrong data".getBytes();
        byte[] failedDecrypt = crypto.decryptData(ciphertext, key, nonce, wrongAad);
        assertNull("Decryption with wrong AAD should fail", failedDecrypt);
    }

    @Test
    public void testChaCha20Poly1305WrongKey() {
        byte[] plaintext = "Secret message".getBytes();
        byte[] key = new byte[32];
        new java.security.SecureRandom().nextBytes(key);
        byte[] nonce = CryptoNative.generateNonce();

        byte[] ciphertext = crypto.encryptData(plaintext, key, nonce, null);

        // Try to decrypt with wrong key
        byte[] wrongKey = new byte[32];
        new java.security.SecureRandom().nextBytes(wrongKey);
        byte[] decrypted = crypto.decryptData(ciphertext, wrongKey, nonce, null);

        assertNull("Decryption with wrong key should fail", decrypted);
    }

    @Test
    public void testMLKEMKeypairGeneration() {
        Object[] keypair = crypto.mlkemKeypair();
        assertNotNull("Keypair should not be null", keypair);
        assertEquals("Keypair should have 2 elements", 2, keypair.length);

        byte[] publicKey = (byte[]) keypair[0];
        byte[] secretKey = (byte[]) keypair[1];

        assertNotNull("Public key should not be null", publicKey);
        assertNotNull("Secret key should not be null", secretKey);
        assertEquals("Public key should be 1568 bytes", 1568, publicKey.length);
        assertEquals("Secret key should be 3168 bytes", 3168, secretKey.length);
    }

    @Test
    public void testMLKEMEncapsulation() {
        // Generate keypair
        Object[] keypair = crypto.mlkemKeypair();
        byte[] publicKey = (byte[]) keypair[0];

        // Encapsulate
        Object[] encapsulated = crypto.mlkemEncapsulate(publicKey);
        assertNotNull("Encapsulation result should not be null", encapsulated);
        assertEquals("Encapsulation should return 2 elements", 2, encapsulated.length);

        byte[] ciphertext = (byte[]) encapsulated[0];
        byte[] sharedSecret = (byte[]) encapsulated[1];

        assertNotNull("Ciphertext should not be null", ciphertext);
        assertNotNull("Shared secret should not be null", sharedSecret);
        assertEquals("Ciphertext should be 1568 bytes", 1568, ciphertext.length);
        assertEquals("Shared secret should be 32 bytes", 32, sharedSecret.length);
    }

    @Test
    public void testMLKEMDecapsulation() {
        // Generate keypair
        Object[] keypair = crypto.mlkemKeypair();
        byte[] publicKey = (byte[]) keypair[0];
        byte[] secretKey = (byte[]) keypair[1];

        // Encapsulate
        Object[] encapsulated = crypto.mlkemEncapsulate(publicKey);
        byte[] ciphertext = (byte[]) encapsulated[0];
        byte[] originalSecret = (byte[]) encapsulated[1];

        // Decapsulate
        byte[] recoveredSecret = crypto.mlkemDecapsulate(ciphertext, secretKey);
        assertNotNull("Decapsulation should succeed", recoveredSecret);
        assertEquals("Recovered secret should be 32 bytes", 32, recoveredSecret.length);
        assertArrayEquals("Recovered secret should match original", originalSecret, recoveredSecret);
    }

    @Test
    public void testMLKEMMultipleEncapsulations() {
        // Generate one keypair
        Object[] keypair = crypto.mlkemKeypair();
        byte[] publicKey = (byte[]) keypair[0];

        // Multiple encapsulations should produce different ciphertexts but valid secrets
        Object[] enc1 = crypto.mlkemEncapsulate(publicKey);
        Object[] enc2 = crypto.mlkemEncapsulate(publicKey);

        byte[] ct1 = (byte[]) enc1[0];
        byte[] ct2 = (byte[]) enc2[0];

        assertFalse("Two encapsulations should produce different ciphertexts",
            java.util.Arrays.equals(ct1, ct2));
    }

    @Test
    public void testMLKEMWrongSecretKey() {
        // Generate two keypairs
        Object[] keypair1 = crypto.mlkemKeypair();
        Object[] keypair2 = crypto.mlkemKeypair();

        byte[] publicKey1 = (byte[]) keypair1[0];
        byte[] secretKey2 = (byte[]) keypair2[1];

        // Encapsulate with publicKey1
        Object[] encapsulated = crypto.mlkemEncapsulate(publicKey1);
        byte[] ciphertext = (byte[]) encapsulated[0];

        // Try to decapsulate with wrong secret key
        byte[] recoveredSecret = crypto.mlkemDecapsulate(ciphertext, secretKey2);
        assertNotNull("Decapsulation with wrong key still returns data", recoveredSecret);
        // Note: ML-KEM decapsulation always succeeds but returns wrong shared secret
        // This is intentional for security (prevents timing attacks)
    }

    @Test
    public void testEndToEndVolumeEncryption() {
        // Simulate volume creation and mounting
        String password = "secure_password_123";

        // Creation phase
        byte[] salt = CryptoNative.generateSalt();
        byte[] masterKey = new byte[32];
        new java.security.SecureRandom().nextBytes(masterKey);

        // Derive key from password
        byte[] pdk = crypto.deriveKeyFromPassword(password, salt);
        assertNotNull("PDK should be derived", pdk);

        // Encrypt master key
        byte[] nonce = new byte[12]; // Zero nonce for simplicity in test
        byte[] encryptedMasterKey = crypto.encryptData(masterKey, pdk, nonce, null);
        assertNotNull("Master key should be encrypted", encryptedMasterKey);

        // Mounting phase (simulate)
        byte[] pdkMount = crypto.deriveKeyFromPassword(password, salt);
        byte[] decryptedMasterKey = crypto.decryptData(encryptedMasterKey, pdkMount, nonce, null);
        assertNotNull("Master key should be decrypted", decryptedMasterKey);
        assertArrayEquals("Decrypted master key should match", masterKey, decryptedMasterKey);
    }
}
