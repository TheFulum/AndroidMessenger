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

                String text = s.toString();
                String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");

                if (digits.length() > MAX_DIGITS) {
                    digits = digits.substring(0, MAX_DIGITS);
                }

                updateMask(digits);
                int newPosition = calculateCursorPosition(digits.length());
                setSelection(Math.min(newPosition, getText().length()));

                isUpdating = false;
            }
        });

        setOnClickListener(v -> {
            int position = getSelectionStart();
            if (position < PREFIX.length()) {
                setSelection(PREFIX.length());
            }
        });
    }

    private void updateMask(String digits) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        builder.append(PREFIX);
        builder.setSpan(
                new ForegroundColorSpan(Color.BLACK),
                0,
                PREFIX.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        builder.append("(");
        appendDigitOrPlaceholder(builder, digits, 0);
        appendDigitOrPlaceholder(builder, digits, 1);
        builder.append(") ");

        for (int i = 2; i < 5; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }
        builder.append("-");

        for (int i = 5; i < 7; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }
        builder.append("-");

        for (int i = 7; i < 9; i++) {
            appendDigitOrPlaceholder(builder, digits, i);
        }

        setText(builder);
    }

    private void appendDigitOrPlaceholder(SpannableStringBuilder builder, String digits, int index) {
        if (digits.length() > index) {
            builder.append(digits.charAt(index));
            builder.setSpan(
                    new ForegroundColorSpan(Color.BLACK),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        } else {
            builder.append("_");
            builder.setSpan(
                    new ForegroundColorSpan(GRAY_COLOR),
                    builder.length() - 1,
                    builder.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }

    private int calculateCursorPosition(int digitsCount) {
        if (digitsCount == 0) return PREFIX.length() + 1;
        if (digitsCount == 1) return PREFIX.length() + 2;
        if (digitsCount == 2) return PREFIX.length() + 5;
        if (digitsCount <= 5) return PREFIX.length() + 3 + digitsCount;
        if (digitsCount <= 7) return PREFIX.length() + 4 + digitsCount;
        if (digitsCount <= 9) return PREFIX.length() + 5 + digitsCount;

        return getText().length();
    }

    public String getUnformattedPhone() {
        String text = getText().toString();
        String digits = text.substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return PREFIX + digits;
    }

    public String getFormattedPhone() {
        return getText().toString().replace("_", "");
    }

    public boolean isComplete() {
        String digits = getText().toString().substring(PREFIX.length()).replaceAll("[^0-9]", "");
        return digits.length() == MAX_DIGITS;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            updateMask("");
            return;
        }

        String digits = phone.replaceAll("[^0-9]", "");

        if (digits.startsWith("375")) {
            digits = digits.substring(3);
        }

        if (digits.length() > MAX_DIGITS) {
            digits = digits.substring(0, MAX_DIGITS);
        }

        isUpdating = true;
        updateMask(digits);
        int newPosition = calculateCursorPosition(digits.length());
        setSelection(Math.min(newPosition, getText().length()));
        isUpdating = false;
    }

    public void clear() {
        setPhone("");
    }
}