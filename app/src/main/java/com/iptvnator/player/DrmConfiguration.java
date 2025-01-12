package com.iptvnator.player;

import android.util.Base64;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager;
import androidx.media3.exoplayer.drm.DrmSessionManager;
import androidx.media3.exoplayer.drm.FrameworkMediaDrm;
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback;
import androidx.media3.exoplayer.drm.UnsupportedDrmException;
import java.util.UUID;
import java.util.Collections;

public class DrmConfiguration {
    private final String scheme;
    private final String keyId;
    private final String key;
    private final String licenseUrl;

    public DrmConfiguration(String scheme, String keyId, String key) {
        this(scheme, keyId, key, null);
    }

    public DrmConfiguration(String scheme, String keyId, String key, String licenseUrl) {
        this.scheme = scheme;
        this.keyId = keyId;
        this.key = key;
        this.licenseUrl = licenseUrl;
    }

    public String getScheme() {
        return scheme;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getKey() {
        return key;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public boolean isValid() {
        return scheme != null && keyId != null && key != null;
    }

    public UUID getDrmUuid() {
        if (scheme == null) return null;
        
        switch (scheme.toLowerCase()) {
            case "widevine":
                return C.WIDEVINE_UUID;
            case "playready":
                return C.PLAYREADY_UUID;
            case "clearkey":
                return C.CLEARKEY_UUID;
            default:
                return null;
        }
    }

    public DrmSessionManager buildDrmSessionManager() {
        UUID drmUuid = getDrmUuid();
        if (drmUuid == null) {
            return null;
        }

        try {
            if ("clearkey".equals(scheme.toLowerCase())) {
                
                byte[] keyIdBytes = hexToBytes(keyId);
                byte[] keyBytes = hexToBytes(key);
                
                String base64KeyId = Base64.encodeToString(keyIdBytes, 
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
                String base64Key = Base64.encodeToString(keyBytes,
                    Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);

                
                String jsonTemplate = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"%s\",\"kid\":\"%s\"}],\"type\":\"temporary\"}";
                String json = String.format(jsonTemplate, base64Key, base64KeyId);
                byte[] licenseData = json.getBytes();

                
                LocalMediaDrmCallback drmCallback = new LocalMediaDrmCallback(licenseData);

                
                return new DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(drmUuid, uuid -> {
                        try {
                            return FrameworkMediaDrm.newInstance(uuid);
                        } catch (UnsupportedDrmException e) {
                            android.util.Log.e("DrmConfiguration", "Failed to create DRM instance", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .build(drmCallback);
            }
        } catch (Exception e) {
            android.util.Log.e("DrmConfiguration", "Error building DRM session manager", e);
        }
        return null;
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    public MediaItem.DrmConfiguration buildMediaDrmConfiguration() {
        UUID drmUuid = getDrmUuid();
        if (drmUuid == null) {
            return null;
        }

        MediaItem.DrmConfiguration.Builder builder = new MediaItem.DrmConfiguration.Builder(drmUuid)
            .setMultiSession(false);

        if ("clearkey".equals(scheme.toLowerCase())) {
            
            builder.setForceDefaultLicenseUri(false)
                  .setMultiSession(false)
                  .setLicenseRequestHeaders(Collections.emptyMap());
        } else if (licenseUrl != null) {
            builder.setLicenseUri(licenseUrl);
        }

        return builder.build();
    }

    public static DrmConfiguration fromUrl(String url) {
        
        if (url == null || !url.startsWith("drm://")) {
            return null;
        }

        try {
            String[] parts = url.substring(6).split("\\?");
            if (parts.length != 2) {
                return null;
            }

            String scheme = parts[0];
            String[] params = parts[1].split("&");
            String keyId = null;
            String key = null;
            String licenseUrl = null;

            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length != 2) continue;

                switch (keyValue[0]) {
                    case "kid":
                        keyId = keyValue[1];
                        break;
                    case "key":
                        key = keyValue[1];
                        break;
                    case "license":
                        licenseUrl = keyValue[1];
                        break;
                }
            }

            return new DrmConfiguration(scheme, keyId, key, licenseUrl);
        } catch (Exception e) {
            return null;
        }
    }
} 