package com.example.accessvault;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddCredentialActivity extends AppCompatActivity {
    private EditText etSiteName, etUsername, etPassword;
    private DBHelper dbHelper;
    private long credentialId = -1;

    private Button btnSave, btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_credential);
        initializeViews();
        setupDatabase();
        setupButtons();
        loadCredentialData();
    }

    private void initializeViews() {
        etSiteName = findViewById(R.id.etSiteName);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);

        btnSave = findViewById(R.id.btnSaveCredential);
        btnEdit = findViewById(R.id.ebtnEdit);
        btnDelete = findViewById(R.id.btnDeleteCredential);

        // 🔁 Add TextWatchers
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupDatabase() {
        dbHelper = new DBHelper(this);
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> saveCredential());
        btnEdit.setOnClickListener(v -> updateCredential());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void loadCredentialData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("credential_id")) {
            credentialId = intent.getLongExtra("credential_id", -1);
            String siteName = intent.getStringExtra("site_name");
            String username = intent.getStringExtra("username");
            String password = intent.getStringExtra("password");

            if (siteName != null) etSiteName.setText(siteName);
            if (username != null) etUsername.setText(username);
            if (password != null) etPassword.setText(password);

            etSiteName.setEnabled(false); // Prevent changing site name once set

            // Show Update and Delete buttons
            btnSave.setVisibility(View.GONE);
            btnEdit.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Show Save button only
            btnSave.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void saveCredential() {
        String siteName = etSiteName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs(siteName, username, password)) {
            return;
        }

        new DatabaseTask().execute(siteName, username, password);
    }

    private boolean validateInputs(String siteName, String username, String password) {
        boolean isValid = true;

        if (siteName.isEmpty()) {
            etSiteName.setError("Site name is required");
            isValid = false;
        }
        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            isValid = false;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        }

        return isValid;
    }

    private void updateCredential() {
        String siteName = etSiteName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!validateInputs(siteName, username, password)) {
            return;
        }

        new UpdateTask().execute(siteName, username, password);
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Credential")
                .setMessage("Are you sure you want to delete this credential?")
                .setPositiveButton("Delete", (dialog, which) -> new DeleteTask().execute())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class DatabaseTask extends AsyncTask<String, Void, Boolean> {
        private Exception error;

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String siteName = params[0];
                String username = params[1];
                String password = params[2];

                return dbHelper.addCredential(siteName, username, password);
            } catch (Exception e) {
                error = e;
                Log.e("SaveError", "Database operation failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(AddCredentialActivity.this,
                        "Credential saved!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String errorMessage = "Failed to save. Reason: ";
                if (error != null) {
                    errorMessage += error.getMessage();
                    if (error instanceof SQLiteConstraintException) {
                        errorMessage = "Failed to save. Site name already exists!";
                    }
                }
                Toast.makeText(AddCredentialActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdateTask extends AsyncTask<String, Void, Boolean> {
        private Exception error;

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String siteName = params[0];
                String username = params[1];
                String password = params[2];

                return dbHelper.updateCredential(credentialId, siteName, username, password);
            } catch (Exception e) {
                error = e;
                Log.e("UpdateError", "Database operation failed: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(AddCredentialActivity.this,
                        "Credential updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String errorMessage = "Failed to update. Reason: ";
                if (error != null) {
                    errorMessage += error.getMessage();
                }
                Toast.makeText(AddCredentialActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class DeleteTask extends AsyncTask<Void, Void, Boolean> {
        private Exception error;

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                dbHelper.deleteCredentialById(credentialId);
                return true;
            } catch (Exception e) {
                error = e;
                Log.e("DeleteError", "Deletion failed: " + e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(AddCredentialActivity.this, "Credential deleted", Toast.LENGTH_SHORT).show();
                finish(); // Go back to Vault
            } else {
                Toast.makeText(AddCredentialActivity.this,
                        "Failed to delete: " + (error != null ? error.getMessage() : "Unknown error"),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}