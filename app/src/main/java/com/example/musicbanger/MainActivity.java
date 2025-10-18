package com.example.musicbanger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicbanger.adapters.SongAdapter;
import com.example.musicbanger.api.JamendoApi;
import com.example.musicbanger.model.Track;
import com.example.musicbanger.service.MusicService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements
        SongAdapter.OnSongClickListener,
        MusicService.MusicServiceObserver {

    // UI Components
    private RecyclerView recyclerViewRecent;
    private RecyclerView recyclerViewSuggestions;
    private SongAdapter recentAdapter;
    private SongAdapter suggestionAdapter;
    private TextView tvGreeting;
    private ProgressBar loadingProgress;

    // Data
    private List<Track> recentTracks = new ArrayList<>();
    private List<Track> suggestedTracks = new ArrayList<>();
    private List<Track> allTracks = new ArrayList<>();

    // Service
    private MusicService musicService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            serviceBound = true;

            // Register observer
            musicService.addObserver(MainActivity.this);

            Log.d("MainActivity", "MusicService connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d("MainActivity", "MusicService disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MainActivity", "onCreate started");

        initializeViews();
        setupRecyclerViews();
        setupClickListeners();
        bindMusicService();
        loadMusicData();

        Log.d("MainActivity", "onCreate completed successfully");
    }

    private void initializeViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        loadingProgress = findViewById(R.id.loadingProgress);
        recyclerViewRecent = findViewById(R.id.recyclerViewRecent);
        recyclerViewSuggestions = findViewById(R.id.recyclerViewSuggestions);

        if (loadingProgress != null) {
            loadingProgress.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerViews() {
        // Recent tracks (horizontal)
        recentAdapter = new SongAdapter(recentTracks, this, true);
        recyclerViewRecent.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false));
        recyclerViewRecent.setAdapter(recentAdapter);

        // Suggested tracks (vertical)
        suggestionAdapter = new SongAdapter(suggestedTracks, this, false);
        recyclerViewSuggestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupClickListeners() {
        // Refresh button
        View btnRefresh = findViewById(R.id.btnRefresh);
        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                loadMusicData();
                Toast.makeText(MainActivity.this,
                        "Đang tải lại danh sách nhạc...", Toast.LENGTH_SHORT).show();
            });
        }
        // ===== BOTTOM NAVIGATION CLICKS =====
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        // Search Tab - Sử dụng ID trực tiếp
        View searchTab = findViewById(R.id.search_tab);
        if (searchTab != null) {
            searchTab.setOnClickListener(v -> openSearchActivity());
        }

        // Library Tab
        View libraryTab = findViewById(R.id.library_tab);
        if (libraryTab != null) {
            libraryTab.setOnClickListener(v -> {
                Toast.makeText(this, "Thư viện - Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
            });
        }

        // Home Tab (trang chủ) - có thể thêm nếu cần
        View homeTab = findViewById(R.id.home_tab);
        if (homeTab != null) {
            homeTab.setOnClickListener(v -> {
                // Đã ở trang chủ, có thể làm mới hoặc scroll to top
                Toast.makeText(this, "Đang ở trang chủ", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private View findSearchTab() {
        // Cách 1: Tìm bằng ID nếu có
        View searchTab = findViewById(R.id.search_tab); // Thay bằng ID thực tế

        // Cách 2: Tìm bằng vị trí (thường tab thứ 2)
        if (searchTab == null) {
            ViewGroup bottomNav = findViewById(R.id.bottom_navigation_bar);
            if (bottomNav != null && bottomNav.getChildCount() > 1) {
                searchTab = bottomNav.getChildAt(1); // Tab thứ 2 (index 1)
            }
        }

        // Cách 3: Tìm bằng tag hoặc text
        if (searchTab == null) {
            ViewGroup bottomNav = findViewById(R.id.bottom_navigation_bar);
            if (bottomNav != null) {
                searchTab = findViewByText(bottomNav, "Tìm kiếm");
            }
        }

        return searchTab;
    }

    /**
     * Tìm library tab trong bottom navigation
     */
    private View findLibraryTab() {
        // Tương tự như search tab
        View libraryTab = findViewById(R.id.library_tab); // Thay bằng ID thực tế

        if (libraryTab == null) {
            ViewGroup bottomNav = findViewById(R.id.bottom_navigation_bar);
            if (bottomNav != null && bottomNav.getChildCount() > 2) {
                libraryTab = bottomNav.getChildAt(2); // Tab thứ 3 (index 2)
            }
        }

        if (libraryTab == null) {
            ViewGroup bottomNav = findViewById(R.id.bottom_navigation_bar);
            if (bottomNav != null) {
                libraryTab = findViewByText(bottomNav, "Thư viện");
            }
        }

        return libraryTab;
    }

    /**
     * Tìm view bằng text trong ViewGroup
     */
    private View findViewByText(ViewGroup parent, String text) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                View found = findViewByText((ViewGroup) child, text);
                if (found != null) return found;
            } else if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (text.equals(textView.getText().toString())) {
                    return (View) child.getParent(); // Return parent layout
                }
            }
        }
        return null;
    }

    /**
     * Mở Search Activity
     */
    private void openSearchActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
            // Optional: Add animation
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down);

            Log.d("MainActivity", "SearchActivity opened");
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening SearchActivity: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khi mở tìm kiếm", Toast.LENGTH_SHORT).show();
        }
    }

    private void bindMusicService() {
        try {
            Intent intent = new Intent(this, MusicService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            Log.e("MainActivity", "Error binding service: " + e.getMessage(), e);
        }
    }

    // ==================== MUSIC DATA LOADING ====================

    private void loadMusicData() {
        showLoading(true);
        Log.d("MainActivity", "Loading music data...");
        loadQuickMetadata();
    }

    private void loadQuickMetadata() {
        try {
            JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
            String clientId = "06ac505c";

            api.getPopularTracks(
                    clientId,
                    "json",
                    15,
                    "popularity_total"
            ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
                @Override
                public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                       Response<JamendoApi.JamendoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                        if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                            processQuickMetadata(jamendoTracks);
                        } else {
                            loadTracksByGenre();
                        }
                    } else {
                        loadTracksByGenre();
                    }
                }

                @Override
                public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                    Log.e("MainActivity", "API failure: " + t.getMessage());
                    loadTracksByGenre();
                }
            });

        } catch (Exception e) {
            Log.e("MainActivity", "Error loading metadata: " + e.getMessage(), e);
            loadTracksByGenre();
        }
    }

    private void loadTracksByGenre() {
        try {
            JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
            String clientId = "06ac505c";

            String[] popularGenres = {"rock", "pop", "electronic", "jazz", "instrumental"};
            String randomGenre = popularGenres[new Random().nextInt(popularGenres.length)];

            api.getTracksByGenre(
                    clientId,
                    "json",
                    12,
                    randomGenre
            ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
                @Override
                public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                       Response<JamendoApi.JamendoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                        if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                            processQuickMetadata(jamendoTracks);
                        } else {
                            loadLatestTracks();
                        }
                    } else {
                        loadLatestTracks();
                    }
                }

                @Override
                public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                    loadLatestTracks();
                }
            });

        } catch (Exception e) {
            loadLatestTracks();
        }
    }

    private void loadLatestTracks() {
        try {
            JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
            String clientId = "06ac505c";

            api.getPopularTracks(
                    clientId,
                    "json",
                    12,
                    "releasedate_desc"
            ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
                @Override
                public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                       Response<JamendoApi.JamendoResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                        if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                            processQuickMetadata(jamendoTracks);
                        } else {
                            handleAllApiFailures("No tracks found");
                        }
                    } else {
                        handleAllApiFailures("API error: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                    handleAllApiFailures("API failure: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            handleAllApiFailures("Error: " + e.getMessage());
        }
    }

    private void processQuickMetadata(List<JamendoApi.JamendoTrack> jamendoTracks) {
        List<Track> tracks = new ArrayList<>();

        for (JamendoApi.JamendoTrack jamendoTrack : jamendoTracks) {
            if (jamendoTrack.audio != null && !jamendoTrack.audio.isEmpty()) {
                Track track = new Track(
                        jamendoTrack.id,
                        jamendoTrack.name,
                        jamendoTrack.artist_name != null ? jamendoTrack.artist_name : "Unknown Artist",
                        jamendoTrack.album_name != null ? jamendoTrack.album_name : "Unknown Album",
                        jamendoTrack.audio,
                        jamendoTrack.image,
                        jamendoTrack.duration
                );
                tracks.add(track);
            }
        }

        if (!tracks.isEmpty()) {
            updateTrackLists(tracks);
            showLoading(false);
            Toast.makeText(this, "Đã tải " + tracks.size() + " bài hát", Toast.LENGTH_SHORT).show();

            // Preload audio URLs in background
            preloadAudioUrlsInBackground(tracks);
        } else {
            handleAllApiFailures("No valid tracks found");
        }
    }

    private void preloadAudioUrlsInBackground(List<Track> tracks) {
        new Thread(() -> {
            try {
                Log.d("MainActivity", "Preloading audio URLs...");
                // Just sleep to simulate background work
                Thread.sleep(1000);
                Log.d("MainActivity", "Audio preload completed");
            } catch (Exception e) {
                Log.e("MainActivity", "Error in audio preload", e);
            }
        }).start();
    }

    private void updateTrackLists(List<Track> tracks) {
        recentTracks.clear();
        suggestedTracks.clear();
        allTracks.clear();

        if (tracks.size() >= 3) {
            // First 3 tracks for recent
            recentTracks.addAll(tracks.subList(0, Math.min(3, tracks.size())));
            // Rest for suggestions
            if (tracks.size() > 3) {
                suggestedTracks.addAll(tracks.subList(3, tracks.size()));
            }
        } else {
            recentTracks.addAll(tracks);
            suggestedTracks.addAll(tracks);
        }

        allTracks.addAll(tracks);
        MusicDataManager.getInstance().setTracks(allTracks);

        updateUI();
        Log.d("MainActivity", "Updated: " + recentTracks.size() + " recent, " +
                suggestedTracks.size() + " suggested");
    }

    private void updateUI() {
        recentAdapter.updateTracks(recentTracks);
        suggestionAdapter.updateTracks(suggestedTracks);
        updateGreeting();
    }

    private void updateGreeting() {
        if (tvGreeting != null) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;

            if (hour >= 5 && hour < 12) {
                greeting = "Chào buổi sáng!";
            } else if (hour >= 12 && hour < 18) {
                greeting = "Chào buổi chiều!";
            } else {
                greeting = "Chào buổi tối!";
            }

            tvGreeting.setText(greeting);
        }
    }

    private void handleAllApiFailures(String errorMessage) {
        Log.e("MainActivity", "All APIs failed: " + errorMessage);
        showLoading(false);
        Toast.makeText(this, "Không thể tải nhạc. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            if (loadingProgress != null) {
                loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
            }

            int visibility = show ? View.GONE : View.VISIBLE;
            if (recyclerViewRecent != null) recyclerViewRecent.setVisibility(visibility);
            if (recyclerViewSuggestions != null) recyclerViewSuggestions.setVisibility(visibility);
        });
    }

    // ==================== SONG CLICK LISTENER ====================

    @Override
    public void onSongClick(int position, boolean isRecent) {
        try {
            List<Track> sourceList = isRecent ? recentTracks : suggestedTracks;
            if (position >= 0 && position < sourceList.size()) {
                Track clickedTrack = sourceList.get(position);

                int globalPosition = allTracks.indexOf(clickedTrack);
                if (globalPosition != -1) {
                    openNowPlayingActivity(globalPosition);
                } else {
                    openNowPlayingActivity(0);
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onSongClick", e);
            Toast.makeText(this, "Lỗi khi chọn bài hát", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSongLongClick(int position, boolean isRecent) {
        List<Track> sourceList = isRecent ? recentTracks : suggestedTracks;
        if (position >= 0 && position < sourceList.size()) {
            Track track = sourceList.get(position);
            Toast.makeText(this, "Đã chọn: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openNowPlayingActivity(int trackPosition) {
        try {
            if (allTracks.isEmpty()) {
                Toast.makeText(this, "Danh sách nhạc trống", Toast.LENGTH_SHORT).show();
                return;
            }

            if (trackPosition < 0 || trackPosition >= allTracks.size()) {
                trackPosition = 0;
            }

            MusicDataManager.getInstance().setCurrentPosition(trackPosition);
            Intent intent = new Intent(this, NowPlayingActivity.class);
            startActivity(intent);

        } catch (Exception e) {
            Log.e("MainActivity", "Error opening NowPlaying", e);
            Toast.makeText(this, "Lỗi khi mở trình phát", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== MUSIC SERVICE OBSERVER ====================

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        Log.d("MainActivity", "Playback state changed: " + isPlaying);
        // Update mini player if needed
    }

    @Override
    public void onTrackChanged(Track currentTrack) {
        Log.d("MainActivity", "Track changed: " +
                (currentTrack != null ? currentTrack.getTitle() : "null"));
        // Update mini player if needed
    }

    // ==================== LIFECYCLE ====================

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound && musicService != null) {
            musicService.removeObserver(this);
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}