package com.example.musicbanger.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.musicbanger.database.entities.PlaylistEntity;
import java.util.List;

@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    LiveData<List<PlaylistEntity>> getAllPlaylists();

    // THÊM PHƯƠNG THỨC SYNC CHO BACKGROUND THREAD
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllPlaylistsSync();

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    PlaylistEntity getPlaylistById(String playlistId);

    @Query("SELECT * FROM playlists WHERE name = :name")
    PlaylistEntity getPlaylistByName(String name);

    @Insert
    void insertPlaylist(PlaylistEntity playlist);

    @Update
    void updatePlaylist(PlaylistEntity playlist);

    @Delete
    void deletePlaylist(PlaylistEntity playlist);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylistById(String playlistId);
}