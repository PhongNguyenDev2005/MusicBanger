package com.example.musicbanger.database.converters;

import androidx.room.TypeConverter;
import com.example.musicbanger.model.Track;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TrackListConverter {
    private static Gson gson = new Gson();

    @TypeConverter
    public static String fromTrackList(List<Track> tracks) {
        if (tracks == null) {
            return "[]";
        }
        return gson.toJson(tracks);
    }

    @TypeConverter
    public static List<Track> toTrackList(String tracksJson) {
        if (tracksJson == null || tracksJson.equals("[]")) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Track>>() {}.getType();
        return gson.fromJson(tracksJson, type);
    }
}