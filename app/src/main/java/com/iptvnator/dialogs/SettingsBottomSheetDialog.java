package com.iptvnator.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.iptvnator.R;
import com.iptvnator.utils.PreferenceManager;

public class SettingsBottomSheetDialog extends BottomSheetDialogFragment {
    private PreferenceManager preferenceManager;
    private TextView themeValue;
    private TextView playerValue;
    private OnSettingsChangeListener listener;

    public interface OnSettingsChangeListener {
        void onThemeChanged(boolean isDarkMode);
        void onPlayerSourceChanged(String playerSource);
        void onClearRecentPlaylists();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnSettingsChangeListener) {
            listener = (OnSettingsChangeListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnSettingsChangeListener");
        }
        preferenceManager = new PreferenceManager(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        themeValue = view.findViewById(R.id.theme_value);
        playerValue = view.findViewById(R.id.player_value);
        MaterialButton clearPlaylistsButton = view.findViewById(R.id.clear_playlists_button);

        // Theme settings
        boolean isDarkMode = preferenceManager.isDarkMode();
        themeValue.setText(isDarkMode ? "Dark Mode" : "Light Mode");
        view.findViewById(R.id.theme_container).setOnClickListener(v -> {
            boolean newMode = !isDarkMode;
            preferenceManager.setDarkMode(newMode);
            themeValue.setText(newMode ? "Dark Mode" : "Light Mode");
            if (listener != null) {
                listener.onThemeChanged(newMode);
            }
        });

        // Player source settings
        String currentPlayer = preferenceManager.getPlayerSource();
        playerValue.setText(currentPlayer);
        view.findViewById(R.id.player_container).setOnClickListener(v -> {
            String newPlayer = "Shaka Player".equals(currentPlayer) ? "ExoPlayer" : "Shaka Player";
            preferenceManager.setPlayerSource(newPlayer);
            playerValue.setText(newPlayer);
            if (listener != null) {
                listener.onPlayerSourceChanged(newPlayer);
            }
        });

        // Clear playlists
        clearPlaylistsButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClearRecentPlaylists();
            }
            dismiss();
        });

        // Supported formats
        view.findViewById(R.id.formats_container).setOnClickListener(v -> {
            // TODO: Show supported formats dialog
        });
    }
} 