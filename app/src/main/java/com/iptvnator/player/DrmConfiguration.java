package com.iptvnator.player;

import android.util.Base64;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import java.util.UUID;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

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

    public String getClearKeyLicenseData() {
        if (!"clearkey".equals(scheme.toLowerCase())) {
            return null;
        }

        String jsonTemplate = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"%s\",\"kid\":\"%s\"}]}";
        String json = String.format(jsonTemplate, key, keyId);
        return Base64.encodeToString(json.getBytes(), Base64.NO_WRAP);
    }

    public MediaItem.DrmConfiguration buildMediaDrmConfiguration() {
        UUID drmUuid = getDrmUuid();
        if (drmUuid == null) {
            return null;
        }

        MediaItem.DrmConfiguration.Builder builder = new MediaItem.DrmConfiguration.Builder(drmUuid)
            .setMultiSession(false);

        if ("clearkey".equals(scheme.toLowerCase())) {
            try {
                // Create JSON format for ClearKey
                String jsonTemplate = "{\"keys\":[{\"kty\":\"oct\",\"k\":\"%s\",\"kid\":\"%s\"}]}";
                String json = String.format(jsonTemplate, key, keyId);
                
                // Convert to bytes
                byte[] keySetId = json.getBytes();
                
                builder.setKeySetId(keySetId)
                      .setMultiSession(false)
                      .setForceDefaultLicenseUri(false);
            } catch (Exception e) {
                android.util.Log.e("DrmConfiguration", "Error setting up ClearKey", e);
                return null;
            }
        } else if (licenseUrl != null) {
            builder.setLicenseUri(licenseUrl);
        }

        return builder.build();
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static DrmConfiguration fromUrl(String url) {
        // Example URL format: drm://clearkey?kid=abcdef&key=123456
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