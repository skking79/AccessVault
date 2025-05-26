package com.example.accessvault;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class VaultActivity extends AppCompatActivity {
    private ListView listViewCredentials;
    private TextView tvEmptyVault;
    private CredentialsAdapter adapter;
    private DBHelper dbHelper;
    private Credential selectedCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        listViewCredentials = findViewById(R.id.listViewCredentials);
        tvEmptyVault = findViewById(R.id.tvEmptyVault);
        FloatingActionButton btnAddCredential = findViewById(R.id.btnAddCredential);

        dbHelper = new DBHelper(this);

        btnAddCredential.setOnClickListener(v ->
                startActivity(new Intent(VaultActivity.this, AddCredentialActivity.class))
        );




        loadCredentials();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Password", text);
        clipboard.setPrimaryClip(clip);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        selectedCredential = adapter.getItem(info.position);
        if (selectedCredential != null) {
            menu.setHeaderTitle(selectedCredential.getSiteName());
            menu.add(0, 0, 0, "Edit");
            menu.add(0, 0, 0, "Delete");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (selectedCredential == null) return false;

        if (item.getTitle().equals("Edit")) {
            editCredential(selectedCredential);
            return true;
        } else if (item.getTitle().equals("Delete")) {
            confirmDelete(selectedCredential);
            return true;
        }
        return false;
    }

    private void editCredential(Credential credential) {
        Intent intent = new Intent(this, AddCredentialActivity.class);
        intent.putExtra("credential_id", credential.getId());
        intent.putExtra("site_name", credential.getSiteName());
        intent.putExtra("username", credential.getUsername());
        intent.putExtra("password", credential.getPassword());
        startActivity(intent);
    }

    private void confirmDelete(Credential credential) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Credential")
                .setMessage("Are you sure you want to delete this credential?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deleteCredential(credential.getId());
                    loadCredentials();
                    Toast.makeText(this, "Credential deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadCredentials() {
        try {
            List<Credential> credentials = dbHelper.getAllCredentials();

            if (credentials == null || credentials.isEmpty()) {
                showEmptyState();
            } else {
                showCredentialsList(credentials);
            }
        } catch (Exception e) {
            Log.e("VaultActivity", "Error loading credentials: " + e.getMessage());
            showEmptyState();
            Toast.makeText(this, "Failed to load credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyState() {
        runOnUiThread(() -> {
            tvEmptyVault.setVisibility(View.VISIBLE);
            listViewCredentials.setVisibility(View.GONE);
            if (adapter != null) {
                adapter.clear();
            }
        });
    }

    private void showCredentialsList(List<Credential> credentials) {
        runOnUiThread(() -> {
            tvEmptyVault.setVisibility(View.GONE);
            listViewCredentials.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new CredentialsAdapter(this, credentials);
                listViewCredentials.setAdapter(adapter);
            } else {
                adapter.clear();
                adapter.addAll(credentials);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCredentials(); // Refresh the list
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}