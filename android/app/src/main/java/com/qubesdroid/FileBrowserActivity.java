package com.qubesdroid;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FileBrowserActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView volumeNameText;
    private TextView emptyStateText;
    private RecyclerView filesRecyclerView;
    private MaterialButton addFileButton;
    private MaterialButton exportFileButton;
    private FloatingActionButton dismountFab;

    private String volumeName;
    private String volumePath;
    private byte[] masterKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.e("QubesDroid", "=== FileBrowserActivity onCreate START ===");

        try {
            setContentView(R.layout.activity_file_browser);
            android.util.Log.e("QubesDroid", "Layout inflated successfully");

            // Get data from intent
            volumeName = getIntent().getStringExtra("volumeName");
            volumePath = getIntent().getStringExtra("volumePath");
            masterKey = getIntent().getByteArrayExtra("masterKey");
            android.util.Log.e("QubesDroid", "Intent data retrieved: volumeName=" + volumeName);

            initializeViews();
            android.util.Log.e("QubesDroid", "Views initialized");

            setupToolbar();
            android.util.Log.e("QubesDroid", "Toolbar setup");

            setupListeners();
            android.util.Log.e("QubesDroid", "Listeners setup");

            displayVolumeInfo();
            android.util.Log.e("QubesDroid", "Volume info displayed");

            // Show empty state initially
            showEmptyState();
            android.util.Log.e("QubesDroid", "=== FileBrowserActivity onCreate COMPLETE ===");
        } catch (Exception e) {
            android.util.Log.e("QubesDroid", "FATAL ERROR in FileBrowserActivity onCreate", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        volumeNameText = findViewById(R.id.volumeNameText);
        emptyStateText = findViewById(R.id.emptyStateText);
        filesRecyclerView = findViewById(R.id.filesRecyclerView);
        addFileButton = findViewById(R.id.addFileButton);
        exportFileButton = findViewById(R.id.exportFileButton);
        dismountFab = findViewById(R.id.dismountFab);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupListeners() {
        addFileButton.setOnClickListener(v -> {
            Toast.makeText(this,
                "File encryption feature coming soon!\n\n" +
                "This will allow you to:\n" +
                "• Select files from your device\n" +
                "• Encrypt them with ChaCha20-Poly1305\n" +
                "• Store them securely in the volume",
                Toast.LENGTH_LONG).show();
        });

        exportFileButton.setOnClickListener(v -> {
            Toast.makeText(this,
                "File decryption feature coming soon!\n\n" +
                "This will allow you to:\n" +
                "• View files in the volume\n" +
                "• Decrypt and export them\n" +
                "• Save them to your device",
                Toast.LENGTH_LONG).show();
        });

        dismountFab.setOnClickListener(v -> {
            // Securely erase master key
            if (masterKey != null) {
                java.util.Arrays.fill(masterKey, (byte) 0);
            }

            Toast.makeText(this, "Volume dismounted securely", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void displayVolumeInfo() {
        if (volumeName != null) {
            volumeNameText.setText(volumeName);
            toolbar.setTitle(volumeName);
        }
    }

    private void showEmptyState() {
        emptyStateText.setVisibility(View.VISIBLE);
        filesRecyclerView.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Securely erase master key when activity is destroyed
        if (masterKey != null) {
            java.util.Arrays.fill(masterKey, (byte) 0);
        }
    }

    @Override
    public void onBackPressed() {
        // Securely dismount before going back
        if (masterKey != null) {
            java.util.Arrays.fill(masterKey, (byte) 0);
        }
        super.onBackPressed();
    }
}
