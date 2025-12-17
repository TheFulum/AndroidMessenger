package com.example.messenger.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

import com.example.messenger.config.AppConfig;

/**
 * Поле ввода телефона с автоматическим форматированием
 * Формат: +375(XX) XXX-XX-XX
 */
public class PhoneMaskEditText extends AppCompatEditText {

    private static final String PREFIX = AppConfig.PhoneNumbers.DEFAULT_COUNTRY_CODE;
    private static final int MAX_DIGITS = AppConfig.PhoneNumbers.PHONE_DIGITS_LENGTH;
    private static final int GRAY_COLOR = Color.parseColor("#9E9E9E");

    private boolean isUpdating = false;

    public PhoneMaskEditText(Context context) {
        super(context);
        init();
    }

    public PhoneMaskEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhoneMaskEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setInputType(InputType.TYPE_CLASS_NUMBER);

        // Устанавливаем начальную маску
        updateMask("");

        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                isUpdating = true;

                // Извлекаем только цифры после префикса
                String text = s.toString();
                String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");

                // Ограничиваем до MAX_DIGITS цифр
                if (digits.length() > MAX_DIGITS) {
                    digits = digits.substring(0, MAX_DIGITS);
                }

                // Обновляем маску
                updateMask(digits);

                // Устанавливаем курсор в правильную позицию
                int newPosition = calculateCursorPosition(digits.length());
                setSelection(Math.min(newPosition, getText().length()));

                isUpdating = false;
            }
        });

        // Запрещаем перемещение курсора перед префиксом
        setOnClickListener(v -> {
            int position = getSelectionStart();
            if (position < PREFIX.length()) {
                setSelection(PREFIX.length());
            }
        });
    }

    /**
     * Обновляет отображаемую маску телефона
     * @param digits введенные цифры (без префикса)
     */
    private void updateMask(String digits) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Префикс +375 (всегда черный)
        builder.append(PREFIX);
        builder.setSpan(
                new ForegroundColorSpan(Color.BLACK),
                0,
                PREFIX.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // Код оператора (__)
        builder.append("(");
        appendDigitOrPlaceholder(builder, digits, 0);
        appendDigitOrPlaceholder(builder, digits, 1);
        builder.append(") ");

        // Первая группа (3 цифры)
        for (int i = 2; i < 5; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }
        builder.append("-");

        // Вторая группа (2 цифры)
        for (int i = 5; i < 7; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }
        builder.append("-");

        // Третья группа (2 цифры)
        for (int i = 7; i < 9; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }

        setText(builder);
    }

    /**
     * Добавляет цифру или placeholder (_) с правильным цветом
     */
    private void appendDigitOrPlaceholder(SpannableStringBuilder builder, String digits, int index) {
        if (digits.length() > index) {
            // Добавляем цифру (черный цвет)
            builder.append(digits.charAt(index));
            builder.setSpan(
                    new ForegroundColorSpan(Color.BLACK),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } else {
            // Добавляем placeholder (серый цвет)
            builder.append("_");
            builder.setSpan(
                    new ForegroundColorSpan(GRAY_COLOR),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    /**
     * Вычисляет позицию курсора на основе количества введенных цифр
     * Формат: +375(__) ___-__-__
     */
    private int calculateCursorPosition(int digitsCount) {
        if (digitsCount == 0) return PREFIX.length() + 1; // После (
        if (digitsCount == 1) return PREFIX.length() + 2; // После первой цифры
        if (digitsCount == 2) return PREFIX.length() + 5; // После ) и пробела
        if (digitsCount <= 5) return PREFIX.length() + 3 + digitsCount; // В первой группе
        if (digitsCount <= 7) return PREFIX.length() + 4 + digitsCount; // Во второй группе
        if (digitsCount <= 9) return PREFIX.length() + 5 + digitsCount; // В третьей группе

        return getText().length();
    }

    // ==================== PUBLIC API ====================

    /**
     * Получить номер телефона без форматирования
     * @return +375XXXXXXXXX
     */
    public String getUnformattedPhone() {
        String text = getText().toString();
        String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return PREFIX + digits;
    }

    /**
     * Получить форматированный номер телефона
     * @return +375(XX) XXX-XX-XX (без незаполненных _)
     */
    public String getFormattedPhone() {
        return getText().toString().replace("_", "");
    }

    /**
     * Проверка на полностью заполненный номер
     * @return true если введены все 9 цифр
     */
    public boolean isComplete() {
        String digits = getText().toString().substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return digits.length() == MAX_DIGITS;
    }

    /**
     * Установить номер телефона программно
     * @param phone номер в любом формате (+375XXXXXXXXX, 375XXXXXXXXX, XXXXXXXXX)
     */
    public void setPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            updateMask("");
            return;
        }

        // Извлекаем цифры
        String digits = phone.replaceAll("[^0-9]", "");

        // Убираем 375 если есть
        if (digits.startsWith("375")) {
            digits = digits.substring(3);
        }

        // Ограничиваем до MAX_DIGITS цифр
        if (digits.length() > MAX_DIGITS) {
            digits = digits.substring(0, MAX_DIGITS);
        }

        isUpdating = true;
        updateMask(digits);
        int newPosition = calculateCursorPosition(digits.length());
        setSelection(Math.min(newPosition, getText().length()));
        isUpdating = false;
    }

    /**
     * Очистить поле
     */
    public void clear() {
        setPhone("");
    }
}