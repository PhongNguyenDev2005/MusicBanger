package com.example.musicbanger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.Player;
import com.example.musicbanger.model.Track;
import com.example.musicbanger.service.MusicService;

import java.util.List;

public class NowPlayingActivity extends AppCompatActivity {

    private static final String TAG = "NowPlayingActivity";

    // UI Components
    private ImageView ivAlbumArt, btnPlayPause, btnBack, btnNext, btnPrevious, btnShuffle, btnRepeat, btnMenu;
    private TextView tvSongTitle, tvArtistName, tvCurrentTime, tvTotalTime;
    private ConstraintLayout seekBarLayout;
    private View seekBarBackground, seekBarProgress, seekThumb, seekThumbHitbox;
    private MusicService.MusicServiceObserver musicObserver;

    // Music service and binding
    private MusicService musicService;
    private boolean bound = false;
    private List<Track> trackList;
    private int currentPosition;

    // Handler for updating progress
    private final Handler handler = new Handler();
    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (bound && musicService != null && musicService.getPlayer() != null) {
                Player player = musicService.getPlayer();
                long currentPos = player.getCurrentPosition();
                long duration = player.getDuration();
                if (duration > 0) {
                    float progressPercent = (float) currentPos / duration;
                    updateProgressView(progressPercent);
                    tvCurrentTime.setText(formatTime((int) (currentPos / 1000)));
                    tvTotalTime.setText(formatTime((int) (duration / 1000)));
                }
            }
            handler.postDelayed(this, 50);
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.LocalBinder b = (MusicService.LocalBinder) binder;
            musicService = b.getService();
            bound = true;

            // TẠO VÀ THÊM OBSERVER
            musicObserver = new MusicService.MusicServiceObserver() {
                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    runOnUiThread(() -> {
                        updatePlayPauseButton(isPlaying);
                        Log.d(TAG, "Playback state changed: " + (isPlaying ? "Playing" : "Paused"));
                    });
                }

                @Override
                public void onTrackChanged(Track currentTrack) {
                    runOnUiThread(() -> {
                        updateUI();
                        Log.d(TAG, "Track changed to: " + (currentTrack != null ? currentTrack.getTitle() : "null"));
                    });
                }
            };

            // ĐĂNG KÝ OBSERVER
            musicService.addObserver(musicObserver);

            // CẬP NHẬT UI NGAY LẬP TỨC
            runOnUiThread(() -> {
                updateUI();

                // KIỂM TRA TRẠNG THÁI SAU 500ms ĐỂ ĐẢM BẢO PLAYER ĐÃ SẴN SÀNG
                handler.postDelayed(() -> {
                    if (musicService != null) {
                        boolean isActuallyPlaying = musicService.isActuallyPlaying();
                        updatePlayPauseButton(isActuallyPlaying);
                        Log.d(TAG, "Delayed playback state check: " + (isActuallyPlaying ? "PLAYING" : "PAUSED"));

                        // Nếu vẫn không playing, thử kiểm tra lại sau 1 giây nữa
                        if (!isActuallyPlaying) {
                            handler.postDelayed(() -> {
                                if (musicService != null) {
                                    boolean finalCheck = musicService.isActuallyPlaying();
                                    updatePlayPauseButton(finalCheck);
                                    Log.d(TAG, "Final playback state check: " + (finalCheck ? "PLAYING" : "PAUSED"));
                                }
                            }, 1000);
                        }
                    }
                }, 500);
            });

            // Bắt đầu cập nhật progress
            handler.post(updateProgress);

            Log.d(TAG, "Service connected and observer registered");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (musicService != null && musicObserver != null) {
                musicService.removeObserver(musicObserver);
            }
            bound = false;
            Log.d(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);

        initializeViews();
        getTrackData();
        setupClickListeners(); // CHỈ GỌI 1 LẦN

        // Kết nối service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        setupSeekBar();
    }

    private void initializeViews() {
        try {
            ivAlbumArt = findViewById(R.id.ivAlbumArt);
            btnBack = findViewById(R.id.btnBack);
            btnMenu = findViewById(R.id.btnMenu);
            tvSongTitle = findViewById(R.id.tvSongTitle);
            tvArtistName = findViewById(R.id.tvArtistName);
            tvCurrentTime = findViewById(R.id.tvCurrentTime);
            tvTotalTime = findViewById(R.id.tvTotalTime);
            btnNext = findViewById(R.id.btnNext);
            btnPrevious = findViewById(R.id.btnPrevious);
            btnShuffle = findViewById(R.id.btnShuffle);
            btnRepeat = findViewById(R.id.btnRepeat);
            seekBarLayout = findViewById(R.id.seekBarLayout);
            seekBarBackground = findViewById(R.id.seekBarBackground);
            seekBarProgress = findViewById(R.id.seekBarProgress);
            seekThumb = findViewById(R.id.seekThumb);
            seekThumbHitbox = findViewById(R.id.seekThumbHitbox);

            View playPauseCard = findViewById(R.id.btnPlayPause);
            if (playPauseCard != null) {
                btnPlayPause = playPauseCard.findViewById(R.id.ivPlayPauseIcon);
            }

            // Back button
            btnBack.setOnClickListener(v -> finish());

            // Menu button
            btnMenu.setOnClickListener(v -> showMenuOptions());

            Log.d(TAG, "Khởi tạo giao diện thành công");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi khởi tạo giao diện: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSeekBar() {
        seekThumbHitbox.setOnTouchListener(new View.OnTouchListener() {
            float initialX;
            float touchDownX;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (bound && musicService != null && musicService.getPlayer() != null) {
                    Player player = musicService.getPlayer();
                    float totalWidth = seekBarLayout.getWidth() - seekThumb.getWidth();
                    if (totalWidth <= 0) return false;

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            handler.removeCallbacks(updateProgress);
                            initialX = seekThumb.getTranslationX();
                            touchDownX = event.getRawX();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float deltaX = event.getRawX() - touchDownX;
                            float newX = initialX + deltaX;
                            newX = Math.max(0, Math.min(newX, totalWidth));

                            seekThumb.setTranslationX(newX);
                            seekThumbHitbox.setTranslationX(newX);

                            float progressPercent = newX / totalWidth;
                            updateProgressView(progressPercent);

                            long newPosition = (long) (progressPercent * player.getDuration());
                            tvCurrentTime.setText(formatTime((int) (newPosition / 1000)));
                            return true;

                        case MotionEvent.ACTION_UP:
                            float finalProgress = (seekThumb.getTranslationX() / totalWidth);
                            long finalPosition = (long) (finalProgress * player.getDuration());
                            player.seekTo(finalPosition);
                            handler.post(updateProgress);
                            return true;
                    }
                }
                return false;
            }
        });
    }

    private void getTrackData() {
        trackList = MusicDataManager.getInstance().getTracks();
        currentPosition = MusicDataManager.getInstance().getCurrentPosition();

        if (trackList == null || trackList.isEmpty()) {
            Log.w(TAG, "Danh sách bài hát rỗng");
            Toast.makeText(this, "Không có bài hát để phát", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (currentPosition < 0 || currentPosition >= trackList.size()) {
            currentPosition = 0;
            MusicDataManager.getInstance().setCurrentPosition(currentPosition);
        }

        Log.d(TAG, "Đã tải " + trackList.size() + " bài hát, vị trí hiện tại: " + currentPosition);
    }

    private void setupClickListeners() {
        // Play/Pause
        btnPlayPause.setOnClickListener(v -> {
            togglePlayPause();
        });

        // Next
        btnNext.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playNext();
            }
        });

        // Previous
        btnPrevious.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playPrevious();
            }
        });

        // Shuffle
        btnShuffle.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.getPlaylistManager().toggleShuffle();
                boolean isShuffle = musicService.getPlaylistManager().isShuffle();
                updateShuffleButton(isShuffle);
                Toast.makeText(this, isShuffle ? "Bật phát ngẫu nhiên" : "Tắt phát ngẫu nhiên",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Repeat
        btnRepeat.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.getPlaylistManager().toggleRepeat();
                MusicService.PlaylistManager.RepeatMode repeatMode = musicService.getPlaylistManager().getRepeatMode();
                updateRepeatButton(repeatMode);

                String message = "";
                switch (repeatMode) {
                    case NONE:
                        message = "Tắt lặp lại";
                        break;
                    case ALL:
                        message = "Lặp lại tất cả";
                        break;
                    case ONE:
                        message = "Lặp lại một bài";
                        break;
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI() {
        Track currentTrack = bound && musicService != null ? musicService.getCurrentTrack() : null;

        if (currentTrack != null) {
            tvSongTitle.setText(currentTrack.getTitle() != null ? currentTrack.getTitle() : getString(R.string.default_song_title));
            tvArtistName.setText(currentTrack.getArtistName() != null ? currentTrack.getArtistName() : getString(R.string.default_artist_name));

            if (currentTrack.getArtworkUri() != null) {
                Glide.with(this).load(currentTrack.getArtworkUri())
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(ivAlbumArt);
            } else {
                ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            }

            // CHỈ CẬP NHẬT SHUFFLE VÀ REPEAT
            if (musicService != null) {
                updateShuffleButton(musicService.getPlaylistManager().isShuffle());
                updateRepeatButton(musicService.getPlaylistManager().getRepeatMode());
                // KHÔNG CẬP NHẬT PLAY/PAUSE Ở ĐÂY - ĐỂ OBSERVER XỬ LÝ
            }

            Log.d(TAG, "Cập nhật giao diện thành công: " + currentTrack.getTitle());
        } else {
            tvSongTitle.setText(R.string.default_song_title);
            tvArtistName.setText(R.string.default_artist_name);
            ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            Log.w(TAG, "Không có bài hát hiện tại");
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (btnPlayPause != null) {
            int resourceId = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
            btnPlayPause.setImageResource(resourceId);
            Log.d(TAG, "Play/Pause button updated: " + (isPlaying ? "PAUSE icon (Playing)" : "PLAY icon (Paused)"));
        } else {
            Log.e(TAG, "btnPlayPause is null!");
        }
    }
    private void togglePlayPause() {
        if (bound && musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.resume();
            }
        }
    }

    private void updateShuffleButton(boolean isShuffle) {
        if (btnShuffle != null) {
            if (isShuffle) {
                btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.primary_color));
                btnShuffle.setAlpha(1.0f);
            } else {
                btnShuffle.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_secondary));
                btnShuffle.setAlpha(0.7f);
            }
        }
    }

    private void updateRepeatButton(MusicService.PlaylistManager.RepeatMode repeatMode) {
        if (btnRepeat != null) {
            switch (repeatMode) {
                case NONE:
                    btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.icon_color_secondary));
                    btnRepeat.setAlpha(0.7f);
                    break;
                case ALL:
                case ONE:
                    btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.primary_color));
                    btnRepeat.setAlpha(1.0f);
                    break;
            }
        }
    }

    private void updateProgressView(float progressPercent) {
        if (seekBarLayout == null || seekBarProgress == null || seekThumb == null) return;

        int totalWidth = seekBarLayout.getWidth() - seekThumb.getWidth();
        if (totalWidth <= 0) return;

        float thumbLeftX = totalWidth * progressPercent;
        seekThumb.setTranslationX(thumbLeftX);

        if (seekThumbHitbox != null) {
            seekThumbHitbox.setTranslationX(thumbLeftX);
        }

        float progressWidth = thumbLeftX + (seekThumb.getWidth() / 2f);
        ViewGroup.LayoutParams params = seekBarProgress.getLayoutParams();
        params.width = (int) progressWidth;
        seekBarProgress.setLayoutParams(params);
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void showMenuOptions() {
        Log.d(TAG, "Menu clicked");
        Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Service đã được bind trong onCreate
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(updateProgress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // QUAN TRỌNG: REMOVE OBSERVER VÀ UNBIND SERVICE
        if (bound) {
            if (musicService != null && musicObserver != null) {
                musicService.removeObserver(musicObserver);
                Log.d(TAG, "Observer removed");
            }
            unbindService(connection);
            bound = false;
            Log.d(TAG, "Service unbound");
        }

        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Activity destroyed");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // CẬP NHẬT UI KHI ACTIVITY RESUME
        if (bound && musicService != null) {
            updateUI();

            // KIỂM TRA LẠI TRẠNG THÁI PLAYBACK
            handler.postDelayed(() -> {
                if (musicService != null) {
                    boolean isActuallyPlaying = musicService.isActuallyPlaying();
                    updatePlayPauseButton(isActuallyPlaying);
                    Log.d(TAG, "onResume playback state: " + (isActuallyPlaying ? "PLAYING" : "PAUSED"));
                }
            }, 300);
        }
    }
}