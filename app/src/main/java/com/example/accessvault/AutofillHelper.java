package com.example.accessvault;

import android.app.assist.AssistStructure;
import android.os.Build;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AutofillHelper {

    private static final String TAG = "AutofillHelper";

    public static class ParsedStructure {
        public AutofillId usernameId;
        public AutofillId passwordId;
        public String detectedSiteIdentifier;
        public List<AutofillId> allFields = new ArrayList<>();

        public boolean isCompleteForFill() {
            return usernameId != null && passwordId != null;
        }

        public boolean isCompleteForSave() {
            return usernameId != null && passwordId != null && detectedSiteIdentifier != null;
        }
    }

    public static ParsedStructure parseStructureForFill(AssistStructure structure, String packageName) {
        ParsedStructure parsed = new ParsedStructure();
        parsed.detectedSiteIdentifier = packageName;

        int nodeCount = structure.getWindowNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            AssistStructure.ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            traverseViewNodes(node, parsed);
        }

        return parsed.isCompleteForFill() ? parsed : null;
    }

    public static Credential parseStructureForSave(AssistStructure structure, String packageName) {
        ParsedStructure parsed = new ParsedStructure();
        parsed.detectedSiteIdentifier = packageName;

        int nodeCount = structure.getWindowNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            AssistStructure.ViewNode node = structure.getWindowNodeAt(i).getRootViewNode();
            traverseViewNodesForSave(node, parsed);
        }

        if (parsed.isCompleteForSave()) {
            return new Credential(-1,
                    parsed.detectedSiteIdentifier,
                    getAutofillText(structure, parsed.usernameId),
                    getAutofillText(structure, parsed.passwordId));
        }

        return null;
    }

    private static void traverseViewNodes(AssistStructure.ViewNode node, ParsedStructure parsed) {
        if (node == null) return;

        CharSequence[] hints = node.getAutofillHints();
        if (hints != null && hints.length > 0) {
            for (CharSequence hint : hints) {
                String h = hint.toString().toLowerCase(Locale.ROOT);
                if (h.contains("username")) {
                    parsed.usernameId = node.getAutofillId();
                } else if (h.contains("password")) {
                    parsed.passwordId = node.getAutofillId();
                }
            }
        }

        // Try fallback based on inputType
        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0) {
            parsed.usernameId = node.getAutofillId();
        } else if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType & android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0) {
            parsed.passwordId = node.getAutofillId();
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseViewNodes(node.getChildAt(i), parsed);
        }
    }

    private static void traverseViewNodesForSave(AssistStructure.ViewNode node, ParsedStructure parsed) {
        if (node == null) return;

        CharSequence[] hints = node.getAutofillHints();
        if (hints != null && hints.length > 0) {
            for (CharSequence hint : hints) {
                String h = hint.toString().toLowerCase(Locale.ROOT);
                if (h.contains("username")) {
                    parsed.usernameId = node.getAutofillId();
                } else if (h.contains("password")) {
                    parsed.passwordId = node.getAutofillId();
                }
            }
        }

        int inputType = node.getInputType();
        if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0) {
            parsed.usernameId = node.getAutofillId();
        } else if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType & android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0) {
            parsed.passwordId = node.getAutofillId();
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            traverseViewNodesForSave(node.getChildAt(i), parsed);
        }
    }

    private static String getAutofillText(AssistStructure structure, AutofillId id) {
        AssistStructure.ViewNode node = findNodeById(structure, id);
        if (node != null && node.getText() != null) {
            return node.getText().toString();
        }
        return "";
    }

    private static AssistStructure.ViewNode findNodeById(AssistStructure structure, AutofillId targetId) {
        int nodeCount = structure.getWindowNodeCount();
        for (int i = 0; i < nodeCount; i++) {
            AssistStructure.ViewNode root = structure.getWindowNodeAt(i).getRootViewNode();
            AssistStructure.ViewNode result = findNodeByIdRecursive(root, targetId);
            if (result != null) return result;
        }
        return null;
    }

    private static AssistStructure.ViewNode findNodeByIdRecursive(AssistStructure.ViewNode node, AutofillId targetId) {
        if (node == null) return null;

        if (targetId.equals(node.getAutofillId())) {
            return node;
        }

        int count = node.getChildCount();
        for (int i = 0; i < count; i++) {
            AssistStructure.ViewNode result = findNodeByIdRecursive(node.getChildAt(i), targetId);
            if (result != null) return result;
        }

        return null;
    }
}