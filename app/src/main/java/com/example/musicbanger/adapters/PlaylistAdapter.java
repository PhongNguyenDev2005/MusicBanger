package com.example.musicbanger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicbanger.R;
import com.example.musicbanger.model.Playlist;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private List<Playlist> playlists;
    private OnPlaylistClickListener listener;

    public PlaylistAdapter(List<Playlist> playlists, OnPlaylistClickListener listener) {
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.bind(playlist);
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public void updatePlaylists(List<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    class PlaylistViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPlaylistName;
        private TextView tvTrackCount;
        private TextView tvPlaylistDescription;
        private ImageView ivPlaylistArt;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tvPlaylistName);
            tvTrackCount = itemView.findViewById(R.id.tvTrackCount);
            tvPlaylistDescription = itemView.findViewById(R.id.tvPlaylistDescription);
            ivPlaylistArt = itemView.findViewById(R.id.ivPlaylistArt);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onPlaylistClick(playlists.get(position));
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onPlaylistLongClick(playlists.get(position));
                        return true;
                    }
                }
                return false;
            });
        }

        public void bind(Playlist playlist) {
            tvPlaylistName.setText(playlist.getName());
            tvTrackCount.setText(playlist.getTrackCount() + " bài hát");
            tvPlaylistDescription.setText(playlist.getDescription());

            // Set icon khác nhau cho playlist mặc định
            if (playlist.getName().equals("Bài hát yêu thích")) {
                ivPlaylistArt.setImageResource(R.drawable.ic_favorite);
            } else if (playlist.getName().equals("Nghe gần đây")) {
                ivPlaylistArt.setImageResource(R.drawable.ic_history);
            } else {
                ivPlaylistArt.setImageResource(R.drawable.ic_playlist);
            }
        }
    }

    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistLongClick(Playlist playlist);
    }
}