package com.example.musicbanger.database.converters;

import androidx.room.TypeConverter;
import com.example.musicbanger.model.Track;
import com.google.gson.Gson;

public class TrackConverter {
    private static Gson gson = new Gson();

    @TypeConverter
    public static String fromTrack(Track track) {
        if (track == null) {
            return null;
        }
        return gson.toJson(track);
    }

    @TypeConverter
    public static Track toTrack(String trackJson) {
        if (trackJson == null) {
            return null;
        }
        return gson.fromJson(trackJson, Track.class);
    }
}