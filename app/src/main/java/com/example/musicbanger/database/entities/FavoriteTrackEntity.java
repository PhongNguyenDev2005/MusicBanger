package com.example.musicbanger.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_tracks")
public class FavoriteTrackEntity {
    @PrimaryKey
    @NonNull
    public String trackId;

    public String trackJson;
    public long addedAt;

    public FavoriteTrackEntity() {
        this.trackId = "";
    }

    public FavoriteTrackEntity(@NonNull String trackId, String trackJson) {
        this.trackId = trackId;
        this.trackJson = trackJson;
        this.addedAt = System.currentTimeMillis();
    }
}