package com.example.musicbanger;

import android.app.Application;
import com.example.musicbanger.manager.UserPlaylistManager;

public class MusicBangerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo UserPlaylistManager
        UserPlaylistManager.initialize(this);
    }
}