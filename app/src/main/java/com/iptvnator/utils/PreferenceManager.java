package com.iptvnator.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PreferenceManager {
    private static final String PREF_NAME = "iptvnator_prefs";
    private static final String KEY_RECENT_PLAYLISTS = "recent_playlists";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_PLAYER_SOURCE = "player_source";
    private static final int MAX_RECENT_PLAYLISTS = 10;

    private final SharedPreferences prefs;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addRecentPlaylist(String url) {
        Set<String> recentUrls = new HashSet<>(prefs.getStringSet(KEY_RECENT_PLAYLISTS, new HashSet<>()));
        recentUrls.add(url);
        
        
        if (recentUrls.size() > MAX_RECENT_PLAYLISTS) {
            List<String> urlList = new ArrayList<>(recentUrls);
            urlList.remove(0);
            recentUrls = new HashSet<>(urlList);
        }
        
        prefs.edit().putStringSet(KEY_RECENT_PLAYLISTS, recentUrls).apply();
    }

    public List<String> getRecentPlaylists() {
        Set<String> recentUrls = prefs.getStringSet(KEY_RECENT_PLAYLISTS, new HashSet<>());
        return new ArrayList<>(recentUrls);
    }

    public void clearRecentPlaylists() {
        prefs.edit().remove(KEY_RECENT_PLAYLISTS).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean isDarkMode) {
        prefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
    }

    public String getPlayerSource() {
        return prefs.getString(KEY_PLAYER_SOURCE, "Shaka Player");
    }

    public void setPlayerSource(String playerSource) {
        prefs.edit().putString(KEY_PLAYER_SOURCE, playerSource).apply();
    }
} 