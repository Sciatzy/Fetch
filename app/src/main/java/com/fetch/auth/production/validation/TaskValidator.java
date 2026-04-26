package com.fetch.auth.production.validation;

import android.text.TextUtils;

import androidx.annotation.Nullable;

public final class TaskValidator {

    public static final int TITLE_MAX_LENGTH = 100;
    public static final int DESCRIPTION_MAX_LENGTH = 1000;

    private TaskValidator() {
        // Utility class.
    }

    public static String normalizeText(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    public static boolean isValidTitle(String title) {
        return !TextUtils.isEmpty(title) && title.length() <= TITLE_MAX_LENGTH;
    }

    public static boolean isValidDescription(String description) {
        return !TextUtils.isEmpty(description) && description.length() <= DESCRIPTION_MAX_LENGTH;
    }

    public static boolean isValidBudget(@Nullable Double budget) {
        return budget == null || budget >= 0;
    }
}

