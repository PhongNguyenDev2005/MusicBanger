package com.example.musicbanger.api;

import com.example.musicbanger.model.Track;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import java.util.List;

public interface JamendoApi {

    // THAY THẾ BẰNG CLIENT ID CỦA BẠN
    String CLIENT_ID = "06ac505c";

    // Lấy bài hát phổ biến
    @GET("v3.0/tracks/")
    Call<JamendoResponse> getPopularTracks(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("order") String order
    );

    // Tìm kiếm bài hát
    @GET("v3.0/tracks/")
    Call<JamendoResponse> searchTracks(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("search") String query
    );

    // Lấy bài hát theo thể loại
    @GET("v3.0/tracks/")
    Call<JamendoResponse> getTracksByGenre(
            @Query("client_id") String clientId,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("tags") String genre
    );

    // Response class
    class JamendoResponse {
        public List<JamendoTrack> results;
        public Headers headers;

        public class Headers {
            public String status;
            public int code;
            public String error_message;
            public String warnings;
            public int results_count;
        }
    }

    // Track class
    class JamendoTrack {
        public String id;
        public String name;
        public String artist_name;
        public String album_name;
        public String audio;           // URL stream audio
        public String audiodownload;   // URL download
        public String image;
        public String releasedate;
        public int duration;
        public String license_ccurl;
        public List<String> tags;

        // Convert to our Track model
        public Track toTrack() {
            return new Track(
                    id,
                    name,
                    artist_name != null ? artist_name : "Unknown Artist",
                    album_name != null ? album_name : "Unknown Album",
                    audio, // SỬA: Dùng 'audio' thay vì 'preview'
                    image, // Album art
                    duration // Duration in seconds
            );
        }
    }
}