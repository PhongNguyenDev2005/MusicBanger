package com.example.musicbanger.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.musicbanger.database.dao.FavoriteDao;
import com.example.musicbanger.database.dao.PlaylistDao;
import com.example.musicbanger.database.entities.FavoriteTrackEntity;
import com.example.musicbanger.database.entities.PlaylistEntity;

@Database(
        entities = {PlaylistEntity.class, FavoriteTrackEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class MusicDatabase extends RoomDatabase {

    private static MusicDatabase instance;

    public abstract PlaylistDao playlistDao();
    public abstract FavoriteDao favoriteDao();

    public static synchronized MusicDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            MusicDatabase.class,
                            "music_database"
                    ).fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();

            // Tạo playlist mặc định
            initializeDefaultData(instance);
        }
        return instance;
    }

    private static void initializeDefaultData(MusicDatabase database) {
        new Thread(() -> {
            android.util.Log.d("MusicDatabase", "Initializing default data...");

            // Tạo playlist "Bài hát yêu thích" nếu chưa có
            if (database.playlistDao().getPlaylistByName("Bài hát yêu thích") == null) {
                android.util.Log.d("MusicDatabase", "Creating favorites playlist");
                PlaylistEntity favorites = new PlaylistEntity(
                        "favorites_default",
                        "Bài hát yêu thích",
                        "Những bài hát bạn yêu thích",
                        true
                );
                database.playlistDao().insertPlaylist(favorites);
            } else {
                android.util.Log.d("MusicDatabase", "Favorites playlist already exists");
            }

            // Tạo playlist "Nghe gần đây" nếu chưa có
            if (database.playlistDao().getPlaylistByName("Nghe gần đây") == null) {
                android.util.Log.d("MusicDatabase", "Creating recently played playlist");
                PlaylistEntity recentlyPlayed = new PlaylistEntity(
                        "recently_played_default",
                        "Nghe gần đây",
                        "Những bài hát bạn đã nghe",
                        true
                );
                database.playlistDao().insertPlaylist(recentlyPlayed);
            } else {
                android.util.Log.d("MusicDatabase", "Recently played playlist already exists");
            }

            android.util.Log.d("MusicDatabase", "Default data initialization completed");
        }).start();
    }
}