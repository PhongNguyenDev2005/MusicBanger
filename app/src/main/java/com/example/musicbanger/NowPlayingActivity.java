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
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

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
    private View seekBarBackground, seekBarProgress, seekThumb, seekThumbHitbox ;

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
            handler.postDelayed(this, 50); // Update every 50ms for ultra-smoothness
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicService.LocalBinder b = (MusicService.LocalBinder) binder;
            musicService = b.getService();
            bound = true;

            // Add Player Listener here for sync
            musicService.getPlayer().addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePlayPauseButton(isPlaying);
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_ENDED) {
                        playNextTrack();
                    }
                }
            });

            initializePlayer();
            updateUI();
            Log.d(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
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
        setupClickListeners();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

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

                            // Di chuyển thumb và hitbox cùng nhau
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

            if (ivAlbumArt == null || btnBack == null || btnMenu == null || tvSongTitle == null || tvArtistName == null ||
                    tvCurrentTime == null || tvTotalTime == null || btnNext == null || btnPrevious == null || btnShuffle == null ||
                    btnRepeat == null || seekBarLayout == null || seekBarBackground == null || seekBarProgress == null ||
                    seekThumb == null || btnPlayPause == null) {
                Log.e(TAG, "Missing view(s) in layout - check activity_now_playing.xml");
                Toast.makeText(this, "Lỗi: Thiếu thành phần giao diện", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Set up drag for seekThumb

            seekThumb.setOnTouchListener(new View.OnTouchListener() {
                private float initialX;                private float initialThumbX;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (bound && musicService != null && musicService.getPlayer() != null) {
                        Player player = musicService.getPlayer();
                        float width = seekBarLayout.getWidth() - seekThumb.getWidth();
                        if (width <= 0) return false;

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                handler.removeCallbacks(updateProgress);
                                initialX = event.getRawX(); // Sử dụng getRawX() để có tọa độ tuyệt đối trên màn hình
                                initialThumbX = seekThumb.getX();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                float dx = event.getRawX() - initialX;
                                float newX = initialThumbX + dx;
                                newX = Math.max(0, Math.min(newX, width));

                                // Cập nhật vị trí của seekThumb
                                seekThumb.setX(newX);

                                float progressPercent = newX / width;
                                // Cập nhật chiều rộng của seekBarProgress để nó đi theo seekThumb
                                updateProgressView(progressPercent);

                                long newPosition = (long) (progressPercent * player.getDuration());
                                tvCurrentTime.setText(formatTime((int) (newPosition / 1000)));
                                break;
                            case MotionEvent.ACTION_UP:
                                float finalX = seekThumb.getX();
                                float finalProgressPercent = finalX / width;
                                long finalPosition = (long) (finalProgressPercent * player.getDuration());
                                player.seekTo(finalPosition);
                                handler.post(updateProgress);
                                Log.d(TAG, "Seeked to: " + finalPosition + "ms");
                                break;
                        }
                        return true;
                    }
                    return false;
                }
            });


            Log.d(TAG, "Khởi tạo giao diện thành công");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi khởi tạo giao diện: " + e.getMessage(), e);
            Toast.makeText(this, "Lỗi khởi tạo giao diện", Toast.LENGTH_SHORT).show();
        }
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
        updateUI();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnNext.setOnClickListener(v -> {
            if (bound && musicService != null) {
                musicService.playNext();
                updateUI();
            } else {
                playNextTrack();
            }
        });
        btnPrevious.setOnClickListener(v -> {
            if (bound && musicService != null) {
                musicService.playPrevious();
                updateUI();
            } else {
                playPreviousTrack();
            }
        });
        btnShuffle.setOnClickListener(v -> {
            if (bound && musicService != null) {
                musicService.getPlaylistManager().toggleShuffle();
                updateShuffleState();
            }
        });
        btnRepeat.setOnClickListener(v -> {
            if (bound && musicService != null) {
                musicService.getPlaylistManager().toggleRepeat();
                updateRepeatState();
            }
        });
        btnMenu.setOnClickListener(v -> showMenuOptions());
    }

    private void initializePlayer() {
        if (bound && musicService != null && trackList != null && !trackList.isEmpty()) {
            musicService.setPlaylistAndPlay(trackList, currentPosition);
            Track currentTrack = trackList.get(currentPosition);
            loadAndPlayTrack(currentTrack);
            handler.post(updateProgress);
            Log.d(TAG, "Khởi tạo trình phát thành công");
        } else {
            Log.w(TAG, "Không thể khởi tạo trình phát: dịch vụ hoặc danh sách bài hát không sẵn sàng");
        }
    }

    private void updateUI() {
        Track currentTrack = bound && musicService != null ? musicService.getCurrentTrack() :
                (trackList != null && currentPosition < trackList.size() ? trackList.get(currentPosition) : null);

        if (currentTrack != null && tvSongTitle != null && tvArtistName != null) {
            tvSongTitle.setText(currentTrack.getTitle() != null ? currentTrack.getTitle() : getString(R.string.default_song_title));
            tvArtistName.setText(currentTrack.getArtistName() != null ? currentTrack.getArtistName() : getString(R.string.default_artist_name));

            if (ivAlbumArt != null) {
                if (currentTrack.getArtworkUri() != null) {
                    Glide.with(this).load(currentTrack.getArtworkUri())
                            .placeholder(R.drawable.ic_music_note)
                            .error(R.drawable.ic_music_note)
                            .into(ivAlbumArt);
                } else {
                    ivAlbumArt.setImageResource(R.drawable.ic_music_note);
                }
            }

            if (tvCurrentTime != null) tvCurrentTime.setText("0:00");
            if (tvTotalTime != null) tvTotalTime.setText("0:00");

            boolean isPlaying = bound && musicService != null && musicService.getPlayer() != null && musicService.getPlayer().isPlaying();
            updatePlayPauseButton(isPlaying);
            updateShuffleState();
            updateRepeatState();
            Log.d(TAG, "Cập nhật giao diện thành công: " + currentTrack.getTitle());
        } else {
            if (tvSongTitle != null) tvSongTitle.setText(R.string.default_song_title);
            if (tvArtistName != null) tvArtistName.setText(R.string.default_artist_name);
            if (ivAlbumArt != null) ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            if (tvCurrentTime != null) tvCurrentTime.setText("0:00");
            if (tvTotalTime != null) tvTotalTime.setText("0:00");
            Log.w(TAG, "Không có bài hát hiện tại hoặc view null");
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (btnPlayPause != null) {
            btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        }
    }

    private void togglePlayPause() {
        if (bound && musicService != null && musicService.getPlayer() != null) {
            if (musicService.getPlayer().isPlaying()) {
                musicService.pause();
                updatePlayPauseButton(false);
            } else {
                musicService.resume();
                updatePlayPauseButton(true);
            }
            handler.post(updateProgress);
        } else {
            Log.w(TAG, "Không thể chuyển đổi play/pause: dịch vụ hoặc trình phát không sẵn sàng");
            Toast.makeText(this, "Không thể phát nhạc", Toast.LENGTH_SHORT).show();
        }
    }

    private void playNextTrack() {
        if (trackList != null && !trackList.isEmpty()) {
            currentPosition = (currentPosition + 1) % trackList.size();
            MusicDataManager.getInstance().setCurrentPosition(currentPosition);
            loadAndPlayTrack(trackList.get(currentPosition));
            updateUI();
        }
    }

    private void playPreviousTrack() {
        if (trackList != null && !trackList.isEmpty()) {
            currentPosition = (currentPosition - 1 + trackList.size()) % trackList.size();
            MusicDataManager.getInstance().setCurrentPosition(currentPosition);
            loadAndPlayTrack(trackList.get(currentPosition));
            updateUI();
        }
    }

    private void updateShuffleState() {
        if (btnShuffle != null && bound && musicService != null) {
            boolean isShuffle = musicService.getPlaylistManager().isShuffle();
            btnShuffle.setColorFilter(isShuffle ?
                    getResources().getColor(R.color.primary_color, getTheme()) :
                    getResources().getColor(R.color.icon_color_secondary, getTheme()));
        }
    }

    private void updateRepeatState() {
        if (btnRepeat != null && bound && musicService != null) {
            int color = getResources().getColor(R.color.icon_color_secondary, getTheme());
            MusicService.PlaylistManager.RepeatMode repeatMode = musicService.getPlaylistManager().getRepeatMode();
            if (repeatMode == MusicService.PlaylistManager.RepeatMode.ONE ||
                    repeatMode == MusicService.PlaylistManager.RepeatMode.ALL) {
                color = getResources().getColor(R.color.primary_color, getTheme());
            }
            btnRepeat.setColorFilter(color);
        }
    }

    private void loadAndPlayTrack(Track track) {
        if (track == null || (track.getStreamUri() == null && track.getStreamUrl() == null)) {
            Log.e(TAG, "Bài hát hoặc URL âm thanh không hợp lệ: " + (track != null ? track.getTitle() : "null"));
            Toast.makeText(this, "Không thể phát bài hát này", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bound && musicService != null) {
            musicService.playTrack(track);
            handler.post(updateProgress);
            Log.d(TAG, "Đang phát bài hát: " + track.getTitle());
        } else {
            Log.w(TAG, "Dịch vụ không khả dụng, không thể phát bài hát");
            Toast.makeText(this, "Không thể phát nhạc", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgressView(float progressPercent) {
        if (seekBarLayout == null || seekBarProgress == null || seekThumb == null) return;

        // Tổng khoảng thumb có thể di chuyển
        int totalWidth = seekBarLayout.getWidth() - seekThumb.getWidth();
        if (totalWidth <= 0) return;

        // Vị trí cạnh trái của thumb
        float thumbLeftX = totalWidth * progressPercent;

        // Cập nhật vị trí thumb (dịch chuyển theo progress)
        seekThumb.setTranslationX(thumbLeftX);
        View seekThumbHitbox = findViewById(R.id.seekThumbHitbox);
        if (seekThumbHitbox != null) {
            seekThumbHitbox.setTranslationX(thumbLeftX);
        }

        // Tính chiều rộng progress kết thúc tại TÂM thumb
        float progressWidth = thumbLeftX + (seekThumb.getWidth() / 2f);

        // Cập nhật LayoutParams cho progress
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
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE); // Sử dụng ServiceConnection
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
            Log.d(TAG, "Dịch vụ đã được ngắt kết nối");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Activity đã bị hủy");
    }


}