package com.example.musicbanger.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist implements Parcelable {
    private String id;
    private String name;
    private String description;
    private List<Track> tracks;
    private long createdAt;
    private boolean isDefault;

    public Playlist() {
        this.id = UUID.randomUUID().toString();
        this.tracks = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.isDefault = false;
    }

    public Playlist(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    public Playlist(String id, String name, String description) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
    }

    // GETTERS AND SETTERS
    public String getId() { return id; }
    public void setId(String id) { this.id = id; } // THÊM SETTER NÀY

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    // Playlist operations
    public void addTrack(Track track) {
        if (!tracks.contains(track)) {
            tracks.add(track);
        }
    }

    public void addTracks(List<Track> newTracks) {
        for (Track track : newTracks) {
            if (!tracks.contains(track)) {
                tracks.add(track);
            }
        }
    }

    public void removeTrack(Track track) {
        tracks.remove(track);
    }

    public void removeTrack(int position) {
        if (position >= 0 && position < tracks.size()) {
            tracks.remove(position);
        }
    }

    public void clearPlaylist() {
        tracks.clear();
    }

    public boolean containsTrack(Track track) {
        return tracks.contains(track);
    }

    public int getTrackCount() {
        return tracks.size();
    }

    // Parcelable implementation
    protected Playlist(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        tracks = in.createTypedArrayList(Track.CREATOR);
        createdAt = in.readLong();
        isDefault = in.readByte() != 0;
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel in) {
            return new Playlist(in);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeTypedList(tracks);
        dest.writeLong(createdAt);
        dest.writeByte((byte) (isDefault ? 1 : 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id.equals(playlist.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}