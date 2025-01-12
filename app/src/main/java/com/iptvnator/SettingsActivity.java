package com.iptvnator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.iptvnator.utils.PreferenceManager;

public class SettingsActivity extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private SwitchMaterial darkModeSwitch;
    private MaterialRadioButton exoPlayerRadio;
    private MaterialRadioButton webViewRadio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferenceManager = new PreferenceManager(this);

        setupToolbar();
        initializeViews();
        loadPreferences();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void initializeViews() {
        darkModeSwitch = findViewById(R.id.dark_mode_switch);
        exoPlayerRadio = findViewById(R.id.exoplayer_radio);
        webViewRadio = findViewById(R.id.webview_radio);
        MaterialButton githubButton = findViewById(R.id.github_button);

        githubButton.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://github.com/4gray/iptvnator"));
            startActivity(browserIntent);
        });
    }

    private void loadPreferences() {
        darkModeSwitch.setChecked(preferenceManager.isDarkMode());
        String playerSource = preferenceManager.getPlayerSource();
        if ("webview".equals(playerSource)) {
            webViewRadio.setChecked(true);
        } else {
            exoPlayerRadio.setChecked(true);
        }
    }

    private void setupListeners() {
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferenceManager.setDarkMode(isChecked);
            recreate();
        });

        exoPlayerRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                preferenceManager.setPlayerSource("exoplayer");
            }
        });

        webViewRadio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                preferenceManager.setPlayerSource("webview");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 