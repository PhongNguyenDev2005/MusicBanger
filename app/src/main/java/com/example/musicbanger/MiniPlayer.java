package com.example.musicbanger;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.musicbanger.MainActivity;
import com.example.musicbanger.NowPlayingActivity;
import com.example.musicbanger.R;
import com.example.musicbanger.model.Track;
import com.example.musicbanger.service.MusicService;

public class MiniPlayer extends ConstraintLayout {
    private static final String TAG = "MiniPlayer";

    // Views
    private CardView cardView;
    private ImageView ivAlbumArt, ivPlayPause, ivNext;
    private TextView tvSongTitle, tvArtist;

    // Callback interface
    public interface MiniPlayerListener {
        void onPlayPauseClicked();
        void onNextClicked();
        void onMiniPlayerClicked();
        MusicService getMusicService();
    }

    private MiniPlayerListener listener;
    private boolean isPlaying = false;

    public MiniPlayer(Context context) {
        super(context);
        init(context);
    }

    public MiniPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MiniPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.component_mini_player, this, true);

        // Initialize views
        cardView = view.findViewById(R.id.miniPlayerCard);
        ivAlbumArt = view.findViewById(R.id.ivMiniAlbumArt);
        ivPlayPause = view.findViewById(R.id.ivMiniPlayPause);
        ivNext = view.findViewById(R.id.ivMiniNext);
        tvSongTitle = view.findViewById(R.id.tvMiniSongTitle);
        tvArtist = view.findViewById(R.id.tvMiniArtist);

        // Set up click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        // Click to open NowPlaying
        cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMiniPlayerClicked();
            }
        });

        // Play/Pause button
        ivPlayPause.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlayPauseClicked();
            }
        });

        // Next button
        ivNext.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNextClicked();
            }
        });
    }

    /**
     * Set listener for mini player events
     */
    public void setListener(MiniPlayerListener listener) {
        this.listener = listener;
    }

    /**
     * Update mini player with current track info
     */
    public void updatePlayer(Track currentTrack, boolean isPlaying) {
        this.isPlaying = isPlaying;

        if (currentTrack != null) {
            setVisibility(View.VISIBLE);

            // Update song info
            tvSongTitle.setText(currentTrack.getTitle());
            tvArtist.setText(currentTrack.getArtistName());

            // Update play/pause icon
            ivPlayPause.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
            );

            // Load album art
            loadAlbumArt(currentTrack.getArtworkUri());

        } else {
            setVisibility(View.GONE);
        }
    }

    /**
     * Load album art and extract dominant color
     */
    private void loadAlbumArt(Uri artworkUri) {
        if (artworkUri != null) {
            Glide.with(getContext())
                    .load(artworkUri)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(ivAlbumArt);

            // Extract dominant color for background
            extractDominantColor(artworkUri);
        } else {
            ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            setDefaultBackground();
        }
    }

    /**
     * Extract dominant color from image and set as background
     */
    private void extractDominantColor(Uri imageUri) {
        try {
            Glide.with(getContext())
                    .asBitmap()
                    .load(imageUri)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource,
                                                    @Nullable Transition<? super Bitmap> transition) {
                            Palette.from(resource).generate(palette -> {
                                if (palette != null) {
                                    int dominantColor = palette.getDominantColor(
                                            getResources().getColor(R.color.card_background)
                                    );

                                    // Get vibrant color if available, otherwise use dominant
                                    int vibrantColor = palette.getVibrantColor(dominantColor);
                                    int finalColor = vibrantColor != dominantColor ? vibrantColor : dominantColor;

                                    // Adjust alpha for better visibility
                                    int backgroundColor = adjustColorAlpha(finalColor, 0.3f);

                                    // Animate background color change
                                    animateBackgroundColor(backgroundColor);
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error extracting dominant color", e);
            setDefaultBackground();
        }
    }

    /**
     * Adjust color alpha
     */
    private int adjustColorAlpha(int color, float alpha) {
        int alphaValue = Math.min(255, Math.max(0, (int)(alpha * 255))) << 24;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return alphaValue | (red << 16) | (green << 8) | blue;
    }

    /**
     * Animate background color change
     */
    private void animateBackgroundColor(int targetColor) {
        int currentColor = cardView.getCardBackgroundColor().getDefaultColor();

        ValueAnimator colorAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), currentColor, targetColor
        );
        colorAnimation.setDuration(800);
        colorAnimation.addUpdateListener(animator -> {
            int color = (int) animator.getAnimatedValue();
            cardView.setCardBackgroundColor(color);
        });
        colorAnimation.start();
    }

    /**
     * Set default background color
     */
    private void setDefaultBackground() {
        cardView.setCardBackgroundColor(
                getResources().getColor(R.color.card_background)
        );
    }

    /**
     * Update play/pause button state
     */
    public void updatePlayPauseState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        ivPlayPause.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
        );
    }

    /**
     * Show loading state
     */
    public void showLoading() {
        setVisibility(View.VISIBLE);
        tvSongTitle.setText("Đang tải...");
        tvArtist.setText("");
        ivPlayPause.setImageResource(R.drawable.ic_pause);
        setDefaultBackground();
    }

    /**
     * Hide mini player
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * Show mini player
     */
    public void show() {
        setVisibility(View.VISIBLE);
    }
}