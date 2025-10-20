package com.example.musicbanger.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicbanger.R;
import com.example.musicbanger.manager.UserPlaylistManager;
import com.example.musicbanger.model.Track;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Track> tracks;
    private final OnSongClickListener listener;
    private final boolean isHorizontal;
    private OnSongMenuClickListener menuClickListener;

    // Thêm biến để kiểm soát hiển thị option xóa
    private boolean showRemoveOption = false;

    public SongAdapter(List<Track> tracks, OnSongClickListener listener, boolean isHorizontal) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
        this.listener = listener;
        this.isHorizontal = isHorizontal;

        // DEBUG LOG
        android.util.Log.d("SongAdapter", "Adapter created - Tracks: " + this.tracks.size() +
                ", Listener: " + (listener != null) +
                ", Horizontal: " + isHorizontal);
    }

    // Phương thức để bật/tắt option xóa
    public void setShowRemoveOption(boolean showRemoveOption) {
        this.showRemoveOption = showRemoveOption;
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

        // Xử lý click menu (nút 3 chấm) - CHỈ cho layout vertical
        if (holder.ivMore != null) {
            holder.ivMore.setOnClickListener(v -> {
                showSongMenu(holder.itemView.getContext(), track, position, holder.ivMore);
            });
        }

        // Click vào item để phát nhạc
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(position, isHorizontal);
            }
        });

        // Long click
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onSongLongClick(position, isHorizontal);
                return true;
            }
            return false;
        });
    }

    private void showSongMenu(Context context, Track track, int position, View anchorView) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.inflate(R.menu.song_context_menu);

        // Thêm option xóa nếu được yêu cầu
        if (showRemoveOption) {
            MenuItem removeItem = popupMenu.getMenu().add("Xóa khỏi playlist");
            removeItem.setOnMenuItemClickListener(item -> {
                if (menuClickListener != null) {
                    menuClickListener.onRemoveFromPlaylist(track);
                }
                return true;
            });
        }

        // Cập nhật trạng thái yêu thích
        MenuItem favoriteItem = popupMenu.getMenu().findItem(R.id.menu_favorite);

        // KIỂM TRA YÊU THÍCH BẰNG CALLBACK ĐỂ TRÁNH BLOCK UI
        UserPlaylistManager.getInstance().isFavorite(track, new UserPlaylistManager.FavoriteCallback() {
            @Override
            public void onFavoriteChecked(boolean isFavorite) {
                ((Activity) context).runOnUiThread(() -> {
                    if (isFavorite) {
                        favoriteItem.setTitle("Bỏ yêu thích");
                        favoriteItem.setIcon(R.drawable.ic_favorite);
                    } else {
                        favoriteItem.setTitle("Yêu thích");
                        favoriteItem.setIcon(R.drawable.ic_favorite);
                    }
                });
            }
        });

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            android.util.Log.d("SongAdapter", "Menu item clicked: " + item.getTitle());

            if (id == R.id.menu_favorite) {
                android.util.Log.d("SongAdapter", "Favorite menu clicked for track: " + track.getTitle());
                if (menuClickListener != null) {
                    menuClickListener.onToggleFavorite(track);
                    android.util.Log.d("SongAdapter", "Menu click listener called");
                } else {
                    android.util.Log.e("SongAdapter", "Menu click listener is NULL!");
                }
                return true;
            } else if (id == R.id.menu_add_to_playlist) {
                android.util.Log.d("SongAdapter", "Add to playlist menu clicked");
                if (menuClickListener != null) {
                    menuClickListener.onAddToPlaylist(track);
                }
                return true;
            } else if (id == R.id.menu_add_to_queue) {
                if (menuClickListener != null) {
                    menuClickListener.onAddToQueue(track);
                }
                return true;
            } else if (showRemoveOption && item.getTitle().equals("Xóa khỏi playlist")) {
                if (menuClickListener != null) {
                    menuClickListener.onRemoveFromPlaylist(track);
                }
                return true;
            }
            return false;
        });

        popupMenu.show();
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

    public interface OnSongMenuClickListener {
        void onAddToPlaylist(Track track);
        void onToggleFavorite(Track track);
        void onAddToQueue(Track track);
        void onRemoveFromPlaylist(Track track);
    }

    public void setOnSongMenuClickListener(OnSongMenuClickListener listener) {
        this.menuClickListener = listener;
    }

    public interface OnSongClickListener {
        void onSongClick(int position, boolean isRecent);
        void onSongLongClick(int position, boolean isRecent);
    }
}