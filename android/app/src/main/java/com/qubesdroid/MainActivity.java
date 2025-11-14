package com.qubesdroid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private TextView versionText;
    private Button createVolumeButton;
    private Button mountVolumeButton;
    private Button settingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize crypto native library
        crypto = new CryptoNative();

        // Initialize UI components
        versionText = findViewById(R.id.version_text);
        createVolumeButton = findViewById(R.id.btn_create_volume);
        mountVolumeButton = findViewById(R.id.btn_mount_volume);
        settingsButton = findViewById(R.id.btn_settings);

        // Display version info
        versionText.setText(crypto.getVersionInfo());

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

        settingsButton.setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        // Check permissions on startup
        checkPermissions();
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
            } else {
                Toast.makeText(this,
                    "Storage permission is required for QubesDroid to function",
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}
