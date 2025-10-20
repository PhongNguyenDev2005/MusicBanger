package com.example.musicbanger;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicbanger.adapters.PlaylistAdapter;
import com.example.musicbanger.manager.UserPlaylistManager;
import com.example.musicbanger.model.Playlist;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class PlaylistManagerActivity extends AppCompatActivity implements PlaylistAdapter.OnPlaylistClickListener {

    private RecyclerView recyclerViewPlaylists;
    private PlaylistAdapter playlistAdapter;
    private List<Playlist> playlists;
    private TextView tvEmptyState;
    private ImageView btnBack, btnCreatePlaylist;
    private FloatingActionButton fabAddPlaylist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_manager);

        setupViews();
        setupClickListeners();
        loadPlaylists();
    }

    private void setupViews() {
        recyclerViewPlaylists = findViewById(R.id.recyclerViewPlaylists);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        btnBack = findViewById(R.id.btnBack);
        btnCreatePlaylist = findViewById(R.id.btnCreatePlaylist);
        fabAddPlaylist = findViewById(R.id.fabAddPlaylist);

        recyclerViewPlaylists.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    private void loadPlaylists() {
        playlists = UserPlaylistManager.getInstance().getAllPlaylists();
        updateEmptyState();

        playlistAdapter = new PlaylistAdapter(playlists, this);
        recyclerViewPlaylists.setAdapter(playlistAdapter);
    }

    private void updateEmptyState() {
        if (playlists.isEmpty() || (playlists.size() <= 2 &&
                playlists.get(0).getName().equals("Bài hát yêu thích") &&
                playlists.get(1).getName().equals("Nghe gần đây"))) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewPlaylists.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewPlaylists.setVisibility(View.VISIBLE);
        }
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tạo playlist mới");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Tên playlist");
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String playlistName = input.getText().toString().trim();
            if (!playlistName.isEmpty()) {
                createNewPlaylist(playlistName);
            } else {
                Toast.makeText(this, "Vui lòng nhập tên playlist", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null);

        builder.show();
    }

    private void createNewPlaylist(String name) {
        UserPlaylistManager.getInstance().createPlaylist(name, "Playlist của tôi");
        loadPlaylists(); // Reload danh sách
        Toast.makeText(this, "Đã tạo playlist: " + name, Toast.LENGTH_SHORT).show();
    }

    // Implement interface từ PlaylistAdapter
    @Override
    public void onPlaylistClick(Playlist playlist) {
        openPlaylistDetail(playlist);
    }

    @Override
    public void onPlaylistLongClick(Playlist playlist) {
        showPlaylistOptions(playlist);
    }

    private void openPlaylistDetail(Playlist playlist) {
        Intent intent = new Intent(this, PlaylistDetailActivity.class);
        intent.putExtra("playlist_id", playlist.getId());
        intent.putExtra("playlist_name", playlist.getName());
        startActivity(intent);
    }

    private void showPlaylistOptions(Playlist playlist) {
        // Không cho phép xóa playlist mặc định
        if (playlist.getName().equals("Bài hát yêu thích") ||
                playlist.getName().equals("Nghe gần đây")) {
            Toast.makeText(this, "Không thể xóa playlist mặc định", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Đổi tên", "Xóa playlist"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(playlist.getName())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showRenamePlaylistDialog(playlist);
                    } else if (which == 1) {
                        showDeletePlaylistConfirmation(playlist);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi tên playlist");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(playlist.getName());
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(playlist.getName())) {
                UserPlaylistManager.getInstance().renamePlaylist(playlist.getId(), newName);
                loadPlaylists(); // Reload
                Toast.makeText(this, "Đã đổi tên playlist", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", null);

        builder.show();
    }

    private void showDeletePlaylistConfirmation(Playlist playlist) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa playlist")
                .setMessage("Bạn có chắc muốn xóa playlist \"" + playlist.getName() + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    UserPlaylistManager.getInstance().deletePlaylist(playlist.getId());
                    loadPlaylists(); // Reload
                    Toast.makeText(this, "Đã xóa playlist", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPlaylists(); // Reload khi quay lại từ playlist detail
    }
}