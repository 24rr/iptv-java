package com.iptvnator.drm;

import android.net.Uri;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.HttpMediaDrmCallback;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.datasource.DefaultHttpDataSource;
import java.util.UUID;

public class DrmConfig {
    private final UUID drmSchemeUuid;
    private final String licenseUrl;
    private final String[] keyRequestProperties;

    public DrmConfig(UUID drmSchemeUuid, String licenseUrl, String[] keyRequestProperties) {
        this.drmSchemeUuid = drmSchemeUuid;
        this.licenseUrl = licenseUrl;
        this.keyRequestProperties = keyRequestProperties;
    }

    public UUID getSchemeType() {
        return drmSchemeUuid;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public DrmSessionManager buildSessionManager() {
        HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(
            licenseUrl,
            new DefaultHttpDataSource.Factory()
        );

        if (keyRequestProperties != null) {
            for (int i = 0; i < keyRequestProperties.length; i += 2) {
                if (i + 1 < keyRequestProperties.length) {
                    drmCallback.setKeyRequestProperty(keyRequestProperties[i], keyRequestProperties[i + 1]);
                }
            }
        }

        return new DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(drmSchemeUuid, uuid -> {
                try {
                    return FrameworkMediaDrm.newInstance(uuid);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .build(drmCallback);
    }

    public static DrmConfig detectFromUrl(String url) {
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        
        if (scheme != null && scheme.equals("drm")) {
            String drmType = uri.getHost();
            String licenseUrl = uri.getQueryParameter("license");
            
            switch (drmType.toLowerCase()) {
                case "widevine":
                    return new DrmConfig(C.WIDEVINE_UUID, licenseUrl, null);
                case "playready":
                    return new DrmConfig(C.PLAYREADY_UUID, licenseUrl, null);
                case "clearkey":
                    return new DrmConfig(C.CLEARKEY_UUID, licenseUrl, null);
                default:
                    return null;
            }
        }
        return null;
    }
} 