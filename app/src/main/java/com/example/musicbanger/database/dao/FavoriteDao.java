package com.example.musicbanger.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.example.musicbanger.database.entities.FavoriteTrackEntity;
import java.util.List;

@Dao
public interface FavoriteDao {

    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    LiveData<List<FavoriteTrackEntity>> getAllFavorites();

    // THÊM PHƯƠNG THỨC SYNC
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    List<FavoriteTrackEntity> getAllFavoritesSync();

    @Query("SELECT * FROM favorite_tracks WHERE trackId = :trackId")
    FavoriteTrackEntity getFavoriteByTrackId(String trackId);

    @Insert
    void addToFavorites(FavoriteTrackEntity favorite);

    @Delete
    void removeFromFavorites(FavoriteTrackEntity favorite);

    @Query("DELETE FROM favorite_tracks WHERE trackId = :trackId")
    void removeFavoriteByTrackId(String trackId);

    @Query("SELECT COUNT(*) FROM favorite_tracks WHERE trackId = :trackId")
    int isTrackFavorite(String trackId);
}