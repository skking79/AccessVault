package com.example.accessvault;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat switchBiometric;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        switchBiometric = findViewById(R.id.switchBiometric);
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        switchBiometric.setChecked(prefs.getBoolean("biometric_enabled", true));

        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("biometric_enabled", isChecked).apply();
            Toast.makeText(this, "Biometric setting updated", Toast.LENGTH_SHORT).show();
        });
    }
}