package com.example.messenger.config;

import android.annotation.SuppressLint;

public class AppConfig {

    // ==================== CLOUDINARY ====================

    /**
        keys: https://cloudinary.com/console
     */
    public static final String CLOUDINARY_CLOUD_NAME = "dsfmj1rgd";
    public static final String CLOUDINARY_API_KEY = "292327364799723";
    public static final String CLOUDINARY_API_SECRET = "ViwIhwljI2owz0zxdFqVX4c8U58";


    public static class CloudinaryFolders {
        public static final String IMAGES = "messenger_images";
        public static final String VIDEOS = "messenger_videos";
        public static final String DOCUMENTS = "messenger_files";
        public static final String VOICES = "messenger_voices";
        public static final String PROFILE_IMAGES = "profile_images";
    }

    // ==================== FIREBASE ====================

    /**
     *keys: in google-services.json
     */
    public static class Firebase {

        public static final int MAX_USERNAME_LENGTH = 20;
        public static final int MIN_USERNAME_LENGTH = 3;
        public static final int MIN_PASSWORD_LENGTH = 6;
        public static final int MAX_PASSWORD_LENGTH = 30;
    }

    // ==================== FILE UPLOAD ====================

    public static class FileUpload {
        public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;      // 100 MB
        public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;      // 10 MB
        public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;      // 50 MB
        public static final long MAX_VOICE_SIZE = 5 * 1024 * 1024;       // 5 MB
        public static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024;   // 20 MB

    }

    // ==================== NOTIFICATIONS ====================

    public static class Notifications {
        public static final String CHANNEL_ID = "messages_channel";
        public static final String CHANNEL_NAME = "Message";
        public static final String FOREGROUND_CHANNEL_ID = "foreground_service_channel";
        public static final int FOREGROUND_SERVICE_ID = 1;
    }


    // ==================== PHONE NUMBERS ====================

    public static class PhoneNumbers {
        public static final String DEFAULT_COUNTRY_CODE = "+375";
        public static final String PHONE_MASK = "+375(__) ___-__-__";
        public static final int PHONE_DIGITS_LENGTH = 9;
    }

    // ==================== HELPER METHODS ====================

    public static java.util.Map<String, Object> getCloudinaryConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
        config.put("api_key", CLOUDINARY_API_KEY);
        config.put("api_secret", CLOUDINARY_API_SECRET);
        return config;
    }
    public static boolean isFileSizeValid(long fileSize, String fileType) {
        if (fileType == null) return fileSize <= FileUpload.MAX_FILE_SIZE;

        if (fileType.startsWith("image/")) {
            return fileSize <= FileUpload.MAX_IMAGE_SIZE;
        } else if (fileType.startsWith("video/")) {
            return fileSize <= FileUpload.MAX_VIDEO_SIZE;
        } else if (fileType.equals("voice")) {
            return fileSize <= FileUpload.MAX_VOICE_SIZE;
        } else {
            return fileSize <= FileUpload.MAX_DOCUMENT_SIZE;
        }
    }

    public static long getMaxFileSizeForType(String fileType) {
        if (fileType == null) return FileUpload.MAX_FILE_SIZE;

        if (fileType.startsWith("image/")) {
            return FileUpload.MAX_IMAGE_SIZE;
        } else if (fileType.startsWith("video/")) {
            return FileUpload.MAX_VIDEO_SIZE;
        } else if (fileType.equals("voice")) {
            return FileUpload.MAX_VOICE_SIZE;
        } else {
            return FileUpload.MAX_DOCUMENT_SIZE;
        }
    }

    @SuppressLint("DefaultLocale")
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}