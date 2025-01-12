package com.iptvnator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.iptvnator.adapters.RecentPlaylistsAdapter;
import com.iptvnator.dialogs.SettingsDialog;
import com.iptvnator.model.Channel;
import com.iptvnator.model.Playlist;
import com.iptvnator.player.PlayerActivity;
import com.iptvnator.utils.PlaylistParser;
import com.iptvnator.utils.PreferenceManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.File;
import android.database.Cursor;
import android.provider.OpenableColumns;

public class MainActivity extends AppCompatActivity implements 
    RecentPlaylistsAdapter.OnPlaylistClickListener,
    SettingsDialog.OnSettingsChangeListener {

    private TextInputEditText playlistUrlInput;
    private MaterialButton loadUrlButton;
    private MaterialButton loadFileButton;
    private RecyclerView recentPlaylists;
    private PreferenceManager preferenceManager;
    private PlaylistParser playlistParser;
    private PlaylistManager playlistManager;
    private RecentPlaylistsAdapter recentPlaylistsAdapter;
    private final OkHttpClient httpClient = new OkHttpClient();

    private final ActivityResultLauncher<String> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.GetContent(),
        this::handleFilePickerResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            preferenceManager = new PreferenceManager(this);
            setTheme(preferenceManager.isDarkMode() ? R.style.AppTheme_Dark : R.style.AppTheme);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            initializeViews();
            setupListeners();
            setupRecentPlaylists();
        } catch (Exception e) {
            e.printStackTrace();
            
            android.widget.Toast.makeText(this, 
                "Error initializing app: " + e.getMessage(), 
                android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        try {
            playlistUrlInput = findViewById(R.id.playlist_url);
            loadUrlButton = findViewById(R.id.load_url_button);
            loadFileButton = findViewById(R.id.load_file_button);
            recentPlaylists = findViewById(R.id.recent_playlists);
            
            if (recentPlaylists != null) {
                recentPlaylists.setLayoutManager(new LinearLayoutManager(this));
            }

            
            playlistParser = new PlaylistParser();
            playlistManager = new PlaylistManager(this);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize views: " + e.getMessage());
        }
    }

    private void setupListeners() {
        try {
            if (loadUrlButton != null) {
                loadUrlButton.setOnClickListener(v -> loadPlaylistFromUrl());
            }
            if (loadFileButton != null) {
                loadFileButton.setOnClickListener(v -> loadPlaylistFromFile());
            }
            View settingsFab = findViewById(R.id.settings_fab);
            if (settingsFab != null) {
                settingsFab.setOnClickListener(v -> showSettings());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to setup listeners: " + e.getMessage());
        }
    }

    private void setupRecentPlaylists() {
        try {
            android.util.Log.d("MainActivity", "Setting up recent playlists");
            if (playlistManager != null && recentPlaylists != null) {
                List<String> recentUrls = playlistManager.getRecentPlaylists();
                android.util.Log.d("MainActivity", "Got recent playlists: " + (recentUrls != null ? recentUrls.size() : "null"));
                
                if (recentUrls == null) {
                    recentUrls = new ArrayList<>();
                }
                
                
                recentPlaylistsAdapter = new RecentPlaylistsAdapter(new ArrayList<>(recentUrls), this);
                recentPlaylists.setAdapter(recentPlaylistsAdapter);
                
                
                View recentPlaylistsHeader = findViewById(R.id.recent_playlists_header);
                if (recentUrls.isEmpty()) {
                    android.util.Log.d("MainActivity", "No recent playlists, hiding section");
                    if (recentPlaylistsHeader != null) {
                        recentPlaylistsHeader.setVisibility(View.GONE);
                    }
                    recentPlaylists.setVisibility(View.GONE);
                } else {
                    android.util.Log.d("MainActivity", "Showing recent playlists section");
                    if (recentPlaylistsHeader != null) {
                        recentPlaylistsHeader.setVisibility(View.VISIBLE);
                    }
                    recentPlaylists.setVisibility(View.VISIBLE);
                }
            } else {
                android.util.Log.w("MainActivity", "PlaylistManager or RecyclerView is null");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in setupRecentPlaylists: " + e.getMessage(), e);
            
            if (playlistManager != null && playlistManager.hasRecentPlaylists()) {
                android.widget.Toast.makeText(this, 
                    "Could not load recent playlists", 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
            
            View recentPlaylistsHeader = findViewById(R.id.recent_playlists_header);
            if (recentPlaylistsHeader != null) {
                recentPlaylistsHeader.setVisibility(View.GONE);
            }
            if (recentPlaylists != null) {
                recentPlaylists.setVisibility(View.GONE);
            }
        }
    }

    private void loadPlaylistFromUrl() {
        String url = playlistUrlInput.getText().toString().trim();
        if (url.isEmpty()) {
            playlistUrlInput.setError("Please enter a URL");
            return;
        }

        loadUrlButton.setEnabled(false);
        playlistManager.loadPlaylistFromUrl(url, channels -> {
            runOnUiThread(() -> {
                if (channels != null && !channels.isEmpty()) {
                    Playlist playlist = new Playlist(channels);
                    playlist.setFilePath(url);
                    playlistManager.addToRecent(url, false);
                    openPlayerActivity(playlist);
                } else {
                    playlistUrlInput.setError("Error loading playlist: No channels found");
                }
                loadUrlButton.setEnabled(true);
            });
        });
    }

    private void loadPlaylistFromFile() {
        try {
            filePickerLauncher.launch("application/x-mpegurl,application/octet-stream,*/*");
        } catch (Exception e) {
            showError("Error launching file picker: " + e.getMessage());
        }
    }

    private void handleFilePickerResult(Uri uri) {
        if (uri == null) {
            showError("No file selected");
            return;
        }
        
        try {
            android.util.Log.d("MainActivity", "Handling file picker result: " + uri);
            loadPlaylist(uri);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error handling file: " + e.getMessage(), e);
            showError("Error loading file: " + e.getMessage());
        }
    }

    private String getFilePathFromUri(Uri uri) {
        if (uri == null) return null;
        
        
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        
        
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    String fileName = cursor.getString(nameIndex);
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "playlist_" + System.currentTimeMillis() + ".m3u8";
                    }
                    
                    File cacheDir = getExternalCacheDir();
                    if (cacheDir == null) {
                        cacheDir = getCacheDir();
                    }
                    
                    File file = new File(cacheDir, fileName);
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        if (inputStream != null) {
                            java.nio.file.Files.copy(
                                inputStream,
                                file.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            );
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return uri.toString();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "Untitled Playlist";
    }

    private void showError(String message) {
        runOnUiThread(() -> {
            if (playlistUrlInput != null) {
                playlistUrlInput.setError(message);
            }
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onPlaylistClick(String url) {
        try {
            android.util.Log.d("MainActivity", "Clicked playlist: " + url);
            if (url == null) {
                showError("Invalid playlist URL");
                return;
            }
            
            if (url.startsWith("content://") || url.startsWith("file://")) {
                android.util.Log.d("MainActivity", "Handling as file: " + url);
                handleFilePickerResult(Uri.parse(url));
            } else {
                android.util.Log.d("MainActivity", "Handling as URL: " + url);
                playlistUrlInput.setText(url);
                loadPlaylistFromUrl();
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onPlaylistClick: " + e.getMessage(), e);
            showError("Error opening playlist: " + e.getMessage());
        }
    }

    private void openPlayerActivity(Playlist playlist) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("playlist", playlist);
        startActivity(intent);
    }

    private void showSettings() {
        SettingsDialog dialog = new SettingsDialog();
        dialog.show(getSupportFragmentManager(), "settings");
    }

    @Override
    public void onThemeChanged(boolean isDarkMode) {
        AppCompatDelegate.setDefaultNightMode(
            isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        recreate();
    }

    @Override
    public void onPlayerSourceChanged(String playerSource) {
    }

    @Override
    public void onClearRecentPlaylists() {
        try {
            android.util.Log.d("MainActivity", "Clearing recent playlists");
            if (playlistManager != null) {
                playlistManager.clearRecentPlaylists();
            }
            if (recentPlaylistsAdapter != null) {
                recentPlaylistsAdapter.updatePlaylists(new ArrayList<>());
            }
            setupRecentPlaylists(); 
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error clearing playlists: " + e.getMessage(), e);
            showError("Error clearing playlists: " + e.getMessage());
        }
    }

    private String readFile(Uri uri) throws Exception {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            
            android.util.Log.d("MainActivity", "Reading file from URI: " + uri);
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            android.util.Log.d("MainActivity", "File read successfully, content length: " + content.length());
            return content.toString();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error reading file", e);
            throw new Exception("Error reading file: " + e.getMessage());
        }
    }

    private void loadPlaylist(Uri uri) {
        try {
            String content = readFile(uri);
            android.util.Log.d("MainActivity", "Loading playlist content: " + content.substring(0, Math.min(content.length(), 1000)));
            
            Playlist playlist = PlaylistParser.parse(content);
            if (playlist != null && !playlist.getChannels().isEmpty()) {
                android.util.Log.d("MainActivity", "Playlist loaded successfully with " + playlist.getChannels().size() + " channels");
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("playlist", playlist);
                startActivity(intent);
            } else {
                showError("No channels found in playlist");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error loading playlist", e);
            showError("Error loading playlist: " + e.getMessage());
        }
    }
} 