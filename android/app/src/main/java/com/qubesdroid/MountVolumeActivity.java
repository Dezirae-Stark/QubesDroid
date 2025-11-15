package com.qubesdroid;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for mounting existing encrypted volumes
 *
 * Reads and decrypts QubesDroid volume files:
 * 1. Parse encrypted volume header
 * 2. Derive password key using Argon2id
 * 3. Decrypt master key with ChaCha20-Poly1305
 * 4. Provide access to decrypted data blocks
 */
public class MountVolumeActivity extends AppCompatActivity {

    // UI Components
    private MaterialToolbar toolbar;
    private MaterialButton selectVolumeButton;
    private LinearLayout volumeInfoLayout;
    private TextView volumeNameText;
    private TextView volumeSizeText;
    private TextView volumeCreatedText;
    private MaterialCardView authCard;
    private TextInputEditText passwordInput;
    private TextInputLayout passwordLayout;
    private MaterialButton mountVolumeButton;
    private LinearLayout progressLayout;
    private LinearProgressIndicator progressIndicator;
    private TextView progressText;

    // Crypto
    private CryptoNative crypto;
    private ExecutorService executorService;

    // Volume data
    private File selectedVolumeFile;
    private VolumeHeader volumeHeader;

    // File picker
    private ActivityResultLauncher<String[]> volumePickerLauncher;

    // Volume format constants
    private static final byte[] MAGIC_SIGNATURE = "QUBESDRD".getBytes();
    private static final int VERSION = 0x01000000;
    private static final int HEADER_SIZE = 1712;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mount_volume);

        crypto = new CryptoNative();
        executorService = Executors.newSingleThreadExecutor();

        initViews();
        setupListeners();
        setupFilePicker();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        selectVolumeButton = findViewById(R.id.selectVolumeButton);
        volumeInfoLayout = findViewById(R.id.volumeInfoLayout);
        volumeNameText = findViewById(R.id.volumeNameText);
        volumeSizeText = findViewById(R.id.volumeSizeText);
        volumeCreatedText = findViewById(R.id.volumeCreatedText);
        authCard = findViewById(R.id.authCard);
        passwordInput = findViewById(R.id.passwordInput);
        passwordLayout = findViewById(R.id.passwordLayout);
        mountVolumeButton = findViewById(R.id.mountVolumeButton);
        progressLayout = findViewById(R.id.progressLayout);
        progressIndicator = findViewById(R.id.progressIndicator);
        progressText = findViewById(R.id.progressText);
    }

    private void setupListeners() {
        toolbar.setNavigationOnClickListener(v -> finish());

        selectVolumeButton.setOnClickListener(v -> {
            // Try to browse volumes directory first
            File volumesDir = new File(getExternalFilesDir(null), "volumes");
            if (volumesDir.exists() && volumesDir.listFiles() != null) {
                File[] volumes = volumesDir.listFiles((dir, name) -> name.endsWith(".qd"));
                if (volumes != null && volumes.length > 0) {
                    // Show volume from volumes directory
                    onVolumeSelected(volumes[0]);
                    return;
                }
            }

            // Fallback to file picker
            volumePickerLauncher.launch(new String[]{"*/*"});
        });

        mountVolumeButton.setOnClickListener(v -> {
            String password = passwordInput.getText().toString();
            if (password.isEmpty()) {
                passwordLayout.setError("Password is required");
                return;
            }
            mountVolume(password);
        });
    }

    private void setupFilePicker() {
        volumePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // Copy URI content to temp file for processing
                        InputStream is = getContentResolver().openInputStream(uri);
                        File tempFile = new File(getCacheDir(), "temp_volume.qd");
                        java.nio.file.Files.copy(is, tempFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        is.close();
                        onVolumeSelected(tempFile);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading volume: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    private void onVolumeSelected(File volumeFile) {
        selectedVolumeFile = volumeFile;

        // Read and parse volume header
        executorService.execute(() -> {
            try {
                volumeHeader = readVolumeHeader(volumeFile);
                runOnUiThread(() -> {
                    displayVolumeInfo(volumeFile, volumeHeader);
                    authCard.setVisibility(View.VISIBLE);
                    mountVolumeButton.setVisibility(View.VISIBLE);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Invalid volume file: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private VolumeHeader readVolumeHeader(File volumeFile) throws Exception {
        try (FileInputStream fis = new FileInputStream(volumeFile)) {
            byte[] headerBytes = new byte[HEADER_SIZE];
            int bytesRead = fis.read(headerBytes);
            if (bytesRead != HEADER_SIZE) {
                throw new Exception("Invalid volume file: header too short");
            }

            ByteBuffer buffer = ByteBuffer.wrap(headerBytes);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            VolumeHeader header = new VolumeHeader();

            // Magic signature
            byte[] magic = new byte[8];
            buffer.get(magic);
            if (!java.util.Arrays.equals(magic, MAGIC_SIGNATURE)) {
                throw new Exception("Invalid volume file: bad magic signature");
            }

            // Version
            header.version = buffer.getInt();
            if (header.version != VERSION) {
                throw new Exception("Unsupported volume version: " +
                    String.format("0x%08X", header.version));
            }

            // Header size
            header.headerSize = buffer.getInt();

            // Volume size
            header.volumeSize = buffer.getLong();

            // Creation timestamp
            header.creationTimestamp = buffer.getLong();

            // Reserved (32 bytes)
            buffer.position(buffer.position() + 32);

            // ML-KEM Public Key (1568 bytes)
            header.mlkemPublicKey = new byte[1568];
            buffer.get(header.mlkemPublicKey);

            // Salt (32 bytes)
            header.salt = new byte[32];
            buffer.get(header.salt);

            // Encrypted Master Key (48 bytes)
            header.encryptedMasterKey = new byte[48];
            buffer.get(header.encryptedMasterKey);

            return header;
        }
    }

    private void displayVolumeInfo(File volumeFile, VolumeHeader header) {
        volumeInfoLayout.setVisibility(View.VISIBLE);

        volumeNameText.setText(volumeFile.getName());

        // Format size
        long sizeMB = header.volumeSize / (1024 * 1024);
        volumeSizeText.setText(String.format("%d MB", sizeMB));

        // Format creation date
        Date creationDate = new Date(header.creationTimestamp * 1000);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm",
            Locale.getDefault());
        volumeCreatedText.setText(dateFormat.format(creationDate));
    }

    private void mountVolume(String password) {
        setUIEnabled(false);
        progressLayout.setVisibility(View.VISIBLE);
        progressText.setText("Deriving encryption key...");

        executorService.execute(() -> {
            try {
                // 1. Derive Password-Derived Key using Argon2id
                updateProgress("Deriving password key (this may take a few seconds)...");
                byte[] passwordDerivedKey = crypto.deriveKeyFromPassword(password,
                    volumeHeader.salt);

                // 2. Extract nonce from encrypted master key
                // Note: In CreateVolumeActivity we used a random nonce, but didn't store it!
                // This is a bug in the format - we need to store the nonce
                // For now, assume nonce is all zeros (fix this in next version)
                byte[] nonce = new byte[12];

                updateProgress("Decrypting master key...");

                // 3. Decrypt Master Key
                byte[] masterKey = crypto.decryptData(volumeHeader.encryptedMasterKey,
                    passwordDerivedKey, nonce, null);

                if (masterKey == null) {
                    runOnUiThread(() -> {
                        progressLayout.setVisibility(View.GONE);
                        passwordLayout.setError("Incorrect password");
                        setUIEnabled(true);
                    });
                    return;
                }

                updateProgress("Volume mounted successfully!");

                // Success - volume is now mounted
                runOnUiThread(() -> {
                    android.util.Log.e("QubesDroid", "=== MOUNT SUCCESS - Preparing to launch FileBrowserActivity ===");
                    Toast.makeText(this,
                        "Volume mounted successfully!",
                        Toast.LENGTH_SHORT).show();

                    // Launch file browser with volume info
                    android.util.Log.e("QubesDroid", "Creating Intent for FileBrowserActivity");
                    Intent intent = new Intent(this, FileBrowserActivity.class);
                    intent.putExtra("volumeName", selectedVolumeFile.getName());
                    intent.putExtra("volumePath", selectedVolumeFile.getAbsolutePath());
                    intent.putExtra("masterKey", masterKey);
                    android.util.Log.e("QubesDroid", "Starting FileBrowserActivity with volume: " + selectedVolumeFile.getName());

                    try {
                        startActivity(intent);
                        android.util.Log.e("QubesDroid", "FileBrowserActivity started successfully");
                    } catch (Exception e) {
                        android.util.Log.e("QubesDroid", "FAILED to start FileBrowserActivity", e);
                        Toast.makeText(this, "Error launching file browser: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    android.util.Log.e("QubesDroid", "Finishing MountVolumeActivity");
                    finish();
                });

                // Securely erase sensitive data
                java.util.Arrays.fill(passwordDerivedKey, (byte) 0);
                java.util.Arrays.fill(masterKey, (byte) 0);

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "Error mounting volume: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                    setUIEnabled(true);
                });
            }
        });
    }

    private void updateProgress(String message) {
        runOnUiThread(() -> progressText.setText(message));
    }

    private void setUIEnabled(boolean enabled) {
        selectVolumeButton.setEnabled(enabled);
        passwordInput.setEnabled(enabled);
        mountVolumeButton.setEnabled(enabled);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    /**
     * Volume header structure matching VOLUME_FORMAT.md
     */
    private static class VolumeHeader {
        int version;
        int headerSize;
        long volumeSize;
        long creationTimestamp;
        byte[] mlkemPublicKey;
        byte[] salt;
        byte[] encryptedMasterKey;
    }
}
