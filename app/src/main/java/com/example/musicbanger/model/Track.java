package com.example.musicbanger.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

public class Track implements Parcelable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private Artist artist;

    @SerializedName("album")
    private Album album;

    @SerializedName("preview")
    private String streamUrl;

    @SerializedName("duration")
    private int duration;

    // LOẠI BỎ CÁC FIELD URI - CHÚNG SẼ ĐƯỢC TÍNH TOÁN KHI CẦN
    // private Uri streamUri;
    // private Uri artworkUri;

    // Constructor mặc định
    public Track() {
    }

    public Track(String id, String title, String artistName, String albumName,
                 String streamUrl, String artworkUrl, int duration) {
        this.id = id;
        this.title = title;
        this.artist = new Artist(artistName);
        this.album = new Album(albumName, artworkUrl);
        this.streamUrl = streamUrl;
        this.duration = duration;
    }

    // Getter methods - TÍNH TOÁN URI KHI CẦN
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtistName() {
        return artist != null ? artist.getName() : "Unknown Artist";
    }
    public String getAlbumName() {
        return album != null ? album.getTitle() : "Unknown Album";
    }

    public Uri getStreamUri() {
        // TÍNH TOÁN KHI CẦN, KHÔNG LƯU TRỮ
        return streamUrl != null ? Uri.parse(streamUrl) : null;
    }

    public Uri getArtworkUri() {
        // TÍNH TOÁN KHI CẦN, KHÔNG LƯU TRỮ
        if (album != null && album.getCover() != null) {
            return Uri.parse(album.getCover());
        }
        return null;
    }

    public int getDuration() { return duration; }
    public String getStreamUrl() { return streamUrl; }

    // Inner classes
    public static class Artist implements Parcelable {
        @SerializedName("name")
        private String name;

        public Artist(String name) {
            this.name = name;
        }

        public String getName() { return name; }

        protected Artist(Parcel in) {
            name = in.readString();
        }

        public static final Creator<Artist> CREATOR = new Creator<Artist>() {
            @Override
            public Artist createFromParcel(Parcel in) {
                return new Artist(in);
            }

            @Override
            public Artist[] newArray(int size) {
                return new Artist[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(name);
        }
    }

    public static class Album implements Parcelable {
        @SerializedName("title")
        private String title;

        @SerializedName("cover")
        private String cover;

        public Album(String title, String cover) {
            this.title = title;
            this.cover = cover;
        }

        public String getTitle() { return title; }
        public String getCover() { return cover; }

        protected Album(Parcel in) {
            title = in.readString();
            cover = in.readString();
        }

        public static final Creator<Album> CREATOR = new Creator<Album>() {
            @Override
            public Album createFromParcel(Parcel in) {
                return new Album(in);
            }

            @Override
            public Album[] newArray(int size) {
                return new Album[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(title);
            dest.writeString(cover);
        }
    }

    // Parcelable implementation - SỬA LẠI, KHÔNG GHI/ĐỌC URI
    protected Track(Parcel in) {
        id = in.readString();
        title = in.readString();
        streamUrl = in.readString();
        duration = in.readInt();
        artist = in.readParcelable(Artist.class.getClassLoader());
        album = in.readParcelable(Album.class.getClassLoader());
    }

    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(streamUrl);
        dest.writeInt(duration);
        dest.writeParcelable(artist, flags);
        dest.writeParcelable(album, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return Objects.equals(id, track.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}