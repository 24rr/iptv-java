package com.iptvnator.player;

import android.content.Context;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.ui.PlayerView;
import java.util.Map;
import java.util.HashMap;

public class IPTVPlayer {
    private final Context context;
    private ExoPlayer player;
    private PlayerView playerView;
    private PlayerCallback callback;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;

    public interface PlayerCallback {
        void onError(String message, Exception error);
        void onStateChanged(int state);
        void onProgress(long position, long duration);
    }

    public IPTVPlayer(Context context, PlayerView playerView, PlayerCallback callback) {
        this.context = context;
        this.playerView = playerView;
        this.callback = callback;
        initializePlayer();
    }

    private void initializePlayer() {
        try {
            
            httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("iptvnator-android")
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true);

            
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(context)
                .setDataSourceFactory(httpDataSourceFactory);

            
            player = new ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

            
            playerView.setPlayer(player);
            playerView.setKeepScreenOn(true);

            
            player.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(PlaybackException error) {
                    String message = "Playback error: ";
                    if (error.errorCode == PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR) {
                        message = "DRM error: " + error.getMessage();
                    } else if (error.errorCode == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED) {
                        message = "Unsupported DRM scheme";
                    } else if (error.errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED) {
                        message = "Failed to acquire DRM license";
                    } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                        message = "Network error - Check your internet connection";
                    }
                    if (callback != null) {
                        callback.onError(message, error);
                    }
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (callback != null) {
                        callback.onStateChanged(state);
                    }
                }

                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition, 
                                                  Player.PositionInfo newPosition, 
                                                  @Player.DiscontinuityReason int reason) {
                    if (callback != null) {
                        callback.onProgress(getCurrentPosition(), getDuration());
                    }
                }
            });

        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error initializing player", e);
            }
        }
    }

    public void play(String url, DrmConfiguration drmConfig) {
        try {
            android.util.Log.d("IPTVPlayer", "Playing URL: " + url);
            if (drmConfig != null) {
                android.util.Log.d("IPTVPlayer", "Using DRM: " + drmConfig.getScheme());
            }

            
            MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(url);

            
            if (drmConfig != null && drmConfig.isValid()) {
                
                var drmSessionManager = drmConfig.buildDrmSessionManager();
                if (drmSessionManager != null) {
                    
                    DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(context)
                        .setDataSourceFactory(httpDataSourceFactory)
                        .setDrmSessionManagerProvider(mediaType -> drmSessionManager);

                    
                    player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItemBuilder.build()));

                    
                    MediaItem mediaItem = mediaItemBuilder.build();
                    player.prepare();
                    player.play();
                }
            }

        } catch (Exception e) {
            if (callback != null) {
                callback.onError("Error playing stream", e);
            }
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void resume() {
        if (player != null) {
            player.play();
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void setVolume(float volume) {
        if (player != null) {
            player.setVolume(volume);
        }
    }
} 