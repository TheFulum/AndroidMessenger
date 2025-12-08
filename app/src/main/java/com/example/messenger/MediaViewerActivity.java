package com.example.messenger;

import android.app.DownloadManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.messenger.databinding.ActivityMediaViewerBinding;

public class MediaViewerActivity extends AppCompatActivity {

    private static final String TAG = "MediaViewerActivity";
    private static final int VIDEO_LOAD_TIMEOUT = 30000; // 30 секунд

    private ActivityMediaViewerBinding binding;
    private String mediaUrl;
    private String mediaType; // "image" или "video"
    private String title;

    private Handler videoHandler;
    private Runnable videoRunnable;
    private Runnable timeoutRunnable;
    private boolean isPlaying = false;

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
        binding.videoView.setOnClickListener(v -> toggleBars());
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

        Log.d(TAG, "Setting video URI: " + mediaUrl);
        binding.videoView.setVideoURI(Uri.parse(mediaUrl));

        binding.videoView.setOnErrorListener((mp, what, extra) -> {
            String errorMsg = "Ошибка загрузки видео - What: " + what + ", Extra: " + extra;
            Log.e(TAG, errorMsg);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            binding.loadingProgress.setVisibility(View.GONE);
            return true;
        });

        binding.videoView.setOnPreparedListener(mp -> {
            Log.d(TAG, "Video prepared successfully");
            binding.loadingProgress.setVisibility(View.GONE);
            binding.videoView.setVisibility(View.VISIBLE);
            binding.playBtn.setVisibility(View.VISIBLE);
            binding.videoControls.setVisibility(View.VISIBLE);

            // Устанавливаем длительность
            int duration = binding.videoView.getDuration();
            binding.videoSeekbar.setMax(duration);
            binding.totalTimeTv.setText(formatTime(duration));
            binding.currentTimeTv.setText(formatTime(0));

            Log.d(TAG, "Video duration: " + duration + "ms");
        });

        // Кнопка Play/Pause
        binding.playBtn.setOnClickListener(v -> {
            if (isPlaying) {
                pauseVideo();
            } else {
                playVideo();
            }
        });

        // SeekBar
        binding.videoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    binding.videoView.seekTo(progress);
                    binding.currentTimeTv.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        binding.videoView.setOnCompletionListener(mp -> {
            Log.d(TAG, "Video completed");
            isPlaying = false;
            binding.playBtn.setImageResource(R.drawable.ic_play);
            binding.playBtn.setVisibility(View.VISIBLE);
            stopVideoProgressUpdater();
        });
    }

    private void playVideo() {
        Log.d(TAG, "Playing video");
        binding.videoView.start();
        isPlaying = true;
        binding.playBtn.setImageResource(R.drawable.ic_pause);
        binding.playBtn.setVisibility(View.GONE);
        startVideoProgressUpdater();
    }

    private void pauseVideo() {
        Log.d(TAG, "Pausing video");
        binding.videoView.pause();
        isPlaying = false;
        binding.playBtn.setImageResource(R.drawable.ic_play);
        binding.playBtn.setVisibility(View.VISIBLE);
        stopVideoProgressUpdater();
    }

    private void startVideoProgressUpdater() {
        if (videoHandler != null && videoRunnable != null) {
            videoHandler.removeCallbacks(videoRunnable);
        }

        videoHandler = new Handler(Looper.getMainLooper());
        videoRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying && binding != null && binding.videoView != null) {
                    try {
                        int currentPosition = binding.videoView.getCurrentPosition();
                        binding.videoSeekbar.setProgress(currentPosition);
                        binding.currentTimeTv.setText(formatTime(currentPosition));
                        videoHandler.postDelayed(this, 100);
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating progress", e);
                    }
                }
            }
        };
        videoHandler.post(videoRunnable);
    }

    private void stopVideoProgressUpdater() {
        if (videoHandler != null && videoRunnable != null) {
            videoHandler.removeCallbacks(videoRunnable);
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void toggleBars() {
        if (binding.topBar.getVisibility() == View.VISIBLE) {
            binding.topBar.setVisibility(View.GONE);
            if (mediaType.equals("video")) {
                binding.videoControls.setVisibility(View.GONE);
            }
        } else {
            binding.topBar.setVisibility(View.VISIBLE);
            if (mediaType.equals("video")) {
                binding.videoControls.setVisibility(View.VISIBLE);
            }
        }
    }

    private void downloadMedia() {
        try {
            // Получаем имя файла из URL или создаем новое
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
            request.setDescription("Downloading file...");
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

    @Override
    protected void onPause() {
        super.onPause();
        if (isPlaying && binding != null && binding.videoView != null) {
            pauseVideo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVideoProgressUpdater();

        // Отменяем таймаут
        if (videoHandler != null && timeoutRunnable != null) {
            videoHandler.removeCallbacks(timeoutRunnable);
        }

        if (binding != null && binding.videoView != null) {
            binding.videoView.stopPlayback();
        }
        binding = null;
    }
}