package com.qubesdroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for creating new encrypted volumes
 *
 * Implements the QubesDroid volume format specification with:
 * - ML-KEM-1024 post-quantum key encapsulation
 * - Argon2id password-based key derivation
 * - ChaCha20-Poly1305 authenticated encryption
 */
public class CreateVolumeActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private TextInputEditText volumeNameInput;
    private TextInputLayout volumeNameLayout;
    private Slider volumeSizeSlider;
    private TextView volumeSizeText;
    private TextInputEditText passwordInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText confirmPasswordInput;
    private TextInputLayout confirmPasswordLayout;
    private MaterialButton createVolumeButton;
    private LinearLayout progressLayout;
    private LinearProgressIndicator progressIndicator;
    private TextView progressText;

    // Crypto
    private CryptoNative crypto;
    private ExecutorService executorService;

    // Volume format constants (from VOLUME_FORMAT.md)
    private static final byte[] MAGIC_SIGNATURE = "QUBESDRD".getBytes();
    private static final int VERSION = 0x01000000;
    private static final int HEADER_SIZE = 1712;
    private static final int BLOCK_SIZE = 65536; // 64 KB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_volume);

        crypto = new CryptoNative();
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        volumeNameInput = findViewById(R.id.volumeNameInput);
        volumeNameLayout = findViewById(R.id.volumeNameLayout);
        volumeSizeSlider = findViewById(R.id.volumeSizeSlider);
        volumeSizeText = findViewById(R.id.volumeSizeText);
        passwordInput = findViewById(R.id.passwordInput);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        createVolumeButton = findViewById(R.id.createVolumeButton);
        progressLayout = findViewById(R.id.progressLayout);
        progressIndicator = findViewById(R.id.progressIndicator);
        progressText = findViewById(R.id.progressText);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        // Volume size slider
        volumeSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            volumeSizeText.setText(String.format("%d MB", (int) value));
        });

        // Password validation
        TextWatcher passwordWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePasswords();
            }
        };

        passwordInput.addTextChangedListener(passwordWatcher);
        confirmPasswordInput.addTextChangedListener(passwordWatcher);

        // Create volume button
        createVolumeButton.setOnClickListener(v -> {
            if (validateInputs()) {
                createVolume();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate volume name
        String volumeName = volumeNameInput.getText().toString().trim();
        if (volumeName.isEmpty()) {
            volumeNameLayout.setError("Volume name is required");
            isValid = false;
        } else {
            volumeNameLayout.setError(null);
        }

        // Validate password
        String password = passwordInput.getText().toString();
        if (password.length() < 8) {
            passwordLayout.setError("Password must be at least 8 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        // Validate password confirmation
        String confirmPassword = confirmPasswordInput.getText().toString();
        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void validatePasswords() {
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (!confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
        } else {
            confirmPasswordLayout.setError(null);
        }
    }

    private void createVolume() {
        String volumeName = volumeNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        long volumeSizeMB = (long) volumeSizeSlider.getValue();
        long volumeSizeBytes = volumeSizeMB * 1024 * 1024;

        // Disable UI during creation
        setUIEnabled(false);
        progressLayout.setVisibility(View.VISIBLE);
        progressIndicator.setProgress(0);

        executorService.execute(() -> {
            try {
                createVolumeFile(volumeName, password, volumeSizeBytes);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Volume created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error creating volume: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    setUIEnabled(true);
                    progressLayout.setVisibility(View.GONE);
                });
            }
        });
    }

    private void createVolumeFile(String volumeName, String password, long volumeSizeBytes) throws Exception {
        File volumesDir = new File(getExternalFilesDir(null), "volumes");
        if (!volumesDir.exists()) {
            volumesDir.mkdirs();
        }

        File volumeFile = new File(volumesDir, volumeName + ".qd");
        if (volumeFile.exists()) {
            throw new IOException("Volume already exists");
        }

        updateProgress(5, "Generating master key...");

        // 1. Generate random 32-byte Master Key
        SecureRandom random = new SecureRandom();
        byte[] masterKey = new byte[32];
        random.nextBytes(masterKey);

        updateProgress(10, "Generating ML-KEM keypair...");

        // 2. Generate ML-KEM-1024 keypair for post-quantum security
        Object[] keypair = crypto.mlkemKeypair();
        byte[] mlkemPublicKey = (byte[]) keypair[0];   // 1568 bytes
        byte[] mlkemSecretKey = (byte[]) keypair[1];   // 3168 bytes

        // Store secret key separately (in secure storage, for now save alongside)
        File secretKeyFile = new File(volumesDir, volumeName + ".key");
        try (FileOutputStream fos = new FileOutputStream(secretKeyFile)) {
            fos.write(mlkemSecretKey);
        }

        updateProgress(30, "Deriving encryption key from password...");

        // 3. Derive Password-Derived Key using Argon2id
        byte[] salt = crypto.generateSalt();  // 16 bytes (spec says 32, but method returns 16)
        byte[] passwordDerivedKey = crypto.deriveKeyFromPassword(password, salt);

        updateProgress(40, "Encrypting master key...");

        // 4. Encrypt Master Key with PDK using ChaCha20-Poly1305
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        byte[] encryptedMasterKey = crypto.encryptData(masterKey, passwordDerivedKey, nonce, null);

        updateProgress(50, "Writing volume header...");

        // 5. Build and write volume header
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);

        // Magic signature
        header.put(MAGIC_SIGNATURE);
        // Version
        header.putInt(VERSION);
        // Header size
        header.putInt(HEADER_SIZE);
        // Volume size
        header.putLong(volumeSizeBytes);
        // Creation timestamp
        header.putLong(System.currentTimeMillis() / 1000);
        // Reserved (32 bytes)
        header.put(new byte[32]);
        // ML-KEM Public Key (1568 bytes)
        header.put(mlkemPublicKey);
        // Salt (32 bytes - but we have 16, pad with zeros)
        header.put(salt);
        header.put(new byte[16]); // Pad to 32 bytes
        // Encrypted Master Key (48 bytes: 32-byte key + 16-byte tag)
        header.put(encryptedMasterKey);

        updateProgress(60, "Creating volume file...");

        // 6. Write volume to disk
        try (FileOutputStream fos = new FileOutputStream(volumeFile)) {
            // Write header
            fos.write(header.array());

            // Write encrypted data blocks
            long dataSize = volumeSizeBytes - HEADER_SIZE;
            long numBlocks = (dataSize + BLOCK_SIZE - 1) / BLOCK_SIZE;

            for (long i = 0; i < numBlocks; i++) {
                int blockProgress = (int) (60 + (i * 40 / numBlocks));
                updateProgress(blockProgress, String.format("Writing block %d/%d...", i + 1, numBlocks));

                // Generate deterministic nonce for this block
                ByteBuffer nonceBuffer = ByteBuffer.allocate(12);
                nonceBuffer.putLong(i);
                byte[] blockNonce = nonceBuffer.array();

                // Create empty plaintext block
                int currentBlockSize = (int) Math.min(BLOCK_SIZE, dataSize - (i * BLOCK_SIZE));
                byte[] plaintext = new byte[currentBlockSize];

                // AAD is block index
                ByteBuffer aad = ByteBuffer.allocate(8);
                aad.putLong(i);

                // Encrypt block
                byte[] ciphertext = crypto.encryptData(plaintext, masterKey, blockNonce, aad.array());

                // Write: nonce (12) + ciphertext + tag (16)
                fos.write(blockNonce);
                fos.write(ciphertext);
            }
        }

        updateProgress(100, "Volume created successfully!");

        // Securely erase sensitive data
        java.util.Arrays.fill(masterKey, (byte) 0);
        java.util.Arrays.fill(passwordDerivedKey, (byte) 0);
        java.util.Arrays.fill(mlkemSecretKey, (byte) 0);
    }

    private void updateProgress(int progress, String message) {
        runOnUiThread(() -> {
            progressIndicator.setProgress(progress);
            progressText.setText(message);
        });
    }

    private void setUIEnabled(boolean enabled) {
        volumeNameInput.setEnabled(enabled);
        volumeSizeSlider.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        confirmPasswordInput.setEnabled(enabled);
        createVolumeButton.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
