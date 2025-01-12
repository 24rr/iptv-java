package com.iptvnator.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.iptvnator.R;

public class JWPlayerLicenseDialog extends DialogFragment {
    private static final String PREFS_NAME = "IPTVnatorPrefs";
    private static final String JW_LICENSE_KEY = "jw_license_key";
    
    private OnLicenseSetListener listener;

    public interface OnLicenseSetListener {
        void onLicenseSet(String license);
    }

    public static JWPlayerLicenseDialog newInstance() {
        return new JWPlayerLicenseDialog();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnLicenseSetListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnLicenseSetListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_jw_license, null);
        
        EditText licenseInput = view.findViewById(R.id.license_input);
        
        // Load saved license if exists
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLicense = prefs.getString(JW_LICENSE_KEY, "");
        licenseInput.setText(savedLicense);

        builder.setView(view)
            .setTitle("JW Player License")
            .setPositiveButton("Save", (dialog, id) -> {
                String license = licenseInput.getText().toString().trim();
                if (!license.isEmpty()) {
                    // Save license
                    prefs.edit().putString(JW_LICENSE_KEY, license).apply();
                    listener.onLicenseSet(license);
                }
            })
            .setNegativeButton("Cancel", (dialog, id) -> {
                if (getDialog() != null) {
                    getDialog().cancel();
                }
            });

        return builder.create();
    }

    public static String getSavedLicense(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(JW_LICENSE_KEY, null);
    }
} 