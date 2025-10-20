package com.example.musicbanger;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicbanger.adapters.SongAdapter;
import com.example.musicbanger.manager.UserPlaylistManager;
import com.example.musicbanger.model.Playlist;
import com.example.musicbanger.model.Track;
import com.example.musicbanger.service.MusicService;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailActivity extends AppCompatActivity implements
        SongAdapter.OnSongClickListener,
        SongAdapter.OnSongMenuClickListener {

    private RecyclerView recyclerViewTracks;
    private SongAdapter songAdapter;
    private List<Track> tracks;
    private Playlist currentPlaylist;

    private TextView tvPlaylistName, tvTrackCount, tvPlaylistDescription, tvEmptyState;
    private ImageView btnBack, btnPlayAllHeader, ivPlaylistArt;
    private View btnShufflePlay, btnPlayAllMain; // ĐÃ SỬA TÊN

    private MusicService musicService;
    private boolean serviceBound = false;

    private String playlistId;
    private String playlistName;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        // Lấy thông tin playlist từ intent
        playlistId = getIntent().getStringExtra("playlist_id");
        playlistName = getIntent().getStringExtra("playlist_name");

        setupViews();
        setupClickListeners();
        loadPlaylistData();

        // Kết nối service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void setupViews() {
        recyclerViewTracks = findViewById(R.id.recyclerViewTracks);
        tvPlaylistName = findViewById(R.id.tvPlaylistName);
        tvTrackCount = findViewById(R.id.tvTrackCount);
        tvPlaylistDescription = findViewById(R.id.tvPlaylistDescription);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBack = findViewById(R.id.btnBack);
        btnPlayAllHeader = findViewById(R.id.btnPlayAllHeader); // ĐÃ SỬA TÊN
        ivPlaylistArt = findViewById(R.id.ivPlaylistArt);
        btnShufflePlay = findViewById(R.id.btnShufflePlay);
        btnPlayAllMain = findViewById(R.id.btnPlayAllMain); // ĐÃ SỬA TÊN

        recyclerViewTracks.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayAllHeader.setOnClickListener(v -> playPlaylist(false));

        btnPlayAllMain.setOnClickListener(v -> playPlaylist(false)); // ĐÃ SỬA TÊN

        btnShufflePlay.setOnClickListener(v -> playPlaylist(true));
    }

    private void loadPlaylistData() {
        if (playlistId != null) {
            currentPlaylist = UserPlaylistManager.getInstance().getPlaylistById(playlistId);
            if (currentPlaylist != null) {
                updatePlaylistHeader();
                loadTracks();
            }
        }
    }

    private void updatePlaylistHeader() {
        tvPlaylistName.setText(currentPlaylist.getName());
        tvTrackCount.setText(currentPlaylist.getTrackCount() + " bài hát");
        tvPlaylistDescription.setText(currentPlaylist.getDescription());

        // Set icon playlist
        if (currentPlaylist.getName().equals("Bài hát yêu thích")) {
            ivPlaylistArt.setImageResource(R.drawable.ic_favorite);
        } else if (currentPlaylist.getName().equals("Nghe gần đây")) {
            ivPlaylistArt.setImageResource(R.drawable.ic_history);
        } else {
            ivPlaylistArt.setImageResource(R.drawable.ic_playlist);
        }
    }

    private void loadTracks() {
        if (currentPlaylist != null) {
            tracks = new ArrayList<>(currentPlaylist.getTracks()); // TẠO DANH SÁCH MỚI
            updateEmptyState();

            songAdapter = new SongAdapter(tracks, this, false);
            songAdapter.setOnSongMenuClickListener(this);

            // BẬT option xóa cho playlist detail (trừ playlist mặc định)
            if (!currentPlaylist.getName().equals("Bài hát yêu thích") &&
                    !currentPlaylist.getName().equals("Nghe gần đây")) {
                songAdapter.setShowRemoveOption(true);
            }

            recyclerViewTracks.setAdapter(songAdapter);
        }
    }

    private void updateEmptyState() {
        if (tracks.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewTracks.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewTracks.setVisibility(View.VISIBLE);
        }
    }

    private void playPlaylist(boolean shuffle) {
        if (currentPlaylist != null && !currentPlaylist.getTracks().isEmpty()) {
            if (serviceBound && musicService != null) {
                if (shuffle) {
                    // PHÁT NGẪU NHIÊN CHỈ TRONG PLAYLIST NÀY
                    musicService.shufflePlayPlaylist(currentPlaylist);
                } else {
                    // PHÁT BÌNH THƯỜNG
                    musicService.playUserPlaylist(currentPlaylist, 0);
                }

                // Mở NowPlayingActivity
                Intent intent = new Intent(this, NowPlayingActivity.class);
                startActivity(intent);

                Toast.makeText(this,
                        shuffle ? "Đang phát ngẫu nhiên: " + currentPlaylist.getName()
                                : "Đang phát playlist: " + currentPlaylist.getName(),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Playlist trống", Toast.LENGTH_SHORT).show();
        }
    }

    // Implement SongAdapter.OnSongClickListener
    @Override
    public void onSongClick(int position, boolean isRecent) {
        if (serviceBound && musicService != null && currentPlaylist != null) {
            musicService.playUserPlaylist(currentPlaylist, position);

            Intent intent = new Intent(this, NowPlayingActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onSongLongClick(int position, boolean isRecent) {
        // Xử lý long click nếu cần
    }

    // Implement SongAdapter.OnSongMenuClickListener - THÊM PHƯƠNG THỨC BỊ THIẾU
    @Override
    public void onRemoveFromPlaylist(Track track) {
        removeTrackFromPlaylist(track);
    }

    @Override
    public void onAddToPlaylist(Track track) {
        // Có thể thêm vào playlist khác
        showAddToOtherPlaylistDialog(track);
    }

    @Override
    public void onToggleFavorite(Track track) {
        UserPlaylistManager.getInstance().toggleFavorite(track);
        boolean isFavorite = UserPlaylistManager.getInstance().isFavorite(track);

        Toast.makeText(this,
                isFavorite ? "Đã thêm vào yêu thích" : "Đã bỏ khỏi yêu thích",
                Toast.LENGTH_SHORT).show();

        // Reload nếu đang ở playlist yêu thích
        if (currentPlaylist.getName().equals("Bài hát yêu thích")) {
            loadPlaylistData();
        }
    }

    @Override
    public void onAddToQueue(Track track) {
        if (serviceBound && musicService != null) {
            musicService.getPlaylistManager().getPlaylist().add(
                    musicService.getPlaylistManager().getCurrentIndex() + 1, track
            );
            Toast.makeText(this, "Đã thêm vào hàng đợi", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddToOtherPlaylistDialog(Track track) {
        List<Playlist> allPlaylists = UserPlaylistManager.getInstance().getAllPlaylists();
        // Lọc bỏ playlist hiện tại
        List<Playlist> otherPlaylists = new ArrayList<>();
        for (Playlist playlist : allPlaylists) {
            if (!playlist.getId().equals(currentPlaylist.getId())) {
                otherPlaylists.add(playlist);
            }
        }

        if (otherPlaylists.isEmpty()) {
            Toast.makeText(this, "Không có playlist khác", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNames = new String[otherPlaylists.size()];
        for (int i = 0; i < otherPlaylists.size(); i++) {
            playlistNames[i] = otherPlaylists.get(i).getName();
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Thêm vào playlist")
                .setItems(playlistNames, (dialog, which) -> {
                    Playlist selectedPlaylist = otherPlaylists.get(which);
                    UserPlaylistManager.getInstance().addTrackToPlaylist(selectedPlaylist.getId(), track);
                    Toast.makeText(this, "Đã thêm vào " + selectedPlaylist.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    // Xử lý xóa bài hát khỏi playlist
    private void removeTrackFromPlaylist(Track track) {
        if (currentPlaylist != null) {
            UserPlaylistManager.getInstance().removeTrackFromPlaylist(currentPlaylist.getId(), track);
            loadPlaylistData(); // Reload
            Toast.makeText(this, "Đã xóa khỏi playlist", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylistData(); // Reload khi quay lại

        // ĐẢM BẢO CẬP NHẬT DỮ LIỆU MỚI NHẤT
        if (playlistId != null) {
            currentPlaylist = UserPlaylistManager.getInstance().getPlaylistById(playlistId);
            if (currentPlaylist != null) {
                updatePlaylistHeader();
                loadTracks();
            }
        }
    }}