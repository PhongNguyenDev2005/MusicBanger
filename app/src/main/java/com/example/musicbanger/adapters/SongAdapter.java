package com.example.musicbanger.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicbanger.R;
import com.example.musicbanger.model.Track;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Track> tracks;
    private final OnSongClickListener listener;
    private final boolean isHorizontal;

    public SongAdapter(List<Track> tracks, OnSongClickListener listener, boolean isHorizontal) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        this.listener = listener;
        this.isHorizontal = isHorizontal;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isHorizontal ? R.layout.item_recent_listen : R.layout.item_song_suggestion;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new SongViewHolder(view, isHorizontal);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(position, isHorizontal);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSongLongClick(position, isHorizontal);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public void updateTracks(List<Track> newTracks) {
        this.tracks = newTracks != null ? newTracks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSongTitle;
        private final TextView tvArtistName;
        private final TextView tvAlbumName;
        private final ImageView ivAlbumArt;
        private final ImageView ivMore;
        private final boolean isHorizontalLayout;

        public SongViewHolder(@NonNull View itemView, boolean isHorizontal) {
            super(itemView);
            this.isHorizontalLayout = isHorizontal;

            // Tìm view dựa trên loại layout
            if (isHorizontal) {
                // Layout horizontal (item_recent_listen)
                tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
                tvArtistName = itemView.findViewById(R.id.tvArtistName);
                tvAlbumName = itemView.findViewById(R.id.tvAlbumName);
                ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
                ivMore = null; // Layout horizontal không có nút more
            } else {
                // Layout vertical (item_song_suggestion)
                tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
                tvArtistName = itemView.findViewById(R.id.tvArtistName);
                tvAlbumName = null; // Layout vertical không có tvAlbumName riêng
                ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
                ivMore = itemView.findViewById(R.id.ivMore);
            }
        }

        public void bind(Track track) {
            if (isHorizontalLayout) {
                // Bind cho layout horizontal
                if (tvSongTitle != null) {
                    tvSongTitle.setText(track.getTitle());
                }
                if (tvArtistName != null) {
                    tvArtistName.setText(track.getArtistName());
                }
                if (tvAlbumName != null) {
                    tvAlbumName.setText(track.getAlbumName());
                }
            } else {
                // Bind cho layout vertical
                if (tvSongTitle != null) {
                    tvSongTitle.setText(track.getTitle());
                }
                if (tvArtistName != null) {
                    tvArtistName.setText(track.getArtistName());
                }
                // Layout vertical không hiển thị album name riêng
            }

            // Load ảnh album art (chung cho cả 2 layout)
            if (ivAlbumArt != null && track.getArtworkUri() != null) {
                Glide.with(itemView.getContext())
                        .load(track.getArtworkUri())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(ivAlbumArt);
            } else if (ivAlbumArt != null) {
                ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            }
        }
    }

    public interface OnSongClickListener {
        void onSongClick(int position, boolean isRecent);
        void onSongLongClick(int position, boolean isRecent);
    }
}