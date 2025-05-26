package com.example.accessvault;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

public class CredentialsAdapter extends ArrayAdapter<Credential> {
    private Context context;

    public CredentialsAdapter(@NonNull Context context, @NonNull List<Credential> credentials) {
        super(context, 0, credentials);
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_credential, parent, false);
        }

        Credential credential = getItem(position);

        TextView tvSiteName = view.findViewById(R.id.tvSiteNameItem);
        TextView tvUsername = view.findViewById(R.id.tvUsernameItem);
        ImageButton btnCopyUser = view.findViewById(R.id.btnCopyUser);
        ImageButton btnCopyPass = view.findViewById(R.id.btnCopyPass);
        ImageButton btnEdit = view.findViewById(R.id.ebtnEdit); // Find edit button

        tvSiteName.setText(credential.getSiteName());
        tvUsername.setText(credential.getUsername());

        // Copy username
        btnCopyUser.setOnClickListener(v -> {
            copyToClipboard(credential.getUsername(), "Username");
        });

        // Copy password
        btnCopyPass.setOnClickListener(v -> {
            copyToClipboard(credential.getPassword(), "Password");
        });

        // ✅ Edit credential
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddCredentialActivity.class);
            intent.putExtra("credential_id", credential.getId());
            intent.putExtra("site_name", credential.getSiteName());
            intent.putExtra("username", credential.getUsername());
            intent.putExtra("password", credential.getPassword());
            context.startActivity(intent);
        });

        return view;
    }

    private void copyToClipboard(String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, label + " copied", Toast.LENGTH_SHORT).show();
    }
}