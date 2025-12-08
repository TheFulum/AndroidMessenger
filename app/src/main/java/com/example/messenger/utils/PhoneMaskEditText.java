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

public class PhoneMaskEditText extends AppCompatEditText {

    private static final String MASK = "+375(__) ___-__-__";
    private static final String PREFIX = "+375";

    private boolean isUpdating = false;
    private int cursorPosition = PREFIX.length();

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

                // Извлекаем только цифры после +375
                String text = s.toString();
                String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");

                // Ограничиваем до 9 цифр (код оператора + номер)
                if (digits.length() > 9) {
                    digits = digits.substring(0, 9);
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
        if (digits.length() >= 1) {
            builder.append(digits.charAt(0));
            builder.setSpan(
                    new ForegroundColorSpan(Color.BLACK),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } else {
            builder.append("_");
            builder.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#9E9E9E")), // grey-500
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        if (digits.length() >= 2) {
            builder.append(digits.charAt(1));
            builder.setSpan(
                    new ForegroundColorSpan(Color.BLACK),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } else {
            builder.append("_");
            builder.setSpan(
                    new ForegroundColorSpan(Color.parseColor("#9E9E9E")),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        builder.append(") ");

        // Первая группа (3 цифры)
        for (int i = 2; i < 5; i++) {
            if (digits.length() > i) {
                builder.append(digits.charAt(i));
                builder.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                builder.append("_");
                builder.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#9E9E9E")),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        builder.append("-");

        // Вторая группа (2 цифры)
        for (int i = 5; i < 7; i++) {
            if (digits.length() > i) {
                builder.append(digits.charAt(i));
                builder.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                builder.append("_");
                builder.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#9E9E9E")),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
        builder.append("-");

        // Третья группа (2 цифры)
        for (int i = 7; i < 9; i++) {
            if (digits.length() > i) {
                builder.append(digits.charAt(i));
                builder.setSpan(
                        new ForegroundColorSpan(Color.BLACK),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            } else {
                builder.append("_");
                builder.setSpan(
                        new ForegroundColorSpan(Color.parseColor("#9E9E9E")),
                        builder.length() - 1,
                        builder.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        setText(builder);
    }

    private int calculateCursorPosition(int digitsCount) {
        // +375(__) ___-__-__
        // 0123456789...

        if (digitsCount == 0) return PREFIX.length() + 1; // После (
        if (digitsCount == 1) return PREFIX.length() + 2; // После первой цифры
        if (digitsCount == 2) return PREFIX.length() + 5; // После )
        if (digitsCount <= 5) return PREFIX.length() + 3 + digitsCount; // В первой группе
        if (digitsCount <= 7) return PREFIX.length() + 4 + digitsCount; // Во второй группе
        if (digitsCount <= 9) return PREFIX.length() + 5 + digitsCount; // В третьей группе

        return getText().length();
    }

    /**
     * Получить номер телефона без форматирования (только +375XXXXXXXXX)
     */
    public String getUnformattedPhone() {
        String text = getText().toString();
        String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return PREFIX + digits;
    }

    /**
     * Получить форматированный номер телефона (+375(XX) XXX-XX-XX)
     */
    public String getFormattedPhone() {
        return getText().toString().replace("_", "");
    }

    /**
     * Проверка на полностью заполненный номер
     */
    public boolean isComplete() {
        String digits = getText().toString().substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return digits.length() == 9;
    }

    /**
     * Установить номер телефона программно
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

        // Ограничиваем до 9 цифр
        if (digits.length() > 9) {
            digits = digits.substring(0, 9);
        }

        isUpdating = true;
        updateMask(digits);
        int newPosition = calculateCursorPosition(digits.length());
        setSelection(Math.min(newPosition, getText().length()));
        isUpdating = false;
    }
}