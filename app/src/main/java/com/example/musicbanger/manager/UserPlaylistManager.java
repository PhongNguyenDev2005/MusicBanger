package com.example.musicbanger.manager;

import android.content.Context;
import com.example.musicbanger.database.MusicDatabase;
import com.example.musicbanger.database.dao.FavoriteDao;
import com.example.musicbanger.database.dao.PlaylistDao;
import com.example.musicbanger.database.entities.FavoriteTrackEntity;
import com.example.musicbanger.database.entities.PlaylistEntity;
import com.example.musicbanger.model.Playlist;
import com.example.musicbanger.model.Track;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserPlaylistManager {
    private static UserPlaylistManager instance;
    private PlaylistDao playlistDao;
    private FavoriteDao favoriteDao;
    private Gson gson;
    private Playlist currentPlayingPlaylist;
    private ExecutorService executor; // THÊM EXECUTOR ĐỂ CHẠY BACKGROUND TASKS

    private UserPlaylistManager(Context context) {
        Context appContext = context.getApplicationContext();
        MusicDatabase database = MusicDatabase.getInstance(appContext);
        this.playlistDao = database.playlistDao();
        this.favoriteDao = database.favoriteDao();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor(); // KHỞI TẠO EXECUTOR
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new UserPlaylistManager(context);
        }
    }

    public static UserPlaylistManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserPlaylistManager must be initialized first. Call initialize() in your Application class.");
        }
        return instance;
    }

    // ========== PLAYLIST OPERATIONS ==========

    public void createPlaylist(String name, String description) {
        executor.execute(() -> {
            String playlistId = UUID.randomUUID().toString();
            PlaylistEntity entity = new PlaylistEntity(playlistId, name, description, false);
            playlistDao.insertPlaylist(entity);
        });
    }

    public void deletePlaylist(String playlistId) {
        executor.execute(() -> {
            PlaylistEntity playlist = playlistDao.getPlaylistById(playlistId);
            if (playlist != null && !playlist.isDefault) {
                playlistDao.deletePlaylist(playlist);
            }
        });
    }

    public void renamePlaylist(String playlistId, String newName) {
        executor.execute(() -> {
            PlaylistEntity playlist = playlistDao.getPlaylistById(playlistId);
            if (playlist != null && !playlist.isDefault) {
                playlist.name = newName;
                playlistDao.updatePlaylist(playlist);
            }
        });
    }

    public void addTrackToPlaylist(String playlistId, Track track) {
        executor.execute(() -> {
            try {
                PlaylistEntity playlist = playlistDao.getPlaylistById(playlistId);
                if (playlist != null) {
                    android.util.Log.d("UserPlaylistManager", "Adding track to playlist: " + playlist.name);

                    List<Track> tracks = com.example.musicbanger.database.converters.TrackListConverter.toTrackList(playlist.tracksJson);
                    android.util.Log.d("UserPlaylistManager", "Current tracks: " + tracks.size());

                    boolean exists = false;
                    for (Track existingTrack : tracks) {
                        if (existingTrack.getId().equals(track.getId())) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        tracks.add(track);
                        playlist.tracksJson = com.example.musicbanger.database.converters.TrackListConverter.fromTrackList(tracks);
                        playlistDao.updatePlaylist(playlist);
                        android.util.Log.d("UserPlaylistManager", "Track added successfully. New count: " + tracks.size());
                    } else {
                        android.util.Log.d("UserPlaylistManager", "Track already exists in playlist");
                    }
                } else {
                    android.util.Log.e("UserPlaylistManager", "Playlist not found: " + playlistId);
                }
            } catch (Exception e) {
                android.util.Log.e("UserPlaylistManager", "Error adding track to playlist: " + e.getMessage());
            }
        });
    }
    public void removeTrackFromPlaylist(String playlistId, Track track) {
        executor.execute(() -> {
            PlaylistEntity playlist = playlistDao.getPlaylistById(playlistId);
            if (playlist != null) {
                List<Track> tracks = com.example.musicbanger.database.converters.TrackListConverter.toTrackList(playlist.tracksJson);

                for (int i = 0; i < tracks.size(); i++) {
                    if (tracks.get(i).getId().equals(track.getId())) {
                        tracks.remove(i);
                        break;
                    }
                }

                playlist.tracksJson = com.example.musicbanger.database.converters.TrackListConverter.fromTrackList(tracks);
                playlistDao.updatePlaylist(playlist);
            }
        });
    }

    // ========== GETTER METHODS - CẦN XỬ LÝ BẤT ĐỒNG BỘ ==========

    public List<Playlist> getAllPlaylists() {
        try {
            List<PlaylistEntity> entities = playlistDao.getAllPlaylistsSync();
            List<Playlist> playlists = new ArrayList<>();

            if (entities != null) {
                for (PlaylistEntity entity : entities) {
                    Playlist playlist = convertToPlaylist(entity);
                    playlists.add(playlist);
                }
            }

            // DEBUG: Log số lượng playlist
            android.util.Log.d("UserPlaylistManager", "Found " + playlists.size() + " playlists");
            for (Playlist p : playlists) {
                android.util.Log.d("UserPlaylistManager", "Playlist: " + p.getName() + " (" + p.getTrackCount() + " tracks)");
            }

            return playlists;
        } catch (Exception e) {
            android.util.Log.e("UserPlaylistManager", "Error getting playlists: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Playlist getPlaylistById(String playlistId) {
        PlaylistEntity entity = playlistDao.getPlaylistById(playlistId);
        return entity != null ? convertToPlaylist(entity) : null;
    }

    public Playlist getFavoritesPlaylist() {
        PlaylistEntity entity = playlistDao.getPlaylistByName("Bài hát yêu thích");
        return entity != null ? convertToPlaylist(entity) : null;
    }

    public Playlist getRecentlyPlayedPlaylist() {
        PlaylistEntity entity = playlistDao.getPlaylistByName("Nghe gần đây");
        return entity != null ? convertToPlaylist(entity) : null;
    }

    // ========== FAVORITE OPERATIONS ==========

    public void toggleFavorite(Track track) {
        android.util.Log.d("UserPlaylistManager", "=== TOGGLE FAVORITE STARTED ===");
        android.util.Log.d("UserPlaylistManager", "Track: " + track.getTitle() + " (ID: " + track.getId() + ")");

        executor.execute(() -> {
            try {
                android.util.Log.d("UserPlaylistManager", "Checking favorite status...");
                boolean isFavorite = favoriteDao.isTrackFavorite(track.getId()) > 0;
                android.util.Log.d("UserPlaylistManager", "Current favorite status: " + isFavorite);

                if (isFavorite) {
                    android.util.Log.d("UserPlaylistManager", "Removing from favorites...");
                    favoriteDao.removeFavoriteByTrackId(track.getId());
                    android.util.Log.d("UserPlaylistManager", "Removed from favorites table");

                    // Also remove from favorites playlist
                    removeTrackFromPlaylist("favorites_default", track);
                    android.util.Log.d("UserPlaylistManager", "Removed from favorites playlist");
                } else {
                    android.util.Log.d("UserPlaylistManager", "Adding to favorites...");
                    String trackJson = gson.toJson(track);
                    android.util.Log.d("UserPlaylistManager", "Track JSON: " + trackJson);

                    FavoriteTrackEntity favorite = new FavoriteTrackEntity(track.getId(), trackJson);
                    favoriteDao.addToFavorites(favorite);
                    android.util.Log.d("UserPlaylistManager", "Added to favorites table");

                    // Also add to favorites playlist
                    addTrackToPlaylist("favorites_default", track);
                    android.util.Log.d("UserPlaylistManager", "Added to favorites playlist");
                }

                android.util.Log.d("UserPlaylistManager", "=== TOGGLE FAVORITE COMPLETED ===");
            } catch (Exception e) {
                android.util.Log.e("UserPlaylistManager", "Error in toggleFavorite: " + e.getMessage(), e);
            }
        });
    }

    public boolean isFavorite(Track track) {
        return favoriteDao.isTrackFavorite(track.getId()) > 0;
    }

    public List<Track> getFavoriteTracks() {
        List<FavoriteTrackEntity> favorites = favoriteDao.getAllFavoritesSync();
        List<Track> tracks = new ArrayList<>();

        if (favorites != null) {
            for (FavoriteTrackEntity favorite : favorites) {
                Track track = gson.fromJson(favorite.trackJson, Track.class);
                tracks.add(track);
            }
        }
        return tracks;
    }

    // ========== RECENTLY PLAYED ==========

    public void addToRecentlyPlayed(Track track) {
        executor.execute(() -> {
            addTrackToPlaylist("recently_played_default", track);

            PlaylistEntity recentlyPlayed = playlistDao.getPlaylistByName("Nghe gần đây");
            if (recentlyPlayed != null) {
                List<Track> tracks = com.example.musicbanger.database.converters.TrackListConverter.toTrackList(recentlyPlayed.tracksJson);

                if (tracks.size() > 50) {
                    tracks = tracks.subList(0, 50);
                    recentlyPlayed.tracksJson = com.example.musicbanger.database.converters.TrackListConverter.fromTrackList(tracks);
                    playlistDao.updatePlaylist(recentlyPlayed);
                }
            }
        });
    }

    // ========== HELPER METHODS ==========

    private Playlist convertToPlaylist(PlaylistEntity entity) {
        Playlist playlist = new Playlist();
        playlist.setId(entity.id);
        playlist.setName(entity.name);
        playlist.setDescription(entity.description);

        List<Track> tracks = com.example.musicbanger.database.converters.TrackListConverter.toTrackList(entity.tracksJson);
        playlist.getTracks().addAll(tracks);

        return playlist;
    }

    public void setCurrentPlayingPlaylist(Playlist playlist) {
        this.currentPlayingPlaylist = playlist;
    }

    public Playlist getCurrentPlayingPlaylist() {
        return currentPlayingPlaylist;
    }

    // THÊM VÀO CUỐI FILE UserPlaylistManager
    public interface FavoriteCallback {
        void onFavoriteChecked(boolean isFavorite);
    }

    public interface PlaylistsCallback {
        void onPlaylistsLoaded(List<Playlist> playlists);
    }

    // THÊM PHƯƠNG THỨC isFavorite VỚI CALLBACK
    public void isFavorite(Track track, FavoriteCallback callback) {
        executor.execute(() -> {
            boolean isFavorite = favoriteDao.isTrackFavorite(track.getId()) > 0;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                callback.onFavoriteChecked(isFavorite);
            });
        });
    }
}