package com.example.musicbanger.service;

import com.example.musicbanger.model.Track;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PlaylistManager {
    public enum RepeatMode { NONE, ONE, ALL }

    private final List<Track> original = new ArrayList<>();
    private final List<Track> playback = new ArrayList<>();
    private int index = 0;
    private boolean shuffle = false;
    private RepeatMode repeatMode = RepeatMode.NONE;

    public void setPlaylist(List<Track> tracks, int startIndex) {
        original.clear();
        original.addAll(tracks);
        rebuild();
        index = Math.max(0, Math.min(startIndex, playback.size() - 1));
    }

    private void rebuild() {
        playback.clear();
        playback.addAll(original);
        if (shuffle) Collections.shuffle(playback, new Random(System.currentTimeMillis()));
    }

    public Track getCurrent() {
        if (playback.isEmpty() || index < 0 || index >= playback.size()) {
            return null;
        }
        return playback.get(index);
    }

    public Track next() {
        if (playback.isEmpty()) return null;
        if (repeatMode == RepeatMode.ONE) return getCurrent();

        index++;
        if (index >= playback.size()) {
            if (repeatMode == RepeatMode.ALL) {
                index = 0;
            } else {
                index = playback.size() - 1;
                return null;
            }
        }
        return getCurrent();
    }

    public Track previous() {
        if (playback.isEmpty()) return null;
        if (repeatMode == RepeatMode.ONE) return getCurrent();

        index--;
        if (index < 0) {
            if (repeatMode == RepeatMode.ALL) {
                index = playback.size() - 1;
            } else {
                index = 0;
                return null;
            }
        }
        return getCurrent();
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        Track current = getCurrent();
        rebuild();
        if (current != null) {
            index = playback.indexOf(current);
            if (index == -1) index = 0;
        }
    }

    public void toggleRepeat() {
        switch (repeatMode) {
            case NONE:
                repeatMode = RepeatMode.ALL;
                break;
            case ALL:
                repeatMode = RepeatMode.ONE;
                break;
            case ONE:
                repeatMode = RepeatMode.NONE;
                break;
        }
    }

    public boolean isShuffle() { return shuffle; }
    public RepeatMode getRepeatMode() { return repeatMode; }
    public int getCurrentIndex() { return index; }
    public List<Track> getPlaylist() { return new ArrayList<>(playback); }
}