package com.iptvnator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.iptvnator.R;
import java.util.List;

public class RecentPlaylistsAdapter extends RecyclerView.Adapter<RecentPlaylistsAdapter.ViewHolder> {
    private List<String> playlists;
    private OnPlaylistClickListener listener;

    public interface OnPlaylistClickListener {
        void onPlaylistClick(String url);
    }

    public RecentPlaylistsAdapter(List<String> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recent_playlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String url = playlists.get(position);
        holder.urlText.setText(url);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(url);
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void updatePlaylists(List<String> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView urlText;

        ViewHolder(View itemView) {
            super(itemView);
            urlText = itemView.findViewById(R.id.playlist_url_text);
        }
    }
} 