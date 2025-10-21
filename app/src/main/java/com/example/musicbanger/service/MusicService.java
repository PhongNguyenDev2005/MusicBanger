package com.example.musicbanger.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicbanger.NowPlayingActivity;
import com.example.musicbanger.R;
import com.example.musicbanger.manager.UserPlaylistManager;
import com.example.musicbanger.model.Playlist;
import com.example.musicbanger.model.Track;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.app.PendingIntent;

public class MusicService extends MediaBrowserServiceCompat {
    private static final String TAG = "MusicService";
    private final IBinder binder = new LocalBinder();
    private ExoPlayer player;
    private PlaylistManager playlist = new PlaylistManager();

    // Thêm các constant
    private static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1;
    private MediaSessionCompat mediaSession;

    public MusicService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService onCreate");
        try {
            initializePlayer();
            initializeMediaSession();
            initializeNotificationChannel();
            Log.d(TAG, "MusicService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in MusicService onCreate: " + e.getMessage(), e);
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        // Cho phép mọi app kết nối
        return new BrowserRoot("root", null);
    }

    // THÊM PHƯƠNG THỨC BẮT BUỘC CHO MediaBrowserServiceCompat
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        // Trả về danh sách rỗng vì chúng ta không cung cấp media browser
        result.sendResult(new ArrayList<>());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicService onStartCommand");

        // Xử lý Media Button từ outside
        if (intent != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }

        return START_STICKY;
    }

    private void initializePlayer() {
        try {
            Log.d(TAG, "Initializing ExoPlayer...");
            player = new ExoPlayer.Builder(this).build();
            Log.d(TAG, "ExoPlayer initialized successfully");

            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    Log.d(TAG, "Playback state changed: " + playbackState);

                    if (player == null) return;

                    updateMediaSession();
                    updateNotification();
                    boolean isPlaying = player.isPlaying();
                    notifyPlaybackStateChanged(isPlaying);

                    if (playbackState == Player.STATE_ENDED) {
                        saveToRecentlyPlayed();
                        handleTrackEnded();
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d(TAG, "Is playing changed: " + isPlaying);
                    try {
                        updateMediaSession();
                        updateNotification();
                        notifyPlaybackStateChanged(isPlaying);

                        // LƯU BÀI HÁT VÀO LỊCH SỬ KHI BẮT ĐẦU PHÁT
                        if (isPlaying) {
                            Track currentTrack = getCurrentTrack();
                            if (currentTrack != null) {
                                UserPlaylistManager.getInstance().addToRecentlyPlayed(currentTrack);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in isPlaying changed: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    Log.e(TAG, "Player error: " + error.getMessage(), error);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error initializing player: " + e.getMessage(), e);
        }
    }

    private void saveToRecentlyPlayed() {
        Track currentTrack = getCurrentTrack();
        if (currentTrack == null) {
            Log.w(TAG, "saveToRecentlyPlayed: no current track to save");
            return;
        }

        UserPlaylistManager.getInstance().addToRecentlyPlayed(currentTrack);
        Log.d(TAG, "Added to recently played: " + currentTrack.getTitle());
    }

    private void handleTrackEnded() {
            if (player == null || playlist == null) return;

            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            Track currentTrack = getCurrentTrack();

            Log.d(TAG, "Track ended. Position: " + currentPosition + " / " + duration);


            if (currentPosition < 3000) {
                Log.d(TAG, "Bài trc");
                Track prev = playlist.previous();
                if (prev != null) {
                    playTrack(prev);
                } else {
                    Log.d(TAG, "Không tìm đc bài trc");
                    notifyPlaybackStateChanged(false);
                }
                return;
            }

            if (currentTrack != null) {
                UserPlaylistManager.getInstance().addToRecentlyPlayed(currentTrack);
            }

            PlaylistManager.RepeatMode repeatMode = playlist.getRepeatMode();

            switch (repeatMode) {
                case ONE:
                    player.seekTo(0);
                    player.play();
                    break;

                case ALL:
                    Track nextTrack = playlist.next();
                    if (nextTrack != null) {
                        playTrack(nextTrack);
                    } else {
                        playlist.resetToStart();
                        playTrack(playlist.getCurrent());
                    }
                    break;

                case NONE:
                default:
                    Track next = playlist.next();
                    if (next != null) {
                        playTrack(next);
                    } else {
                        notifyPlaybackStateChanged(false);
                    }
                    break;
            }
    }

    private Handler handler = new Handler();

    private void initializeMediaSession() {
        try {
            Log.d(TAG, "Initializing MediaSession...");

            // Tạo MediaSession với token
            mediaSession = new MediaSessionCompat(this, "MusicBanger");

            // QUAN TRỌNG: Set session token cho MediaBrowserService
            setSessionToken(mediaSession.getSessionToken());

            mediaSession.setActive(true);

            // Set flags
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    Log.d(TAG, "MediaSession onPlay");
                    resume();
                }

                @Override
                public void onPause() {
                    Log.d(TAG, "MediaSession onPause");
                    pause();
                }

                @Override
                public void onSkipToNext() {
                    Log.d(TAG, "MediaSession onSkipToNext");
                    playNext();
                }

                @Override
                public void onSkipToPrevious() {
                    Log.d(TAG, "MediaSession onSkipToPrevious");
                    playPrevious();
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "MediaSession onStop");
                    stopSelf();
                }
            });

            Log.d(TAG, "MediaSession initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing MediaSession: " + e.getMessage(), e);
        }
    }

    // CHỈ GIỮ LẠI 1 PHƯƠNG THỨC UPDATE MEDIA SESSION
    private void updateMediaSession() {
        try {
            if (mediaSession == null) return;

            Track currentTrack = getCurrentTrack();

            // Update Metadata
            if (currentTrack != null) {
                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack.getTitle())
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentTrack.getArtistName())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentTrack.getAlbumName())
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, currentTrack.getDuration() * 1000L);

                mediaSession.setMetadata(metadataBuilder.build());
            }

            // Update Playback State
            int playbackState = PlaybackStateCompat.STATE_NONE;
            long position = 0;

            if (player != null) {
                position = player.getCurrentPosition();

                if (player.isPlaying()) {
                    playbackState = PlaybackStateCompat.STATE_PLAYING;
                } else if (player.getPlaybackState() == Player.STATE_READY) {
                    playbackState = PlaybackStateCompat.STATE_PAUSED;
                } else if (player.getPlaybackState() == Player.STATE_BUFFERING) {
                    playbackState = PlaybackStateCompat.STATE_BUFFERING;
                }
            }

            // Set actions available
            long actions = PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                    PlaybackStateCompat.ACTION_SEEK_TO;

            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(actions)
                    .setState(playbackState, position, 1.0f, System.currentTimeMillis());

            mediaSession.setPlaybackState(stateBuilder.build());

        } catch (Exception e) {
            Log.e(TAG, "Error updating MediaSession", e);
        }
    }

    // XÓA PHƯƠNG THỨC updateMediaSessionPlaybackState() VÌ TRÙNG LẶP

    private void initializeNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        try {
            Track currentTrack = getCurrentTrack();
            if (currentTrack == null) {
                Log.w(TAG, "No current track for notification");
                return createFallbackNotification();
            }

            Intent intent = new Intent(this, NowPlayingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Tạo notification builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentTitle(currentTrack.getTitle())
                    .setContentText(currentTrack.getArtistName())
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(isPlaying())
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true);

            // Thêm MediaStyle
            if (mediaSession != null) {
                builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));
            }

            // Thêm actions
            builder.addAction(R.drawable.ic_skip_previous, "Previous",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                    .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                            isPlaying() ? "Pause" : "Play",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    isPlaying() ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY))
                    .addAction(R.drawable.ic_skip_next, "Next",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

            // Load ảnh album bất đồng bộ
            loadAlbumArtForNotification(builder, currentTrack);

            return builder.build();

        } catch (Exception e) {
            Log.e(TAG, "Error creating notification", e);
            return createFallbackNotification();
        }
    }

    private void loadAlbumArtForNotification(NotificationCompat.Builder builder, Track track) {
        try {
            if (track.getArtworkUri() != null) {
                // Sử dụng Glide để load ảnh
                Glide.with(this)
                        .asBitmap()
                        .load(track.getArtworkUri())
                        .into(new CustomTarget<Bitmap>(256, 256) { // Kích thước tối ưu cho notification
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource,
                                                        @Nullable Transition<? super Bitmap> transition) {
                                try {
                                    // Set ảnh khi load thành công
                                    builder.setLargeIcon(resource);
                                    updateNotificationWithBitmap(resource);
                                    Log.d(TAG, "Album art loaded successfully for notification");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error setting album art", e);
                                    setFallbackIcon(builder);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                // Dùng ảnh fallback nếu load thất bại
                                setFallbackIcon(builder);
                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                Log.w(TAG, "Failed to load album art, using fallback");
                                setFallbackIcon(builder);
                            }
                        });
            } else {
                // Không có artwork URI, dùng fallback
                setFallbackIcon(builder);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading album art for notification", e);
            setFallbackIcon(builder);
        }
    }

    private void updateNotificationWithBitmap(Bitmap bitmap) {
        try {
            // Cập nhật notification với ảnh mới
            Notification notification = createNotificationWithBitmap(bitmap);
            if (notification != null) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification with bitmap", e);
        }
    }

    private Notification createNotificationWithBitmap(Bitmap largeIcon) {
        try {
            Track currentTrack = getCurrentTrack();
            if (currentTrack == null) return null;

            Intent intent = new Intent(this, NowPlayingActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setLargeIcon(largeIcon) // Set ảnh đã load
                    .setContentTitle(currentTrack.getTitle())
                    .setContentText(currentTrack.getArtistName())
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(isPlaying())
                    .setShowWhen(false)
                    .setOnlyAlertOnce(true);

            if (mediaSession != null) {
                builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));
            }

            builder.addAction(R.drawable.ic_skip_previous, "Previous",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                    .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                            isPlaying() ? "Pause" : "Play",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    isPlaying() ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY))
                    .addAction(R.drawable.ic_skip_next, "Next",
                            MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification with bitmap", e);
            return null;
        }
    }

    private void setFallbackIcon(NotificationCompat.Builder builder) {
        try {
            Bitmap fallbackIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_music_note);
            builder.setLargeIcon(fallbackIcon);
        } catch (Exception e) {
            Log.e(TAG, "Error setting fallback icon", e);
        }
    }

    private Notification createFallbackNotification() {
        // Notification đơn giản không có ảnh album
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle("MusicBanger")
                .setContentText("Đang phát nhạc")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying())
                .setShowWhen(false)
                .setOnlyAlertOnce(true);

        if (mediaSession != null) {
            builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2));
        }

        builder.addAction(R.drawable.ic_skip_previous, "Previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
                .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying() ? "Pause" : "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                isPlaying() ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY))
                .addAction(R.drawable.ic_skip_next, "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        return builder.build();
    }

    private void updateNotification() {
        try {
            Notification notification = createNotification();
            if (notification != null) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startMusicForeground() {
        try {
            Notification notification = createNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Started music foreground service");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
    }

    private void stopMusicForeground() {
        try {
            stopForeground(true);
            stopSelf();
            Log.d(TAG, "Stopped music foreground service");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground service", e);
        }
    }

    public class LocalBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Public methods
    public void setPlaylistAndPlay(List<Track> tracks, int startIndex) {
        try {
            Log.d(TAG, "setPlaylistAndPlay - tracks: " + (tracks != null ? tracks.size() : 0) + ", startIndex: " + startIndex);
            playlist.setPlaylist(tracks, startIndex);
            Track current = playlist.getCurrent();
            if (current != null) {
                playTrack(current);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setPlaylistAndPlay: " + e.getMessage(), e);
        }
    }

    public void playTrack(Track track) {
        try {
            if (track == null) return;

            Log.d(TAG, "Playing track: " + track.getTitle());

            if (track.getStreamUri() != null) {
                MediaItem mediaItem = MediaItem.fromUri(track.getStreamUri());
                if (player != null) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                    Log.d(TAG, "Track playback started");

                    updateMediaSession();
                    startMusicForeground();

                    // QUAN TRỌNG: THÔNG BÁO TRACK CHANGED VÀ PLAYBACK STATE CHANGED
                    notifyTrackChanged(track);

                    // THÔNG BÁO PLAYBACK STATE SAU KHI PLAYER ĐÃ SẴN SÀNG
                    handler.postDelayed(() -> {
                        notifyPlaybackStateChanged(true);
                    }, 100);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing track: " + e.getMessage(), e);
        }
    }

    public void playNext() {
        try {
            Log.d(TAG, "playNext called");
            Track next = playlist.next();
            if (next != null) {
                playTrack(next); // playTrack sẽ tự động gọi notifyTrackChanged
            } else {
                // Nếu không có bài tiếp theo, vẫn thông báo playback state
                notifyPlaybackStateChanged(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in playNext: " + e.getMessage(), e);
        }
    }

    public void playPrevious() {
        //VINH: t thế cái try catch thành if cho nó performance hơn
        if (playlist == null){
            Log.w(TAG, "Playlist is null");
            notifyPlaybackStateChanged(false);
            return;
        }

        Track prev = playlist.previous();
        if (prev == null){
            Log.w(TAG, "no previous track available");
            notifyPlaybackStateChanged(false);
            return;
        }
        playTrack(prev);
    }

    public void pause() {
        try {
            Log.d(TAG, "pause called");
            if (player != null) {
                player.pause();
                updateMediaSession();
                updateNotification();
                notifyPlaybackStateChanged(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in pause: " + e.getMessage(), e);
        }
    }

    public void resume() {
        try {
            Log.d(TAG, "resume called");
            if (player != null) {
                player.play();
                updateMediaSession();
                startMusicForeground();
                notifyPlaybackStateChanged(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in resume: " + e.getMessage(), e);
        }
    }

    public void stop() {
        try {
            Log.d(TAG, "stop called");
            if (player != null) {
                player.stop();
                updateMediaSession();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in stop: " + e.getMessage(), e);
        }
    }

    public void seekTo(long position) {
        try {
            if (player != null) {
                player.seekTo(position);
                updateMediaSession();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in seekTo: " + e.getMessage(), e);
        }
    }

    public boolean isPlaying() {
        if (player != null) {
            boolean playing = player.isPlaying();
            Log.d(TAG, "isPlaying() called - returning: " + playing);
            return playing;
        }
        Log.d(TAG, "isPlaying() called - player is null, returning false");
        return false;
    }

    public boolean isActuallyPlaying() {
        if (player != null) {
            int playbackState = player.getPlaybackState();
            boolean isPlaying = player.isPlaying();

            Log.d(TAG, "Player state - PlaybackState: " + playbackState +
                    ", isPlaying: " + isPlaying +
                    ", Buffering: " + (playbackState == Player.STATE_BUFFERING) +
                    ", Ready: " + (playbackState == Player.STATE_READY));

            // Trả về true nếu player đang ở trạng thái READY và isPlaying là true
            return playbackState == Player.STATE_READY && isPlaying;
        }
        return false;
    }

    public Track getCurrentTrack() {
        return playlist.getCurrent();
    }

    public PlaylistManager getPlaylistManager() {
        return playlist;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "MusicService onDestroy");
        try {
            if (player != null) {
                player.release();
                player = null;
            }
            if (mediaSession != null) {
                mediaSession.setActive(false);
                mediaSession.release();
            }
            stopMusicForeground();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }

        // Lưu track hiện tại vào recently played - TRUYỀN CONTEXT VÀO ĐÂY
        Track currentTrack = getCurrentTrack();
        if (currentTrack != null) {
            UserPlaylistManager.getInstance().addToRecentlyPlayed(currentTrack);
        }
    }

    // PlaylistManager class (giữ nguyên)
    public class PlaylistManager {

        public enum RepeatMode { NONE, ONE, ALL }
        private final List<Track> original = new ArrayList<>();
        private final List<Track> playback = new ArrayList<>();
        private int index = 0;
        private boolean shuffle = false;
        private RepeatMode repeatMode = RepeatMode.NONE;

        public void setPlaylist(List<Track> tracks, int startIndex) {
            try {
                original.clear();
                if (tracks != null) {
                    original.addAll(tracks);
                }
                rebuild();
                index = Math.max(0, Math.min(startIndex, playback.size() - 1));
                Log.d(TAG, "Playlist set - size: " + playback.size() + ", index: " + index);
            } catch (Exception e) {
                Log.e(TAG, "Error setting playlist: " + e.getMessage(), e);
            }
        }

        private void resetToStart() {
            if (playback == null || playback.isEmpty()){
                Log.d(TAG, "fail resetToStart: playlist trống");
                return;
            }
            index = 0;
        }

        private void rebuild() {
            playback.clear();
            playback.addAll(original);
            if (shuffle) {
                Collections.shuffle(playback, new Random(System.currentTimeMillis()));
            }
        }

        public Track getCurrent() {
            if (playback.isEmpty() || index < 0 || index >= playback.size()) {
                return null;
            }
            return playback.get(index);
        }

        public Track next() {
            if (playback.isEmpty()) {
                Log.d(TAG, "Playback list is empty");
                return null;
            }

            // KIỂM TRA NẾU ĐANG Ở CUỐI DANH SÁCH
            if (index >= playback.size() - 1) {
                Log.d(TAG, "Reached end of playlist");
                if (repeatMode == RepeatMode.ALL) {
                    index = 0; // QUAY VỀ ĐẦU PLAYLIST
                    Log.d(TAG, "Repeat all - looping to start");
                } else {
                    // KHÔNG PHÁT TIẾP NGOÀI PLAYLIST HIỆN TẠI
                    Log.d(TAG, "No repeat - stopping at end of playlist");
                    return null;
                }
            } else {
                index++; // CHUYỂN ĐẾN BÀI TIẾP THEO
            }

            Track nextTrack = getCurrent();
            Log.d(TAG, "Next track: " + (nextTrack != null ? nextTrack.getTitle() : "null"));
            return nextTrack;
        }

        public Track previous() {
            if (playback.isEmpty()) {
                Log.d(TAG, "Playback list is empty");
                return null;
            }

            // KIỂM TRA NẾU ĐANG Ở ĐẦU DANH SÁCH
            if (index <= 0) {
                Log.d(TAG, "Reached start of playlist");
                if (repeatMode == RepeatMode.ALL) {
                    index = playback.size() - 1; // QUAY VỀ CUỐI PLAYLIST
                    Log.d(TAG, "Repeat all - looping to end");
                } else {
                    // KHÔNG PHÁT LÙI NGOÀI PLAYLIST HIỆN TẠI
                    Log.d(TAG, "No repeat - stopping at start of playlist");
                    return null;
                }
            } else {
                index--; // CHUYỂN ĐẾN BÀI TRƯỚC
            }

            Track prevTrack = getCurrent();
            Log.d(TAG, "Previous track: " + (prevTrack != null ? prevTrack.getTitle() : "null"));
            return prevTrack;
        }

        public void toggleShuffle() {
            shuffle = !shuffle;
            Track current = getCurrent();
            rebuild();
            if (current != null) {
                index = playback.indexOf(current);
                if (index == -1) index = 0;
            }
        }

        public void toggleRepeat() {
            switch (repeatMode) {
                case NONE:
                    repeatMode = RepeatMode.ALL;
                    break;
                case ALL:
                    repeatMode = RepeatMode.ONE;
                    break;
                case ONE:
                    repeatMode = RepeatMode.NONE;
                    break;
            }
        }

        public boolean isShuffle() { return shuffle; }
        public RepeatMode getRepeatMode() { return repeatMode; }
        public void setRepeatMode(RepeatMode repeatMode) { this.repeatMode = repeatMode; }
        public int getCurrentIndex() { return index; }
        public List<Track> getPlaylist() { return new ArrayList<>(playback); }
    }

    // Observer pattern (giữ nguyên)
    public interface MusicServiceObserver {
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackChanged(Track currentTrack);
    }

    private List<MusicServiceObserver> observers = new ArrayList<>();

    public void addObserver(MusicServiceObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(MusicServiceObserver observer) {
        observers.remove(observer);
    }

    private void notifyPlaybackStateChanged(boolean isPlaying) {
        Log.d(TAG, "Notifying playback state: " + (isPlaying ? "PLAYING" : "PAUSED") +
                " to " + observers.size() + " observers");

        // TẠO DANH SÁCH COPY ĐỂ TRÁNH CONCURRENT MODIFICATION
        List<MusicServiceObserver> observersCopy = new ArrayList<>(observers);

        for (MusicServiceObserver observer : observersCopy) {
            try {
                observer.onPlaybackStateChanged(isPlaying);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying observer: " + e.getMessage(), e);
            }
        }
    }

    private void notifyTrackChanged(Track currentTrack) {
        Log.d(TAG, "Notifying track changed: " +
                (currentTrack != null ? currentTrack.getTitle() : "null") +
                " to " + observers.size() + " observers");

        for (MusicServiceObserver observer : observers) {
            try {
                observer.onTrackChanged(currentTrack);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying observer: " + e.getMessage(), e);
            }
        }
    }

    public ExoPlayer getPlayer() {
        return player;
    }

    // Thêm phương thức play user playlist
    public void playUserPlaylist(Playlist playlist, int startIndex) {
        try {
            if (playlist != null && !playlist.getTracks().isEmpty()) {
                UserPlaylistManager.getInstance().setCurrentPlayingPlaylist(playlist);
                setPlaylistAndPlay(playlist.getTracks(), startIndex);
                Log.d(TAG, "Playing user playlist: " + playlist.getName());

                // Auto-save to recently played
                UserPlaylistManager.getInstance().addToRecentlyPlayed(playlist.getTracks().get(startIndex));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing user playlist", e);
        }
    }

    // Thêm phương thức cho favorites
    public void toggleFavorite(Track track) {
        UserPlaylistManager.getInstance().toggleFavorite(track);
    }

    public boolean isFavorite(Track track) {
        return UserPlaylistManager.getInstance().isFavorite(track);
    }

    // THÊM PHƯƠNG THỨC PHÁT NGẪU NHIÊN TRONG PLAYLIST
    public void shufflePlayPlaylist(Playlist playlist) {
        try {
            if (playlist != null && !playlist.getTracks().isEmpty()) {
                UserPlaylistManager.getInstance().setCurrentPlayingPlaylist(playlist);

                // Tạo danh sách ngẫu nhiên
                List<Track> shuffledTracks = new ArrayList<>(playlist.getTracks());
                Collections.shuffle(shuffledTracks);

                // Phát từ đầu danh sách ngẫu nhiên
                setPlaylistAndPlay(shuffledTracks, 0);

                Log.d(TAG, "Shuffle playing playlist: " + playlist.getName() + " (" + shuffledTracks.size() + " tracks)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in shufflePlayPlaylist", e);
        }
    }

    // THÊM PHƯƠNG THỨC PHÁT NGẪU NHIÊN TRONG DANH SÁCH BÀI HÁT
    public void shufflePlayTracks(List<Track> tracks) {
        try {
            if (tracks != null && !tracks.isEmpty()) {
                // Tạo danh sách ngẫu nhiên
                List<Track> shuffledTracks = new ArrayList<>(tracks);
                Collections.shuffle(shuffledTracks);

                // Phát từ đầu danh sách ngẫu nhiên
                setPlaylistAndPlay(shuffledTracks, 0);

                Log.d(TAG, "Shuffle playing tracks: " + shuffledTracks.size() + " tracks");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in shufflePlayTracks", e);
        }
    }

    // THÊM PHƯƠNG THỨC RESET PLAYLIST
    public void resetAndPlayTrack(Track track) {
        try {
            Log.d(TAG, "Resetting playlist and playing single track: " + track.getTitle());

            // Tạo playlist mới chỉ với track này
            List<Track> singleTrackList = new ArrayList<>();
            singleTrackList.add(track);

            // Set playlist mới và phát
            setPlaylistAndPlay(singleTrackList, 0);

            // Reset current playing playlist
            UserPlaylistManager.getInstance().setCurrentPlayingPlaylist(null);

        } catch (Exception e) {
            Log.e(TAG, "Error in resetAndPlayTrack: " + e.getMessage(), e);
        }
    }

    // THÊM PHƯƠNG THỨC RESET VÀ PLAY DANH SÁCH MỚI
    public void resetAndPlayTracks(List<Track> tracks, int startIndex) {
        try {
            Log.d(TAG, "Resetting and playing new track list: " + tracks.size() + " tracks");

            // Set playlist mới
            setPlaylistAndPlay(tracks, startIndex);

            // Reset current playing playlist
            UserPlaylistManager.getInstance().setCurrentPlayingPlaylist(null);

        } catch (Exception e) {
            Log.e(TAG, "Error in resetAndPlayTracks: " + e.getMessage(), e);
        }
    }

    public int getPlaybackState() {
        if (player != null) {
            return player.getPlaybackState();
        }
        return Player.STATE_IDLE;
    }

}