package com.example.musicbanger.service;

import androidx.media.session.MediaButtonReceiver;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.PlaybackException;

import com.example.musicbanger.NowPlayingActivity;
import com.example.musicbanger.R;
import com.example.musicbanger.model.Track;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private final IBinder binder = new LocalBinder();
    private ExoPlayer player;
    private PlaylistManager playlist = new PlaylistManager();
    private MediaSessionCompat mediaSession;

    public MusicService() {
        super();
    }

    private static final String CHANNEL_ID = "music_channel";
    private static final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "MusicService onCreate");
        try {
            initializePlayer();
            initializeMediaSession();
            Log.d(TAG, "MusicService initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in MusicService onCreate: " + e.getMessage(), e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "MusicService onStartCommand");
        return START_STICKY; // QUAN TRỌNG: Giữ service chạy
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
                    try {
                        updateMediaSessionPlaybackState();
                        updateNotification();
                        if (playbackState == Player.STATE_ENDED) {
                            playNext();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in playback state changed: " + e.getMessage(), e);
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d(TAG, "Is playing changed: " + isPlaying);
                    try {
                        updateMediaSessionPlaybackState();
                        updateNotification();
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

    private void initializeMediaSession() {
        try {
            Log.d(TAG, "Initializing MediaSession...");
            mediaSession = new MediaSessionCompat(this, "MusicService");
            mediaSession.setActive(true);
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

    @SuppressLint("ForegroundServiceType")
    private void updateNotification() {
        try {
            Track currentTrack = playlist.getCurrent();
            if (currentTrack == null) {
                Log.w(TAG, "No current track for notification");
                return;
            }

            createNotificationChannel();

            Intent intent = new Intent(this, NowPlayingActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Tạo action buttons
            NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                    player.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                    player.isPlaying() ? "Pause" : "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            player.isPlaying() ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY)
            );

            NotificationCompat.Action nextAction = new NotificationCompat.Action(
                    R.drawable.ic_skip_next,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
            );

            NotificationCompat.Action prevAction = new NotificationCompat.Action(
                    R.drawable.ic_skip_previous,
                    "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
            );

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(currentTrack.getTitle())
                    .setContentText(currentTrack.getArtistName())
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_music_note))
                    .setSmallIcon(R.drawable.ic_music_note)
                    .setContentIntent(pendingIntent)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2))
                    .addAction(prevAction)
                    .addAction(playPauseAction)
                    .addAction(nextAction)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOnlyAlertOnce(true)
                    .build();

            startForeground(NOTIFICATION_ID, notification);
            Log.d(TAG, "Notification updated");

        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage(), e);
        }
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Music Playback",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Notifications for music playback");
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: " + e.getMessage(), e);
        }
    }

    private void updateMediaSessionPlaybackState() {
        try {
            PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_STOP
                    );

            int state = PlaybackStateCompat.STATE_NONE;
            if (player != null) {
                if (player.isPlaying()) {
                    state = PlaybackStateCompat.STATE_PLAYING;
                } else if (player.getPlaybackState() == Player.STATE_READY) {
                    state = PlaybackStateCompat.STATE_PAUSED;
                } else if (player.getPlaybackState() == Player.STATE_BUFFERING) {
                    state = PlaybackStateCompat.STATE_BUFFERING;
                }
            }

            stateBuilder.setState(state, player != null ? player.getCurrentPosition() : 0, 1.0f);
            if (mediaSession != null) {
                mediaSession.setPlaybackState(stateBuilder.build());
            }

            // Update metadata
            Track current = playlist.getCurrent();
            if (current != null && mediaSession != null) {
                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, current.getTitle())
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, current.getArtistName())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, current.getAlbumName())
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, current.getDuration() * 1000L);
                mediaSession.setMetadata(metadataBuilder.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating media session: " + e.getMessage(), e);
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

    // Public methods for activity to call
    public void setPlaylistAndPlay(List<Track> tracks, int startIndex) {
        try {
            Log.d(TAG, "setPlaylistAndPlay - tracks: " + (tracks != null ? tracks.size() : 0) + ", startIndex: " + startIndex);
            playlist.setPlaylist(tracks, startIndex);
            Track current = playlist.getCurrent();
            if (current != null) {
                playTrack(current);
            } else {
                Log.e(TAG, "No current track after setting playlist");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setPlaylistAndPlay: " + e.getMessage(), e);
        }
    }

    public void playTrack(Track track) {
        try {
            if (track == null) {
                Log.e(TAG, "Track is null");
                return;
            }

            Log.d(TAG, "Playing track: " + track.getTitle());
            Log.d(TAG, "Stream URI: " + track.getStreamUri());

            if (track.getStreamUri() != null) {
                MediaItem mediaItem = MediaItem.fromUri(track.getStreamUri());
                if (player != null) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                    Log.d(TAG, "Track playback started");

                    updateMediaSessionPlaybackState();
                    updateNotification();
                } else {
                    Log.e(TAG, "Player is null");
                }
            } else {
                Log.e(TAG, "Stream URI is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error playing track: " + e.getMessage(), e);
        }
        notifyTrackChanged(track);
        notifyPlaybackStateChanged(true);
    }

    public void playNext() {
        try {
            Log.d(TAG, "playNext called");
            Track next = playlist.next();
            if (next != null) {
                playTrack(next);
            } else {
                Log.w(TAG, "No next track available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in playNext: " + e.getMessage(), e);
        }
        Track next = playlist.next();
        if (next != null) {
            notifyTrackChanged(next);
            notifyPlaybackStateChanged(true);
        }
    }

    public void playPrevious() {
        try {
            Log.d(TAG, "playPrevious called");
            Track prev = playlist.previous();
            if (prev != null) {
                playTrack(prev);
            } else {
                Log.w(TAG, "No previous track available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in playPrevious: " + e.getMessage(), e);
        }
    }

    public void pause() {
        try {
            Log.d(TAG, "pause called");
            if (player != null) {
                player.pause();
                updateMediaSessionPlaybackState();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in pause: " + e.getMessage(), e);
        }
        notifyPlaybackStateChanged(false);

    }

    public void resume() {
        try {
            Log.d(TAG, "resume called");
            if (player != null) {
                player.play();
                updateMediaSessionPlaybackState();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in resume: " + e.getMessage(), e);
        }
        notifyPlaybackStateChanged(true);
    }

    public void stop() {
        try {
            Log.d(TAG, "stop called");
            if (player != null) {
                player.stop();
                updateMediaSessionPlaybackState();
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
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in seekTo: " + e.getMessage(), e);
        }
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
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
            stopForeground(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        }
    }

    // PlaylistManager class
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
            if (playback.isEmpty()) return null;
            if (repeatMode == RepeatMode.ONE) return getCurrent();

            index++;
            if (index >= playback.size()) {
                if (repeatMode == RepeatMode.ALL) {
                    index = 0;
                } else {
                    index = playback.size() - 1;
                    return null;
                }
            }
            return getCurrent();
        }

        public Track previous() {
            if (playback.isEmpty()) return null;
            if (repeatMode == RepeatMode.ONE) return getCurrent();

            index--;
            if (index < 0) {
                if (repeatMode == RepeatMode.ALL) {
                    index = playback.size() - 1;
                } else {
                    index = 0;
                    return null;
                }
            }
            return getCurrent();
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
        for (MusicServiceObserver observer : observers) {
            observer.onPlaybackStateChanged(isPlaying);
        }
    }

    private void notifyTrackChanged(Track currentTrack) {
        for (MusicServiceObserver observer : observers) {
            observer.onTrackChanged(currentTrack);
        }
    }

    // Phương thức getPlayer() trả về ExoPlayer
    public ExoPlayer getPlayer() {
        return player;
    }
}