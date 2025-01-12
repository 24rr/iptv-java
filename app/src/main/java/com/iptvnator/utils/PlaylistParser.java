package com.iptvnator.utils;

import com.iptvnator.model.Channel;
import com.iptvnator.model.Playlist;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class PlaylistParser {
    public static Playlist parse(String content) throws IllegalArgumentException {
        try {
            
            content = content.trim();
            content = content.replace("\uFEFF", ""); 
            content = content.replace("\r\n", "\n").replace("\r", "\n"); 

            android.util.Log.d("PlaylistParser", "Starting to parse playlist content");
            android.util.Log.d("PlaylistParser", "Content starts with: " + content.substring(0, Math.min(content.length(), 100)));

            if (!content.startsWith("#EXTM3U")) {
                throw new IllegalArgumentException("Invalid playlist format: Not a valid M3U file");
            }

            List<Channel> channels = new ArrayList<>();
            String[] lines = content.split("\n");
            Channel.Builder currentChannel = null;
            Channel.DrmConfig drmConfig = null;

            android.util.Log.d("PlaylistParser", "Total lines in playlist: " + lines.length);

            for (String line : lines) {
                line = line.trim();
                android.util.Log.d("PlaylistParser", "Processing line: " + line);
                
                if (line.startsWith("#EXTINF:")) {
                    currentChannel = new Channel.Builder();
                    currentChannel.group("Ungrouped");

                    
                    String[] tvgId = findMatch(line, "tvg-id=\"([^\"]*)\"");
                    String[] tvgName = findMatch(line, "tvg-name=\"([^\"]*)\"");
                    String[] tvgLogo = findMatch(line, "tvg-logo=\"([^\"]*)\"");
                    String[] groupTitle = findMatch(line, "group-title=\"([^\"]*)\"");
                    
                    
                    if (tvgLogo == null || tvgLogo[1].isEmpty()) {
                        tvgLogo = findMatch(line, "logo=\"([^\"]*)\"");
                        if (tvgLogo == null || tvgLogo[1].isEmpty()) {
                            tvgLogo = findMatch(line, "tvg-logo='([^']*)'");
                        }
                        if (tvgLogo == null || tvgLogo[1].isEmpty()) {
                            tvgLogo = findMatch(line, "logo='([^']*)'");
                        }
                    }

                    
                    if (groupTitle == null || groupTitle[1].isEmpty()) {
                        groupTitle = findMatch(line, "group='([^']*)'");
                        if (groupTitle == null || groupTitle[1].isEmpty()) {
                            groupTitle = findMatch(line, "group=\"([^\"]*)\"");
                        }
                    }

                    
                    String[] channelName = findMatch(line, ",(.*)$");

                    String name = channelName != null ? channelName[1].trim() : "Unnamed Channel";
                    android.util.Log.d("PlaylistParser", "Found channel: " + name);

                    currentChannel.id(tvgId != null ? tvgId[1] : "")
                            .name(name)
                            .logo(tvgLogo != null ? tvgLogo[1] : "")
                            .group(groupTitle != null ? groupTitle[1] : "Ungrouped");

                } else if (line.startsWith("#KODIPROP:")) {
                    String drmLine = line.replace("#KODIPROP:", "").trim();
                    android.util.Log.d("PlaylistParser", "Processing KODIPROP line: " + drmLine);
                    
                    if (drmLine.startsWith("inputstream.adaptive.license_type")) {
                        String drmType = drmLine.split("=")[1].toLowerCase();
                        if (drmConfig == null) {
                            drmConfig = new Channel.DrmConfig();
                        }
                        drmConfig.setType(drmType);
                        android.util.Log.d("PlaylistParser", "Found DRM type: " + drmType);
                    } else if (drmLine.startsWith("inputstream.adaptive.license_key")) {
                        String licenseKey = drmLine.split("=")[1];
                        if (drmConfig == null) {
                            drmConfig = new Channel.DrmConfig();
                            drmConfig.setType("clearkey"); 
                        }
                        drmConfig.setLicenseKey(licenseKey);
                        android.util.Log.d("PlaylistParser", "Found license key: " + licenseKey);
                    }
                } else if (line.startsWith("http")) {
                    if (currentChannel != null) {
                        currentChannel.url(line);
                        if (drmConfig != null) {
                            android.util.Log.d("PlaylistParser", "Setting DRM config for channel: " + currentChannel.build().getName());
                            android.util.Log.d("PlaylistParser", "DRM type: " + drmConfig.getType());
                            android.util.Log.d("PlaylistParser", "License key: " + drmConfig.getLicenseKey());
                            android.util.Log.d("PlaylistParser", "DRM config valid: " + drmConfig.isValid());
                            currentChannel.drmConfig(drmConfig);
                        }
                        Channel channel = currentChannel.build();
                        android.util.Log.d("PlaylistParser", "Adding channel: " + channel.getName() + 
                                         ", URL: " + channel.getUrl() + 
                                         ", Has DRM: " + (channel.getDrmConfig() != null && channel.getDrmConfig().isValid()));
                        channels.add(channel);
                        currentChannel = null;
                        drmConfig = null;
                    }
                }
            }

            if (channels.isEmpty()) {
                throw new IllegalArgumentException("No channels found in playlist");
            }

            android.util.Log.d("PlaylistParser", "Successfully parsed " + channels.size() + " channels");
            return new Playlist(channels);
        } catch (Exception e) {
            android.util.Log.e("PlaylistParser", "Error parsing playlist", e);
            throw new IllegalArgumentException("Failed to parse playlist: " + e.getMessage());
        }
    }

    private static String[] findMatch(String input, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(input);
        return m.find() ? new String[]{m.group(0), m.group(1)} : null;
    }
} 