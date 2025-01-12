package com.iptvnator.model;

import java.io.Serializable;

public class Channel implements Serializable {
    private String id;
    private String name;
    private String url;
    private String logo;
    private String group;
    private DrmConfig drmConfig;

    private Channel(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.url = builder.url;
        this.logo = builder.logo;
        this.group = builder.group;
        this.drmConfig = builder.drmConfig;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getLogo() {
        return logo;
    }

    public String getGroup() {
        return group;
    }

    public DrmConfig getDrmConfig() {
        return drmConfig;
    }

    public static class Builder {
        private String id;
        private String name;
        private String url;
        private String logo;
        private String group;
        private DrmConfig drmConfig;

        public Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder logo(String logo) {
            this.logo = logo;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder drmConfig(DrmConfig drmConfig) {
            this.drmConfig = drmConfig;
            return this;
        }

        public Channel build() {
            return new Channel(this);
        }
    }

    public static class DrmConfig implements Serializable {
        private String type;
        private String licenseKey;

        public DrmConfig() {
            this.type = "";
            this.licenseKey = "";
        }

        public String getType() {
            return type != null ? type.toLowerCase() : "";
        }

        public void setType(String type) {
            this.type = type;
            android.util.Log.d("DrmConfig", "Setting DRM type: " + type);
        }

        public String getLicenseKey() {
            return licenseKey != null ? licenseKey : "";
        }

        public void setLicenseKey(String licenseKey) {
            this.licenseKey = licenseKey;
            android.util.Log.d("DrmConfig", "Setting license key: " + licenseKey);
        }

        public boolean isValid() {
            boolean valid = type != null && 
                          (type.toLowerCase().equals("clearkey") || type.toLowerCase().contains("clearkey")) && 
                          licenseKey != null && 
                          licenseKey.contains(":");
            android.util.Log.d("DrmConfig", "DRM config valid: " + valid + 
                             " (type=" + type + ", licenseKey=" + (licenseKey != null ? licenseKey.substring(0, Math.min(20, licenseKey.length())) + "..." : "null") + ")");
            return valid;
        }
    }
} 