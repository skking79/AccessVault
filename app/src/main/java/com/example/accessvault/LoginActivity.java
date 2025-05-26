package com.example.accessvault;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_CODE_BIOMETRIC_ENROLL = 1001;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeBiometricAuthentication();
    }

    private void initializeBiometricAuthentication() {
        executor = ContextCompat.getMainExecutor(this);

        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                showToast("Authentication error: " + errString);
                Log.e(TAG, "Auth Error: " + errorCode + " - " + errString);
                handleAuthenticationError();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                showToast("Authentication succeeded!");

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(LoginActivity.this, VaultActivity.class));
                    finish();
                }, 500); // 500ms delay to ensure smooth transition
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                showToast("Authentication failed");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.biometric_prompt_title))
                .setSubtitle(getString(R.string.biometric_prompt_subtitle))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        checkBiometricSupport();
    }

    private void checkBiometricSupport() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL ;
        switch (biometricManager.canAuthenticate(authenticators)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "Biometric authentication ready");
                biometricPrompt.authenticate(promptInfo);
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.w(TAG, "No biometric hardware detected");
                showToast(getString(R.string.error_no_biometric_hardware));
                proceedToVault(); // Optional: skip biometric if not available
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.w(TAG, "Biometric hardware unavailable");
                showToast(getString(R.string.error_biometric_unavailable));
                proceedToVault(); // Fallback to manual login
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.i(TAG, "No biometrics enrolled");
                launchBiometricEnrollment();
                break;
            default:
                Log.e(TAG, "Unexpected biometric status");
                showToast(getString(R.string.error_unknown_biometric_status));
                finish();
        }
    }

    private void launchBiometricEnrollment() {
        try {
            Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
            enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            startActivityForResult(enrollIntent, REQUEST_CODE_BIOMETRIC_ENROLL);
        } catch (Exception e) {
            Log.e(TAG, "Biometric enrollment error: " + e.getMessage());
            showToast(getString(R.string.error_launching_enrollment));
            finish();
        }
    }

    private void proceedToVault() {
        startActivity(new Intent(this, VaultActivity.class));
        finish();
    }

    private void handleAuthenticationError() {
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_BIOMETRIC_ENROLL) {
            checkBiometricSupport();
        }
    }
}