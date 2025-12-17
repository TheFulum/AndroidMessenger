package com.example.messenger.config;

/**
 * Конфигурация приложения
 * Здесь хранятся все API ключи, пароли и настройки сторонних сервисов
 *
 * ВАЖНО: Перед коммитом в Git добавьте этот файл в .gitignore!
 */
public class AppConfig {

    // ==================== CLOUDINARY ====================

    /**
     * Cloudinary - сервис для хранения изображений и файлов
     * Получить ключи: https://cloudinary.com/console
     */
    public static final String CLOUDINARY_CLOUD_NAME = "dsfmj1rgd";
    public static final String CLOUDINARY_API_KEY = "292327364799723";
    public static final String CLOUDINARY_API_SECRET = "ViwIhwljI2owz0zxdFqVX4c8U58";

    /**
     * Папки для различных типов файлов в Cloudinary
     */
    public static class CloudinaryFolders {
        public static final String IMAGES = "messenger_images";
        public static final String VIDEOS = "messenger_videos";
        public static final String DOCUMENTS = "messenger_files";
        public static final String VOICES = "messenger_voices";
        public static final String PROFILE_IMAGES = "profile_images";
    }

    // ==================== FIREBASE ====================

    /**
     * Firebase конфигурация
     * Ключи находятся в google-services.json
     * Дополнительные настройки можно указать здесь
     */
    public static class Firebase {
        // Таймауты для операций (в миллисекундах)
        public static final long DATABASE_TIMEOUT = 10000; // 10 секунд
        public static final long STORAGE_TIMEOUT = 30000;  // 30 секунд

        // Максимальные размеры
        public static final int MAX_MESSAGE_LENGTH = 5000;
        public static final int MAX_USERNAME_LENGTH = 20;
        public static final int MIN_USERNAME_LENGTH = 3;
        public static final int MIN_PASSWORD_LENGTH = 6;
        public static final int MAX_PASSWORD_LENGTH = 30;
    }

    // ==================== FILE UPLOAD ====================

    /**
     * Настройки загрузки файлов
     */
    public static class FileUpload {
        // Максимальные размеры файлов (в байтах)
        public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;      // 100 MB
        public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;      // 10 MB
        public static final long MAX_VIDEO_SIZE = 50 * 1024 * 1024;      // 50 MB
        public static final long MAX_VOICE_SIZE = 5 * 1024 * 1024;       // 5 MB
        public static final long MAX_DOCUMENT_SIZE = 20 * 1024 * 1024;   // 20 MB

        // Поддерживаемые форматы
        public static final String[] SUPPORTED_IMAGE_FORMATS = {
                "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        };

        public static final String[] SUPPORTED_VIDEO_FORMATS = {
                "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo"
        };

        public static final String[] SUPPORTED_AUDIO_FORMATS = {
                "audio/mpeg", "audio/mp4", "audio/x-m4a", "audio/wav"
        };
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Настройки уведомлений
     */
    public static class Notifications {
        public static final String CHANNEL_ID = "messages_channel";
        public static final String CHANNEL_NAME = "Message";
        public static final String FOREGROUND_CHANNEL_ID = "foreground_service_channel";
        public static final int FOREGROUND_SERVICE_ID = 1;
    }

    // ==================== UI SETTINGS ====================

    /**
     * Настройки пользовательского интерфейса
     */
    public static class UI {
        // Цвета (можно использовать вместо прямых ссылок на resources)
        public static final int COLOR_PRIMARY = 0xFF2196F3;
        public static final int COLOR_ONLINE = 0xFF4CAF50;
        public static final int COLOR_OFFLINE = 0xFF9E9E9E;

        // Таймауты для UI операций
        public static final long SCROLL_DELAY_MS = 300;
        public static final long SPLASH_DELAY_MS = 2000;
        public static final long TYPING_INDICATOR_DELAY_MS = 3000;

        // Количество элементов для пагинации
        public static final int MESSAGES_PER_PAGE = 50;
        public static final int CHATS_PER_PAGE = 20;
        public static final int USERS_PER_PAGE = 20;
    }

    // ==================== CACHE ====================

    /**
     * Настройки кэширования
     */
    public static class Cache {
        public static final long IMAGE_CACHE_SIZE = 50 * 1024 * 1024;  // 50 MB
        public static final long DISK_CACHE_SIZE = 100 * 1024 * 1024;  // 100 MB
        public static final int MEMORY_CACHE_PERCENTAGE = 15;           // 15% от доступной памяти
    }

    // ==================== PHONE NUMBERS ====================

    /**
     * Настройки для телефонных номеров
     */
    public static class PhoneNumbers {
        public static final String DEFAULT_COUNTRY_CODE = "+375";
        public static final String PHONE_MASK = "+375(__) ___-__-__";
        public static final int PHONE_DIGITS_LENGTH = 9; // Без кода страны
    }

    // ==================== DEBUG ====================

    /**
     * Настройки для отладки
     * В продакшене установить все в false
     */
    public static class Debug {
        public static final boolean ENABLE_LOGGING = true;
        public static final boolean ENABLE_CRASH_REPORTING = false;
        public static final boolean SHOW_TOAST_ERRORS = true;
        public static final boolean ENABLE_ANALYTICS = false;
    }

    // ==================== API ENDPOINTS ====================

    /**
     * Дополнительные API эндпоинты (если используются)
     */
    public static class ApiEndpoints {
        // Если у вас есть свой backend
        public static final String BASE_URL = "https://your-api.com/";
        public static final String API_VERSION = "v1";

        // Endpoints
        public static final String AUTH_ENDPOINT = BASE_URL + API_VERSION + "/auth";
        public static final String USERS_ENDPOINT = BASE_URL + API_VERSION + "/users";
    }

    // ==================== RATE LIMITS ====================

    /**
     * Ограничения частоты запросов
     */
    public static class RateLimits {
        // Максимальное количество сообщений в минуту
        public static final int MAX_MESSAGES_PER_MINUTE = 30;

        // Максимальное количество загрузок файлов в час
        public static final int MAX_FILE_UPLOADS_PER_HOUR = 10;

        // Минимальная задержка между сообщениями (мс)
        public static final long MIN_MESSAGE_DELAY_MS = 500;
    }

    // ==================== SECURITY ====================

    /**
     * Настройки безопасности
     */
    public static class Security {
        // Время жизни сессии (в миллисекундах)
        public static final long SESSION_TIMEOUT = 7 * 24 * 60 * 60 * 1000L; // 7 дней

        // Максимальное количество попыток входа
        public static final int MAX_LOGIN_ATTEMPTS = 5;

        // Время блокировки после превышения попыток (мс)
        public static final long LOGIN_LOCKOUT_DURATION = 15 * 60 * 1000L; // 15 минут
    }

    // ==================== HELPER METHODS ====================

    /**
     * Проверка, что все необходимые конфиги заполнены
     */
    public static boolean isConfigured() {
        return !CLOUDINARY_CLOUD_NAME.isEmpty()
                && !CLOUDINARY_API_KEY.isEmpty()
                && !CLOUDINARY_API_SECRET.isEmpty();
    }

    /**
     * Получить строку конфигурации Cloudinary для MediaManager
     */
    public static java.util.Map<String, Object> getCloudinaryConfig() {
        java.util.Map<String, Object> config = new java.util.HashMap<>();
        config.put("cloud_name", CLOUDINARY_CLOUD_NAME);
        config.put("api_key", CLOUDINARY_API_KEY);
        config.put("api_secret", CLOUDINARY_API_SECRET);
        return config;
    }

    /**
     * Проверка размера файла
     */
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

    /**
     * Получить максимальный размер для типа файла
     */
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

    /**
     * Форматирование размера файла для отображения
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}