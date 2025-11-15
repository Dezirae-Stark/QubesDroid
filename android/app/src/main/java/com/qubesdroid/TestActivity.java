package com.qubesdroid;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class TestActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this, "TEST ACTIVITY LAUNCHED!", Toast.LENGTH_LONG).show();

        TextView textView = new TextView(this);
        textView.setText("TEST ACTIVITY IS WORKING!\n\nThis proves activity launching works.");
        textView.setTextSize(24);
        textView.setPadding(50, 50, 50, 50);
        setContentView(textView);
    }
}
