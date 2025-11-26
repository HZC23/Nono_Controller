package com.hzc.nonocontroller.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {

    private static final String PREFS_NAME = "NoNoPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_INVERT_LAYOUT = "invert_layout";

    private final SharedPreferences sharedPreferences;

    public SettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setDarkMode(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false); // Default to false (light mode)
    }

    public void setInvertLayout(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_INVERT_LAYOUT, enabled).apply();
    }

    public boolean isInvertLayout() {
        return sharedPreferences.getBoolean(KEY_INVERT_LAYOUT, false); // Default to not inverted
    }
}
