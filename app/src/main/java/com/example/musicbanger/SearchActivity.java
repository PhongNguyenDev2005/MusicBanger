package com.example.musicbanger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.musicbanger.adapters.SongAdapter;
import com.example.musicbanger.api.JamendoApi;
import com.example.musicbanger.model.Track;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private EditText etSearch;
    private ImageView btnBack, btnClear;
    private ProgressBar loadingProgress;
    private TextView tvSearchResultsTitle, tvEmptyState;
    private RecyclerView recyclerViewSearchResults, recyclerViewGenreSuggestions;
    private LinearLayout layoutSearchSuggestions;

    private SongAdapter searchResultsAdapter;
    private SongAdapter genreSuggestionsAdapter;
    private TextInputLayout searchInputLayout;

    private List<Track> searchResults = new ArrayList<>();
    private List<Track> genreSuggestions = new ArrayList<>();

    private Timer searchTimer;
    private static final int SEARCH_DELAY = 800; // milliseconds

    // Các từ khóa tìm kiếm gợi ý
    private final String[] SEARCH_SUGGESTIONS = {
            "rock", "pop", "jazz", "electronic", "acoustic",
            "piano", "guitar", "chill", "ambient", "dance"
    };

    // Cache cho API results
    private LruCache<String, List<Track>> apiCache = new LruCache<>(100);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        searchInputLayout = findViewById(R.id.searchInputLayout);
        initializeViews();
        setupRecyclerViews();
        setupSearchSuggestions();
        setupClickListeners();
        loadGenreSuggestions(); // Load đề xuất thể loại ngay khi mở
        setupHintBehavior();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        btnClear = findViewById(R.id.btnClear);
        loadingProgress = findViewById(R.id.loadingProgress);
        tvSearchResultsTitle = findViewById(R.id.tvSearchResultsTitle);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        recyclerViewSearchResults = findViewById(R.id.recyclerViewSearchResults);
        recyclerViewGenreSuggestions = findViewById(R.id.recyclerViewGenreSuggestions);
        layoutSearchSuggestions = findViewById(R.id.layoutSearchSuggestions);
    }

    private void setupRecyclerViews() {
        // Adapter cho kết quả tìm kiếm (set isHorizontal = false để dùng item_song_suggestion)
        searchResultsAdapter = new SongAdapter(searchResults, this, false);
        recyclerViewSearchResults.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewSearchResults.setAdapter(searchResultsAdapter);

        // Adapter cho đề xuất thể loại
        genreSuggestionsAdapter = new SongAdapter(genreSuggestions, this, false);
        recyclerViewGenreSuggestions.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewGenreSuggestions.setAdapter(genreSuggestionsAdapter);
    }

    private void setupSearchSuggestions() {
        // Thêm các chip gợi ý tìm kiếm
        for (String suggestion : SEARCH_SUGGESTIONS) {
            Chip chip = new Chip(this);
            chip.setText(suggestion);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                etSearch.setText(suggestion);
                performSearch(suggestion);
            });

            layoutSearchSuggestions.addView(chip);

            // Thêm margin giữa các chip
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) chip.getLayoutParams();
            params.setMargins(0, 0, 16, 8);
            chip.setLayoutParams(params);
        }
    }

    private void setupClickListeners() {
        // Nút back
        btnBack.setOnClickListener(v -> finish());

        // Nút clear search
        btnClear.setOnClickListener(v -> {
            etSearch.setText("");
            clearSearchResults();
        });

        // Text change listener với debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Hiển thị/ẩn nút clear
                btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                // Debounce search
                if (searchTimer != null) {
                    searchTimer.cancel();
                }

                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            String query = s.toString().trim();
                            if (query.length() >= 2) {
                                performSearch(query);
                            } else if (query.isEmpty()) {
                                clearSearchResults();
                            }
                        });
                    }
                }, SEARCH_DELAY);
            }
        });

        // Xử lý nút search trên bàn phím
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString().trim();
            if (query.length() >= 2) {
                performSearch(query);
            }
            return true;
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;

        // Check cache
        List<Track> cachedResults = apiCache.get("search_" + query);
        if (cachedResults != null) {
            Log.d("SearchActivity", "✅ Loaded from cache: " + query);
            processSearchResults(cachedResults, query);
            return;
        }

        Log.d("SearchActivity", "🔍 Searching for: " + query);
        showLoading(true);
        showEmptyState(false);

        JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
        String clientId = "06ac505c";

        api.searchTracks(
                clientId,
                "json",
                10, // Giảm limit để load nhanh
                query
        ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
            @Override
            public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                   Response<JamendoApi.JamendoResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                    if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                        Log.d("SearchActivity", "✅ Search success: " + jamendoTracks.size() + " results");
                        List<Track> tracks = new ArrayList<>();
                        for (JamendoApi.JamendoTrack jTrack : jamendoTracks) {
                            if (jTrack.audio != null && !jTrack.audio.isEmpty()) {
                                Track track = jTrack.toTrack();
                                if (track.getStreamUri() != null) {
                                    tracks.add(track);
                                }
                            }
                        }
                        apiCache.put("search_" + query, tracks);
                        processSearchResults(tracks, query);
                    } else {
                        showNoResults();
                    }
                } else {
                    showSearchError();
                }
            }

            @Override
            public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                showLoading(false);
                showSearchError();
            }
        });
    }

    private void processSearchResults(List<Track> tracks, String query) {
        searchResults.clear();
        searchResults.addAll(tracks);
        searchResultsAdapter.updateTracks(searchResults);
        tvSearchResultsTitle.setVisibility(tracks.isEmpty() ? View.GONE : View.VISIBLE);
        recyclerViewSearchResults.setVisibility(tracks.isEmpty() ? View.GONE : View.VISIBLE);
        tvEmptyState.setVisibility(tracks.isEmpty() ? View.VISIBLE : View.GONE);
        if (!tracks.isEmpty()) {
            tvSearchResultsTitle.setText("Kết quả tìm kiếm cho \"" + query + "\" (" + tracks.size() + ")");
            loadRelatedGenreSuggestions(query);
        } else {
            tvEmptyState.setText("Không tìm thấy kết quả nào");
        }
    }

    private void loadRelatedGenreSuggestions(String query) {
        String relatedGenre = getRelatedGenre(query);

        List<Track> cachedGenre = apiCache.get("genre_" + relatedGenre);
        if (cachedGenre != null) {
            processGenreSuggestions(cachedGenre, relatedGenre);
            return;
        }

        JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
        String clientId = "06ac505c";

        api.getTracksByGenre(
                clientId,
                "json",
                5, // Giảm limit
                relatedGenre
        ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
            @Override
            public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                   Response<JamendoApi.JamendoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                    if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                        List<Track> tracks = new ArrayList<>();
                        for (JamendoApi.JamendoTrack jTrack : jamendoTracks) {
                            if (jTrack.audio != null && !jTrack.audio.isEmpty()) {
                                Track track = jTrack.toTrack();
                                if (track.getStreamUri() != null) {
                                    tracks.add(track);
                                }
                            }
                        }
                        apiCache.put("genre_" + relatedGenre, tracks);
                        processGenreSuggestions(tracks, relatedGenre);
                    }
                }
            }

            @Override
            public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                Log.e("SearchActivity", "Genre suggestions failed: " + t.getMessage());
            }
        });
    }

    private String getRelatedGenre(String query) {
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.contains("rock")) return "rock";
        if (lowerQuery.contains("pop")) return "pop";
        if (lowerQuery.contains("jazz")) return "jazz";
        if (lowerQuery.contains("electronic") || lowerQuery.contains("edm")) return "electronic";
        if (lowerQuery.contains("acoustic")) return "acoustic";
        if (lowerQuery.contains("piano")) return "piano";
        if (lowerQuery.contains("guitar")) return "guitar";
        if (lowerQuery.contains("chill") || lowerQuery.contains("relax")) return "chillout";
        if (lowerQuery.contains("ambient")) return "ambient";
        if (lowerQuery.contains("dance")) return "dance";

        // Mặc định
        String[] defaultGenres = {"rock", "pop", "electronic", "instrumental"};
        return defaultGenres[new java.util.Random().nextInt(defaultGenres.length)];
    }

    private void loadGenreSuggestions() {
        String popularGenre = "popular";
        List<Track> cached = apiCache.get("genre_" + popularGenre);
        if (cached != null) {
            processGenreSuggestions(cached, popularGenre);
            return;
        }

        JamendoApi api = com.example.musicbanger.api.MusicApiService.getJamendoApi();
        String clientId = "06ac505c";

        api.getTracksByGenre(
                clientId,
                "json",
                10,
                popularGenre
        ).enqueue(new Callback<JamendoApi.JamendoResponse>() {
            @Override
            public void onResponse(Call<JamendoApi.JamendoResponse> call,
                                   Response<JamendoApi.JamendoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<JamendoApi.JamendoTrack> jamendoTracks = response.body().results;
                    if (jamendoTracks != null && !jamendoTracks.isEmpty()) {
                        List<Track> tracks = new ArrayList<>();
                        for (JamendoApi.JamendoTrack jTrack : jamendoTracks) {
                            if (jTrack.audio != null && !jTrack.audio.isEmpty()) {
                                Track track = jTrack.toTrack();
                                if (track.getStreamUri() != null) {
                                    tracks.add(track);
                                }
                            }
                        }
                        apiCache.put("genre_" + popularGenre, tracks);
                        processGenreSuggestions(tracks, "phổ biến");
                    }
                }
            }

            @Override
            public void onFailure(Call<JamendoApi.JamendoResponse> call, Throwable t) {
                Log.e("SearchActivity", "Default genre suggestions failed: " + t.getMessage());
            }
        });
    }

    private void processGenreSuggestions(List<Track> tracks, String genre) {
        genreSuggestions.clear();
        genreSuggestions.addAll(tracks);
        genreSuggestionsAdapter.updateTracks(genreSuggestions);
        TextView tvGenreTitle = findViewById(R.id.tvGenreSuggestionsTitle);
        tvGenreTitle.setText("Đề xuất " + genre + " khác");
    }

    private void clearSearchResults() {
        searchResults.clear();
        searchResultsAdapter.updateTracks(searchResults);
        tvSearchResultsTitle.setVisibility(View.GONE);
        recyclerViewSearchResults.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Nhập từ khóa để tìm kiếm bài hát");
        loadGenreSuggestions();
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        tvEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showNoResults() {
        tvSearchResultsTitle.setVisibility(View.VISIBLE);
        recyclerViewSearchResults.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Không tìm thấy kết quả nào");
        tvSearchResultsTitle.setText("Kết quả tìm kiếm");
    }

    private void showSearchError() {
        Toast.makeText(SearchActivity.this, "Lỗi kết nối, vui lòng thử lại", Toast.LENGTH_SHORT).show();
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Lỗi kết nối");
    }

    @Override
    public void onSongClick(int position, boolean isRecent) {
        try {
            List<Track> sourceList = isRecent ? searchResults : genreSuggestions;
            if (position >= 0 && position < sourceList.size()) {
                Track clickedTrack = sourceList.get(position);
                Log.d("SearchActivity", "🎵 Clicked track: " + clickedTrack.getTitle());

                List<Track> allTracks = new ArrayList<>();
                allTracks.addAll(searchResults);
                allTracks.addAll(genreSuggestions);
                MusicDataManager.getInstance().setTracks(allTracks);

                int globalPosition = allTracks.indexOf(clickedTrack);
                if (globalPosition != -1) {
                    MusicDataManager.getInstance().setCurrentPosition(globalPosition);

                    Intent intent = new Intent(SearchActivity.this, NowPlayingActivity.class);
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            Log.e("SearchActivity", "❌ Error in onSongClick: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khi chọn bài hát", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSongLongClick(int position, boolean isRecent) {
        List<Track> sourceList = isRecent ? searchResults : genreSuggestions;
        if (position >= 0 && position < sourceList.size()) {
            Track track = sourceList.get(position);
            Toast.makeText(this, "Đã chọn: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupHintBehavior() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    searchInputLayout.setHint(" ");
                } else {
                    searchInputLayout.setHint("Tìm kiếm bài hát, nghệ sĩ...");
                }
            }
        });

        etSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etSearch.getText().length() == 0) {
                searchInputLayout.setHint(" ");
            } else if (!hasFocus && etSearch.getText().length() == 0) {
                searchInputLayout.setHint("Tìm kiếm bài hát, nghệ sĩ...");
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    hideKeyboard(v);
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (etSearch.hasFocus() || etSearch.getText().length() > 0) {
            hideKeyboard(etSearch);
            etSearch.clearFocus();
            etSearch.setText("");
        } else {
            super.onBackPressed();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
            View view = getCurrentFocus();
            if (view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchTimer != null) {
            searchTimer.cancel();
        }
    }
}