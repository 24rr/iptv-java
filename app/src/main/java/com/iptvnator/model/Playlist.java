package com.iptvnator.model;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Playlist implements Serializable {
    private String filePath;
    private List<Channel> channels;

    public Playlist() {
        this.channels = new ArrayList<>();
    }

    public Playlist(List<Channel> channels) {
        this.channels = channels != null ? channels : new ArrayList<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    public void addChannel(Channel channel) {
        if (channels == null) {
            channels = new ArrayList<>();
        }
        channels.add(channel);
    }

    public int size() {
        return channels != null ? channels.size() : 0;
    }

    public boolean isEmpty() {
        return channels == null || channels.isEmpty();
    }
} 