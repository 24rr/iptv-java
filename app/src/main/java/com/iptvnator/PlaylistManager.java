package com.iptvnator;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import com.iptvnator.model.Channel;
import com.iptvnator.utils.PlaylistParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.os.Handler;
import android.os.Looper;

public class PlaylistManager {
    private static final String PREFS_NAME = "IPTVnatorPrefs";
    private static final String RECENT_PLAYLISTS_KEY = "recent_playlists";
    private static final int MAX_RECENT_PLAYLISTS = 10;
    
    private Context context;
    private SharedPreferences prefs;
    private OkHttpClient client;
    private PlaylistParser playlistParser;

    public PlaylistManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.client = new OkHttpClient();
        this.playlistParser = new PlaylistParser();
        
        // Clear invalid data if exists
        try {
            prefs.getStringSet(RECENT_PLAYLISTS_KEY, null);
        } catch (ClassCastException e) {
            // Clear the invalid data
            prefs.edit().remove(RECENT_PLAYLISTS_KEY).apply();
        }
    }

    public interface PlaylistCallback {
        void onPlaylistLoaded(List<Channel> channels);
    }

    public void loadPlaylistFromUrl(String url, PlaylistCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to load playlist: " + response.code());
                }
                
                String playlistContent = response.body().string()
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replaceAll("^\uFEFF", "");

                if (!playlistContent.trim().startsWith("#EXTM3U")) {
                    throw new Exception("Invalid playlist format: Not a valid M3U file");
                }

                List<Channel> channels = parseM3U(playlistContent);
                if (channels == null || channels.isEmpty()) {
                    throw new Exception("No channels found in playlist");
                }
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onPlaylistLoaded(channels);
                });
            } catch (Exception e) {
                e.printStackTrace(); // Log the full stack trace
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onPlaylistLoaded(null);
                });
            }
        }).start();
    }

    public void loadPlaylistFromFile(Uri uri, PlaylistCallback callback) {
        android.util.Log.d("PlaylistManager", "Loading playlist from file: " + uri);
        new Thread(() -> {
            try {
                if (uri == null) {
                    throw new Exception("URI is null");
                }

                android.util.Log.d("PlaylistManager", "Opening input stream");
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new Exception("Could not open file");
                }

                android.util.Log.d("PlaylistManager", "Reading file content");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder content = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                inputStream.close();

                String playlistContent = content.toString();
                android.util.Log.d("PlaylistManager", "File content length: " + playlistContent.length());
                
                if (playlistContent.isEmpty()) {
                    throw new Exception("Playlist file is empty");
                }

                playlistContent = playlistContent
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replaceAll("^\uFEFF", "");

                if (!playlistContent.trim().startsWith("#EXTM3U")) {
                    android.util.Log.e("PlaylistManager", "Invalid content start: " + playlistContent.substring(0, Math.min(50, playlistContent.length())));
                    throw new Exception("Invalid playlist format: Not a valid M3U file");
                }

                android.util.Log.d("PlaylistManager", "Parsing M3U content");
                List<Channel> channels = parseM3U(playlistContent);
                android.util.Log.d("PlaylistManager", "Found channels: " + (channels != null ? channels.size() : "null"));
                
                if (channels == null || channels.isEmpty()) {
                    throw new Exception("No channels found in playlist");
                }
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onPlaylistLoaded(channels);
                });
            } catch (Exception e) {
                android.util.Log.e("PlaylistManager", "Error loading playlist: " + e.getMessage(), e);
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onPlaylistLoaded(null);
                });
            }
        }).start();
    }

    private ArrayList<Channel> parseM3U(String content) {
        android.util.Log.d("PlaylistManager", "Starting M3U parsing");
        try {
            ArrayList<Channel> channels = new ArrayList<>();
            if (content == null || content.trim().isEmpty()) {
                android.util.Log.e("PlaylistManager", "Content is null or empty");
                return channels;
            }

            String[] lines = content.split("\n");
            android.util.Log.d("PlaylistManager", "Number of lines: " + lines.length);
            
            String currentName = null;
            String currentLogo = null;
            String currentGroup = null;
            
            for (String line : lines) {
                try {
                    line = line.trim();
                    
                    if (line.startsWith("#EXTINF:")) {
                        android.util.Log.d("PlaylistManager", "Processing EXTINF line: " + line);
                        // Parse channel info
                        int nameStart = line.lastIndexOf(",");
                        if (nameStart != -1) {
                            currentName = line.substring(nameStart + 1).trim();
                            android.util.Log.d("PlaylistManager", "Found channel name: " + currentName);
                        }
                        
                        // Parse tvg-logo if present
                        int logoStart = line.indexOf("tvg-logo=\"");
                        if (logoStart != -1) {
                            int logoEnd = line.indexOf("\"", logoStart + 10);
                            if (logoEnd != -1) {
                                currentLogo = line.substring(logoStart + 10, logoEnd);
                                android.util.Log.d("PlaylistManager", "Found logo URL: " + currentLogo);
                            }
                        }
                        
                        // Parse group-title if present
                        int groupStart = line.indexOf("group-title=\"");
                        if (groupStart != -1) {
                            int groupEnd = line.indexOf("\"", groupStart + 13);
                            if (groupEnd != -1) {
                                currentGroup = line.substring(groupStart + 13, groupEnd);
                                android.util.Log.d("PlaylistManager", "Found group: " + currentGroup);
                            }
                        }
                    } else if (!line.startsWith("#") && !line.isEmpty() && currentName != null) {
                        // This is a URL line
                        android.util.Log.d("PlaylistManager", "Creating channel with URL: " + line);
                        Channel channel = new Channel.Builder()
                            .name(currentName)
                            .url(line)
                            .logo(currentLogo)
                            .group(currentGroup)
                            .build();
                        channels.add(channel);
                        
                        currentName = null;
                        currentLogo = null;
                        currentGroup = null;
                    }
                } catch (Exception e) {
                    android.util.Log.e("PlaylistManager", "Error parsing line: " + e.getMessage(), e);
                }
            }
            
            android.util.Log.d("PlaylistManager", "Finished parsing, found " + channels.size() + " channels");
            return channels;
        } catch (Exception e) {
            android.util.Log.e("PlaylistManager", "Error in parseM3U: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public void addToRecent(String playlistUrl, boolean isFile) {
        try {
            Set<String> recentPlaylists = new HashSet<>(getRecentPlaylists());
            String entry = isFile ? "file://" + playlistUrl : playlistUrl;
            recentPlaylists.add(entry);
            
            // Maintain max size
            if (recentPlaylists.size() > MAX_RECENT_PLAYLISTS) {
                ArrayList<String> list = new ArrayList<>(recentPlaylists);
                recentPlaylists = new HashSet<>(list.subList(list.size() - MAX_RECENT_PLAYLISTS, list.size()));
            }
            
            prefs.edit().putStringSet(RECENT_PLAYLISTS_KEY, recentPlaylists).apply();
        } catch (Exception e) {
            android.util.Log.e("PlaylistManager", "Error adding to recent playlists", e);
            // Clear the preferences if there's an error
            prefs.edit().remove(RECENT_PLAYLISTS_KEY).apply();
        }
    }

    public boolean hasRecentPlaylists() {
        Set<String> recentSet = prefs.getStringSet(RECENT_PLAYLISTS_KEY, null);
        return recentSet != null && !recentSet.isEmpty();
    }

    public ArrayList<String> getRecentPlaylists() {
        try {
            Set<String> recentSet = prefs.getStringSet(RECENT_PLAYLISTS_KEY, new HashSet<>());
            return recentSet != null ? new ArrayList<>(recentSet) : new ArrayList<>();
        } catch (ClassCastException e) {
            // If there's a type mismatch, clear the data and return empty list
            prefs.edit().remove(RECENT_PLAYLISTS_KEY).apply();
            return new ArrayList<>();
        }
    }

    public boolean isFilePlaylist(String url) {
        return url != null && url.startsWith("file://");
    }

    public String getPlaylistPath(String url) {
        if (isFilePlaylist(url)) {
            return url.substring(7); // Remove "file://" prefix
        }
        return url;
    }

    public void clearRecentPlaylists() {
        prefs.edit().remove(RECENT_PLAYLISTS_KEY).apply();
    }
} 