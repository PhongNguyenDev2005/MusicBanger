package com.example.musicbanger;

import com.example.musicbanger.model.Track;
import java.util.ArrayList;
import java.util.List;

public class MusicDataManager {
    private static MusicDataManager instance;
    private List<Track> allTracks = new ArrayList<>();
    private int currentPosition = 0;

    private MusicDataManager() {}

    public static MusicDataManager getInstance() {
        if (instance == null) {
            instance = new MusicDataManager();
        }
        return instance;
    }

    public void setTracks(List<Track> tracks) {
        this.allTracks.clear();
        if (tracks != null) {
            this.allTracks.addAll(tracks);
        }
    }

    public List<Track> getTracks() {
        return new ArrayList<>(allTracks);
    }

    public void setCurrentPosition(int position) {
        this.currentPosition = position;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public Track getCurrentTrack() {
        if (currentPosition >= 0 && currentPosition < allTracks.size()) {
            return allTracks.get(currentPosition);
        }
        return null;
    }

    public Track getTrack(int position) {
        if (position >= 0 && position < allTracks.size()) {
            return allTracks.get(position);
        }
        return null;
    }
}