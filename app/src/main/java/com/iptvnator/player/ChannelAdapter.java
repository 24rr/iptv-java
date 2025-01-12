package com.iptvnator.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.iptvnator.R;
import com.iptvnator.model.Channel;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
    private List<Channel> channels;
    private final OnChannelClickListener clickListener;

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel);
    }

    public ChannelAdapter(List<Channel> channels, OnChannelClickListener clickListener) {
        this.channels = channels;
        this.clickListener = clickListener;
    }

    public void updateChannels(List<Channel> newChannels) {
        this.channels = newChannels;
        notifyDataSetChanged();
    }

    public List<Channel> getChannels() {
        return channels;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Channel channel = channels.get(position);
        holder.channelName.setText(channel.getName());
        
        if (channel.getLogo() != null && !channel.getLogo().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(channel.getLogo())
                    .placeholder(R.drawable.ic_channel_placeholder)
                    .error(R.drawable.ic_channel_placeholder)
                    .into(holder.channelIcon);
        } else {
            holder.channelIcon.setImageResource(R.drawable.ic_channel_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onChannelClick(channel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return channels != null ? channels.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView channelIcon;
        final TextView channelName;

        ViewHolder(View view) {
            super(view);
            channelIcon = view.findViewById(R.id.channel_icon);
            channelName = view.findViewById(R.id.channel_name);
        }
    }
} 