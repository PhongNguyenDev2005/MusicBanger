package com.example.musicbanger.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.example.musicbanger.database.converters.TrackListConverter;

@Entity(tableName = "playlists")
@TypeConverters(TrackListConverter.class)
public class PlaylistEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public String description;
    public long createdAt;
    public boolean isDefault;
    public String tracksJson;

    public PlaylistEntity() {
        this.id = ""; // Khởi tạo giá trị mặc định
    }

    public PlaylistEntity(@NonNull String id, String name, String description, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = System.currentTimeMillis();
        this.isDefault = isDefault;
        this.tracksJson = "[]";
    }
}