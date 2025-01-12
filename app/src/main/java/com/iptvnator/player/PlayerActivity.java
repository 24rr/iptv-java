package com.iptvnator.player;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;
import com.google.android.material.snackbar.Snackbar;
import com.iptvnator.R;
import com.iptvnator.model.Channel;
import com.iptvnator.model.Playlist;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    private RecyclerView channelList;
    private TextView noChannelsText;
    private ChannelAdapter channelAdapter;
    private Playlist playlist;
    private IPTVPlayer player;
    private PlayerView playerView;
    private SearchView searchView;
    private List<Channel> allChannels;
    private boolean isSortedAscending = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        if (getSharedPreferences("IPTVnatorPrefs", MODE_PRIVATE).getBoolean("dark_mode", false)) {
            setTheme(R.style.AppTheme_Dark);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        playlist = (Playlist) getIntent().getSerializableExtra("playlist");
        if (playlist == null) {
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupChannelList();
        setupPlayer();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeViews() {
        channelList = findViewById(R.id.channel_list);
        noChannelsText = findViewById(R.id.no_channels_text);
        playerView = findViewById(R.id.player_view);

        channelList.setLayoutManager(new LinearLayoutManager(this));
        channelList.setHasFixedSize(true);
    }

    private void setupChannelList() {
        allChannels = playlist.getChannels();
        if (allChannels == null || allChannels.isEmpty()) {
            channelList.setVisibility(View.GONE);
            noChannelsText.setVisibility(View.VISIBLE);
            return;
        }

        channelList.setVisibility(View.VISIBLE);
        noChannelsText.setVisibility(View.GONE);

        channelAdapter = new ChannelAdapter(new ArrayList<>(allChannels), this::onChannelClick);
        channelList.setAdapter(channelAdapter);
    }

    private void setupPlayer() {
        try {
            player = new IPTVPlayer(this, playerView, new IPTVPlayer.PlayerCallback() {
                @Override
                public void onError(String message, Exception error) {
                    android.util.Log.e("PlayerActivity", message, error);
                    Snackbar.make(playerView, message, Snackbar.LENGTH_LONG).show();
                }

                @Override
                public void onStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        android.util.Log.d("PlayerActivity", "Playback ready");
                        playerView.setVisibility(View.VISIBLE);
                    } else if (state == Player.STATE_BUFFERING) {
                        android.util.Log.d("PlayerActivity", "Buffering...");
                    }
                }

                @Override
                public void onProgress(long position, long duration) {
                    
                }
            });
        } catch (Exception e) {
            android.util.Log.e("PlayerActivity", "Error setting up player", e);
            Snackbar.make(playerView, "Error setting up player: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void onChannelClick(Channel channel) {
        if (channel.getUrl() != null && !channel.getUrl().isEmpty()) {
            try {
                android.util.Log.d("PlayerActivity", "Loading channel: " + channel.getName());
                android.util.Log.d("PlayerActivity", "URL: " + channel.getUrl());
                
                
                Channel.DrmConfig channelDrmConfig = channel.getDrmConfig();
                DrmConfiguration drmConfig = null;
                
                if (channelDrmConfig != null && channelDrmConfig.isValid()) {
                    android.util.Log.d("PlayerActivity", "Using DRM config: type=" + channelDrmConfig.getType() + 
                            ", license=" + channelDrmConfig.getLicenseKey().substring(0, Math.min(20, channelDrmConfig.getLicenseKey().length())) + "...");
                            
                    if ("clearkey".equals(channelDrmConfig.getType())) {
                        
                        String[] keyParts = channelDrmConfig.getLicenseKey().split(":");
                        if (keyParts.length != 2) {
                            throw new IllegalArgumentException("Invalid ClearKey format. Expected format: keyId:key");
                        }
                        
                        drmConfig = new DrmConfiguration("clearkey", keyParts[0], keyParts[1]);
                    }
                }
                
                player.play(channel.getUrl(), drmConfig);
                
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(channel.getName());
                }
            } catch (Exception e) {
                String errorMsg = "Error loading channel: " + e.getMessage();
                android.util.Log.e("PlayerActivity", errorMsg, e);
                Snackbar.make(playerView, errorMsg, Snackbar.LENGTH_LONG).show();
            }
        } else {
            Snackbar.make(playerView, "Invalid channel URL", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player_menu, menu);
        
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterChannels(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterChannels(newText);
                return true;
            }
        });
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            sortChannels();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterChannels(String query) {
        if (allChannels == null) return;
        
        if (query == null || query.isEmpty()) {
            channelAdapter.updateChannels(allChannels);
            return;
        }

        List<Channel> filteredList = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();
        
        for (Channel channel : allChannels) {
            if (channel.getName().toLowerCase().contains(lowerCaseQuery)) {
                filteredList.add(channel);
            }
        }
        
        channelAdapter.updateChannels(filteredList);
    }

    private void sortChannels() {
        if (allChannels == null) return;
        
        List<Channel> currentChannels = new ArrayList<>(channelAdapter.getChannels());
        if (isSortedAscending) {
            Collections.sort(currentChannels, (c1, c2) -> 
                c2.getName().compareToIgnoreCase(c1.getName()));
        } else {
            Collections.sort(currentChannels, (c1, c2) -> 
                c1.getName().compareToIgnoreCase(c2.getName()));
        }
        isSortedAscending = !isSortedAscending;
        channelAdapter.updateChannels(currentChannels);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.resume();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
} 