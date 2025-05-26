package com.example.accessvault;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.assist.AssistStructure;
import android.content.ComponentName;
import android.os.Build;
import android.os.CancellationSignal;
import android.service.autofill.AutofillService;
import android.service.autofill.Dataset;
import android.service.autofill.FillCallback;
import android.service.autofill.FillContext;
import android.service.autofill.FillRequest;
import android.service.autofill.FillResponse;
import android.service.autofill.SaveCallback;
import android.service.autofill.SaveRequest;
import android.text.InputType;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MyAutofillService extends AutofillService {

    private static final String TAG = "MyAutofillService";
    private DBHelper dbHelper;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(getApplicationContext());
        Log.d(TAG, "Autofill Service Created");
    }

    @Override
    public void onFillRequest(@NonNull FillRequest request, @NonNull CancellationSignal cancellationSignal, @NonNull FillCallback callback) {
        Log.d(TAG, "onFillRequest called");

        executor.execute(() -> {
            try {
                List<FillContext> fillContexts = request.getFillContexts();
                if (fillContexts == null || fillContexts.isEmpty()) {
                    callback.onSuccess(null);
                    return;
                }

                AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
                String packageName = structure.getActivityComponent().getPackageName();

                AutofillHelper.ParsedStructure parsedStructure = AutofillHelper.parseStructureForFill(structure, packageName);

                if (parsedStructure != null && parsedStructure.isCompleteForFill()) {
                    Credential credential = dbHelper.getCredentialBySiteName(parsedStructure.detectedSiteIdentifier);
                    if (credential != null) {
                        RemoteViews presentation = createRemoteViews(
                                credential.getUsername() + "\n" + credential.getPassword()
                        );

                        Dataset dataset = new Dataset.Builder(presentation)
                                .setValue(parsedStructure.usernameId, AutofillValue.forText(credential.getUsername()))
                                .setValue(parsedStructure.passwordId, AutofillValue.forText(credential.getPassword()))
                                .setId(credential.getSiteName())
                                .build();

                        FillResponse response = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            response = new FillResponse.Builder()
                                    .addDataset(dataset)
                                    .setFlags(FillResponse.FLAG_DELAY_FILL)
                                    .build();
                        }

                        callback.onSuccess(response);
                        Log.d(TAG, "Filled dataset for: " + credential.getSiteName());
                        return;
                    } else {
                        Log.w(TAG, "No matching credential found for: " + parsedStructure.detectedSiteIdentifier);
                    }
                } else {
                    Log.d(TAG, "Incomplete structure: usernameId=" +
                            (parsedStructure != null ? parsedStructure.usernameId : "null") +
                            ", passwordId=" +
                            (parsedStructure != null ? parsedStructure.passwordId : "null"));
                }

                callback.onSuccess(null);
            } catch (Exception e) {
                Log.e(TAG, "Fill request failed", e);
                callback.onFailure("Fill request failed: " + e.getMessage());
            }
        });
    }
    private static void traverseViewNodes(AssistStructure.ViewNode node, AutofillHelper.ParsedStructure parsed) {
        if (node == null) return;

        CharSequence hintText = node.getHint();
        String hintStr = hintText != null ? hintText.toString().toLowerCase(Locale.ROOT) : "";

        // Try autofillHints
        if (node.getAutofillHints() != null && node.getAutofillHints().length > 0) {
            for (CharSequence hint : node.getAutofillHints()) {
                if (hint.toString().contains("username")) {
                    parsed.usernameId = node.getAutofillId();
                } else if (hint.toString().contains("password")) {
                    parsed.passwordId = node.getAutofillId();
                }
            }
        }

        // Fallback: check input type
        int inputType = node.getInputType();
        if ((inputType & InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
                (inputType & InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0) {
            parsed.usernameId = node.getAutofillId();
        } else if ((inputType & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType & InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0) {
            parsed.passwordId = node.getAutofillId();
        }

        // Recursively check children
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseViewNodes(node.getChildAt(i), parsed);
        }
    }
    @Override
    public void onSaveRequest(@NonNull SaveRequest request, @NonNull SaveCallback callback) {
        Log.d(TAG, "onSaveRequest called");

        if (isAddCredentialActivityActive()) {
            Log.d(TAG, "Ignoring save request from AddCredentialActivity");
            callback.onSuccess();
            return;
        }

        executor.execute(() -> {
            try {
                List<FillContext> fillContexts = request.getFillContexts();
                if (fillContexts == null || fillContexts.isEmpty()) {
                    callback.onFailure("No fill contexts");
                    return;
                }

                AssistStructure structure = fillContexts.get(fillContexts.size() - 1).getStructure();
                String packageName = structure.getActivityComponent().getPackageName();

                Credential credentialToSave = AutofillHelper.parseStructureForSave(structure, packageName);

                if (credentialToSave != null &&
                        credentialToSave.getSiteName() != null &&
                        credentialToSave.getUsername() != null &&
                        credentialToSave.getPassword() != null) {

                    boolean success = dbHelper.updateOrInsertCredential(
                            credentialToSave.getSiteName(),
                            credentialToSave.getUsername(),
                            credentialToSave.getPassword());

                    if (success) {
                        Log.d(TAG, "Credential saved successfully for: " + credentialToSave.getSiteName());
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Database error during save");
                    }
                } else {
                    callback.onFailure("Incomplete or invalid credential data");
                }
            } catch (Exception e) {
                Log.e(TAG, "Save request error", e);
                callback.onFailure("Error during save: " + e.getMessage());
            }
        });
    }

    private boolean isAddCredentialActivityActive() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            if (am != null) {
                List<ActivityManager.AppTask> tasks = am.getAppTasks();
                if (tasks != null && !tasks.isEmpty()) {
                    ComponentName componentName = tasks.get(0).getTaskInfo().topActivity;
                    return componentName != null && componentName.getClassName().contains("AddCredentialActivity");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Activity check error: " + e.getMessage());
        }
        return false;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private RemoteViews createRemoteViews(String text) {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.autofill_dataset);
        views.setTextViewText(R.id.dataset_text, text);
        try {
            views.setImageViewResource(R.id.dataset_icon, R.drawable.ic_launcher_foreground);
        } catch (Exception ignored) {
            Log.e(TAG, "Failed to set icon in dataset view");
        }
        return views;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (dbHelper != null) {
            dbHelper.close();
        }
        Log.d(TAG, "Autofill Service Destroyed");
    }
}