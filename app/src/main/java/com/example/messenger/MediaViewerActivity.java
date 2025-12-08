package com.example.messenger;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.messenger.databinding.ActivityMediaViewerBinding;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;

public class MediaViewerActivity extends AppCompatActivity {

    private static final String TAG = "MediaViewerActivity";

    private ActivityMediaViewerBinding binding;
    private String mediaUrl;
    private String mediaType; // "image" или "video"
    private String title;

    private ExoPlayer player;
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Полноэкранный режим
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        binding = ActivityMediaViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Получаем данные из Intent
        mediaUrl = getIntent().getStringExtra("mediaUrl");
        mediaType = getIntent().getStringExtra("mediaType");
        title = getIntent().getStringExtra("title");

        Log.d(TAG, "Media URL: " + mediaUrl);
        Log.d(TAG, "Media Type: " + mediaType);

        if (mediaUrl == null || mediaType == null) {
            Toast.makeText(this, "Ошибка загрузки медиа", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadMedia();
    }

    private void setupUI() {
        // Заголовок
        binding.titleTv.setText(title != null ? title : "Медиа");

        // Кнопка назад
        binding.backBtn.setOnClickListener(v -> finish());

        // Кнопка скачать
        binding.downloadBtn.setOnClickListener(v -> downloadMedia());

        // Клик по экрану для скрытия/показа панелей
        binding.photoView.setOnClickListener(v -> toggleBars());

        // Для ExoPlayer управление панелями встроено
    }

    private void loadMedia() {
        if (mediaType.equals("image")) {
            loadImage();
        } else if (mediaType.equals("video")) {
            Log.d(TAG, "Loading video...");
            loadVideo();
        }
    }

    private void loadImage() {
        binding.loadingProgress.setVisibility(View.VISIBLE);

        Glide.with(this)
                .load(mediaUrl)
                .into(binding.photoView);

        // Скрываем прогресс после загрузки
        binding.photoView.postDelayed(() -> {
            binding.loadingProgress.setVisibility(View.GONE);
            binding.photoView.setVisibility(View.VISIBLE);
        }, 500);
    }

    private void loadVideo() {
        binding.loadingProgress.setVisibility(View.VISIBLE);
        binding.playerView.setVisibility(View.VISIBLE);

        Log.d(TAG, "Initializing ExoPlayer for URL: " + mediaUrl);

        try {
            initializePlayer();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing player", e);
            binding.loadingProgress.setVisibility(View.GONE);
            Toast.makeText(this, "Ошибка загрузки видео: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializePlayer() {
        if (player != null) {
            return;
        }

        // Создаем ExoPlayer
        player = new ExoPlayer.Builder(this).build();

        // Привязываем к UI
        binding.playerView.setPlayer(player);

        // Настраиваем источник данных с правильными заголовками
        DefaultHttpDataSource.Factory httpDataSourceFactory =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(Util.getUserAgent(this, "MessengerApp"))
                        .setAllowCrossProtocolRedirects(true);

        DefaultDataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(this, httpDataSourceFactory);

        // Создаем MediaSource
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl));
        MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);

        // Настраиваем плеер
        player.setMediaSource(mediaSource);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);

        // Добавляем слушатели
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                String stateString;
                switch (playbackState) {
                    case ExoPlayer.STATE_IDLE:
                        stateString = "IDLE";
                        break;
                    case ExoPlayer.STATE_BUFFERING:
                        stateString = "BUFFERING";
                        binding.loadingProgress.setVisibility(View.VISIBLE);
                        break;
                    case ExoPlayer.STATE_READY:
                        stateString = "READY";
                        binding.loadingProgress.setVisibility(View.GONE);
                        break;
                    case ExoPlayer.STATE_ENDED:
                        stateString = "ENDED";
                        binding.loadingProgress.setVisibility(View.GONE);
                        break;
                    default:
                        stateString = "UNKNOWN";
                        break;
                }
                Log.d(TAG, "Playback state changed to: " + stateString);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage(), error);
                binding.loadingProgress.setVisibility(View.GONE);

                String errorMessage = "Ошибка воспроизведения";
                if (error.getMessage() != null) {
                    errorMessage += ": " + error.getMessage();
                }

                Toast.makeText(MediaViewerActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "Is playing: " + isPlaying);
            }
        });

        // Начинаем подготовку
        player.prepare();

        Log.d(TAG, "Player initialized and preparing media");
    }

    private void toggleBars() {
        if (binding.topBar.getVisibility() == View.VISIBLE) {
            binding.topBar.setVisibility(View.GONE);
        } else {
            binding.topBar.setVisibility(View.VISIBLE);
        }
    }

    private void downloadMedia() {
        try {
            String fileName;
            if (mediaType.equals("image")) {
                fileName = "image_" + System.currentTimeMillis() + ".jpg";
            } else if (mediaType.equals("video")) {
                fileName = "video_" + System.currentTimeMillis() + ".mp4";
            } else {
                fileName = "file_" + System.currentTimeMillis();
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mediaUrl));
            request.setTitle(fileName);
            request.setDescription("Скачивание файла...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Скачивание начато", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка скачивания", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Download error", e);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentMediaItemIndex();
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23 && mediaType.equals("video")) {
            if (player == null) {
                initializePlayer();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null) && mediaType.equals("video")) {
            initializePlayer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
        binding = null;
    }
}