package com.qubesdroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;

/**
 * QubesDroid - Post-Quantum Encrypted Volume Manager
 *
 * Main activity providing access to:
 * - Create new encrypted volumes
 * - Mount existing volumes
 * - Browse encrypted files
 * - Settings
 */
public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private CryptoNative crypto;
    private MaterialToolbar toolbar;
    private TextView versionText;
    private MaterialButton createVolumeButton;
    private MaterialButton mountVolumeButton;
    private MaterialCardView recentVolumesCard;
    private ExtendedFloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize crypto native library
        crypto = new CryptoNative();

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        versionText = findViewById(R.id.versionText);
        createVolumeButton = findViewById(R.id.createVolumeButton);
        mountVolumeButton = findViewById(R.id.mountVolumeButton);
        recentVolumesCard = findViewById(R.id.recentVolumesCard);
        fab = findViewById(R.id.fab);

        // Display version info
        String cryptoVersion = crypto.getVersionInfo();
        versionText.setText("QubesDroid v1.0.0-alpha\n" + cryptoVersion);

        // Set up button listeners
        createVolumeButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startActivity(new Intent(this, CreateVolumeActivity.class));
            }
        });

        mountVolumeButton.setOnClickListener(v -> {
            if (checkPermissions()) {
                startActivity(new Intent(this, MountVolumeActivity.class));
            }
        });

        fab.setOnClickListener(v -> {
            if (checkPermissions()) {
                startActivity(new Intent(this, CreateVolumeActivity.class));
            }
        });

        // Check permissions on startup
        checkPermissions();

        // Load recent volumes if any exist
        loadRecentVolumes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload recent volumes when returning to activity
        loadRecentVolumes();
    }

    private void loadRecentVolumes() {
        File volumesDir = new File(getExternalFilesDir(null), "volumes");
        if (volumesDir.exists() && volumesDir.listFiles() != null) {
            File[] volumes = volumesDir.listFiles((dir, name) -> name.endsWith(".qd"));
            if (volumes != null && volumes.length > 0) {
                recentVolumesCard.setVisibility(android.view.View.VISIBLE);
                // In a full implementation, we would populate the RecyclerView here
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("About QubesDroid")
            .setMessage("QubesDroid v1.0.0-alpha\n\n" +
                "Post-Quantum Encrypted Volume Manager\n\n" +
                "Cryptography:\n" +
                "• ML-KEM-1024 (Post-Quantum KEM)\n" +
                "• ChaCha20-Poly1305 (AEAD)\n" +
                "• Argon2id (Password Hashing)\n" +
                "• BLAKE2s-256 (Hashing)\n\n" +
                "\u00a9 2025 QubesDroid Project")
            .setPositiveButton("OK", null)
            .show();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                Toast.makeText(this,
                    "Please grant file access permission in settings",
                    Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return false;
            }
        } else {
            // Android 10 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                loadRecentVolumes();
            } else {
                Toast.makeText(this,
                    "Storage permission is required for QubesDroid to function",
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
