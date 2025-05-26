package com.example.accessvault;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "accessvault.db";
    private static final int DATABASE_VERSION = 2;
    public static final String TABLE_CREDENTIALS = "credentials";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SITE_NAME = "site_name";
    public static final String COLUMN_ENCRYPTED_DATA = "encrypted_data";

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "AccessVaultKey";
    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final int IV_SIZE = 16;
    private static final String TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        try {
            getOrCreateSecretKey(KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Keystore key initialization failed", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_CREDENTIALS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SITE_NAME + " TEXT NOT NULL UNIQUE,"
                + COLUMN_ENCRYPTED_DATA + " TEXT NOT NULL" + ")";
        db.execSQL(CREATE_TABLE);
    }
    public boolean updateCredential(long id, String siteName, String username, String password) {
        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_SITE_NAME, siteName.trim());

            String combinedData = URLEncoder.encode(username, "UTF-8")
                    + ":" + URLEncoder.encode(password, "UTF-8");

            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, null);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal(combinedData.getBytes(StandardCharsets.UTF_8));

            String ivString = Base64.encodeToString(iv, Base64.NO_WRAP);
            String encryptedDataString = Base64.encodeToString(encryptedData, Base64.NO_WRAP);
            values.put(COLUMN_ENCRYPTED_DATA, ivString + ":" + encryptedDataString);

            int rowsAffected = db.update(TABLE_CREDENTIALS, values,
                    COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            return rowsAffected > 0;
        } catch (Exception e) {
            Log.e(TAG, "Update failed: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    public boolean deleteCredentialById(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_CREDENTIALS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREDENTIALS);
        onCreate(db);
    }

    private SecretKey getOrCreateSecretKey(String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        if (!keyStore.containsAlias(alias)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(true);
            keyGenerator.init(builder.build());
            return keyGenerator.generateKey();
        }
        return (SecretKey) keyStore.getKey(alias, null);
    }

    private Cipher getCipher(int mode, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_MODE);
        SecretKey secretKey = getOrCreateSecretKey(KEY_ALIAS);
        if (mode == Cipher.ENCRYPT_MODE) {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        } else {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        }
        return cipher;
    }

    public boolean addCredential(String siteName, String username, String password) {
        SQLiteDatabase db = null;
        try {
            Log.d(TAG, "Saving credential - Site: " + siteName + ", User: " + username);

            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, null);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            ContentValues values = new ContentValues();
            values.put(COLUMN_SITE_NAME, siteName.trim());
            values.put(COLUMN_ENCRYPTED_DATA, Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encryptedData, Base64.NO_WRAP));

            db = this.getWritableDatabase();
            long result = db.insertWithOnConflict(TABLE_CREDENTIALS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
            return false;
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }
    public boolean updateOrInsertCredential(String siteName, String username, String password) {
        try {
            Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, null);
            byte[] iv = cipher.getIV();
            byte[] encryptedData = cipher.doFinal((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            String encryptedValue = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
                    Base64.encodeToString(encryptedData, Base64.NO_WRAP);

            ContentValues values = new ContentValues();
            values.put(COLUMN_SITE_NAME, siteName);
            values.put(COLUMN_ENCRYPTED_DATA, encryptedValue);

            SQLiteDatabase db = this.getWritableDatabase();
            long existingId = getCredentialBySiteName(db, siteName).getId();

            if (existingId != -1) {
                int rowsAffected = db.update(TABLE_CREDENTIALS, values,
                        COLUMN_ID + " = ?", new String[]{String.valueOf(existingId)});
                db.close();
                return rowsAffected > 0;
            } else {
                long result = db.insert(TABLE_CREDENTIALS, null, values);
                db.close();
                return result != -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
            return false;
        }
    }
    private long getExistingCredentialId(SQLiteDatabase db, String siteName) {
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CREDENTIALS,
                    new String[]{COLUMN_ID},
                    COLUMN_SITE_NAME + " = ?",
                    new String[]{siteName},
                    null, null, null);
            return cursor.moveToFirst() ? cursor.getLong(0) : -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public List<Credential> getAllCredentials() {
        List<Credential> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            cursor = db.query(TABLE_CREDENTIALS, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                        String siteName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SITE_NAME));
                        String encryptedValue = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCRYPTED_DATA));
                        String[] parts = encryptedValue.split(":", 2);
                        if (parts.length != 2) {
                            Log.w(TAG, "Invalid encrypted data format");
                            continue;
                        }
                        byte[] iv = Base64.decode(parts[0], Base64.DEFAULT);
                        byte[] encryptedData = Base64.decode(parts[1], Base64.DEFAULT);
                        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, iv);
                        byte[] decryptedBytes = cipher.doFinal(encryptedData);
                        String decryptedCombinedData = new String(decryptedBytes, StandardCharsets.UTF_8);
                        String[] userPassParts = decryptedCombinedData.split(":", 2);
                        String username = "";
                        String password = "";
                        if (userPassParts.length >= 1) username = userPassParts[0];
                        if (userPassParts.length >= 2) password = userPassParts[1];
                        list.add(new Credential(id, siteName, username, password));
                    } catch (Exception e) {
                        Log.e(TAG, "Error decrypting data for row " + cursor.getPosition(), e);
                        continue;
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Database error", e);
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return list;
    }

    public Credential getCredentialBySiteName(String siteName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_CREDENTIALS,
                new String[]{COLUMN_ID, COLUMN_SITE_NAME, COLUMN_ENCRYPTED_DATA},
                COLUMN_SITE_NAME + "=?",
                new String[]{siteName}, null, null, null);
        Credential credential = null;
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String storedEncryptedString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCRYPTED_DATA));
                    String[] parts = storedEncryptedString.split(":", 2);
                    if (parts.length == 2) {
                        byte[] iv = Base64.decode(parts[0], Base64.DEFAULT);
                        byte[] encryptedData = Base64.decode(parts[1], Base64.DEFAULT);
                        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, iv);
                        byte[] decryptedDataBytes = cipher.doFinal(encryptedData);
                        String decryptedCombinedData = new String(decryptedDataBytes, StandardCharsets.UTF_8);
                        String[] userPassParts = decryptedCombinedData.split(":", 2);
                        String username = userPassParts.length > 0 ? userPassParts[0] : "";
                        String password = userPassParts.length > 1 ? userPassParts[1] : "";
                        credential = new Credential(id, siteName, username, password);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error retrieving credential", e);
            } finally {
                cursor.close();
            }
        }
        db.close();
        return credential;
    }
    public Credential getCredentialBySiteName(SQLiteDatabase db, String siteName) {
        Cursor cursor = db.query(TABLE_CREDENTIALS,
                new String[]{COLUMN_ID, COLUMN_SITE_NAME, COLUMN_ENCRYPTED_DATA},
                COLUMN_SITE_NAME + "=?", new String[]{siteName}, null, null, null);

        Credential credential = null;
        if (cursor != null && cursor.moveToFirst()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String encryptedData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ENCRYPTED_DATA));

            // Optional: Decrypt data here and get username/password
            credential = new Credential(id, siteName, "", "");  // Modify based on actual decryption logic
            cursor.close();
        }
        return credential;
    }
    public boolean deleteCredential(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_CREDENTIALS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return result > 0;
    }
}