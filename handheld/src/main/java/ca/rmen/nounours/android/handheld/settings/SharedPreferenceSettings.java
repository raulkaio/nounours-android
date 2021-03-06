/*
 *   Copyright (c) 2009 - 2016 Carmen Alvarez
 *
 *   This file is part of Nounours for Android.
 *
 *   Nounours for Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nounours for Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nounours for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.nounours.android.handheld.settings;

import android.content.Context;
import android.preference.PreferenceManager;

import ca.rmen.nounours.R;
import ca.rmen.nounours.android.common.compat.ResourcesCompat;
import ca.rmen.nounours.android.common.settings.NounoursSettings;

public final class SharedPreferenceSettings implements NounoursSettings {
    static final String PREF_THEME = "Theme";
    static final String PREF_BACKGROUND_COLOR = "BackgroundColor";
    private static final String PREF_SOUND_AND_VIBRATE = "SoundAndVibrate";
    private static final String PREF_DIM = "nounourslwp_dim";
    private static final String PREF_GRAYSCALE = "grayscale";
    private static final String PREFIX_APP = "app_";
    private static final String PREFIX_LWP = "lwp_";
    private static final String PREFIX_DREAM = "dream_";
    // IdleTimeout changed from 1.3.5 to 2.0.0 from a Long to a String
    // We just rename the preference here and don't care about migrating this setting.
    private static final String PREF_IDLE_TIMEOUT = "IdleTimeout2";

    private final Context mContext;
    // To have different app_settings for the app vs lwp, we prefix the app_settings:
    private final String mPreferencePrefix;
    private final String mDefaultThemeId;

    public static NounoursSettings getAppSettings(Context context) {
        return new SharedPreferenceSettings(context, PREFIX_APP, context.getString(R.string.DEFAULT_APP_THEME_ID));
    }

    public static NounoursSettings getLwpSettings(Context context) {
        return new SharedPreferenceSettings(context, PREFIX_LWP, context.getString(R.string.DEFAULT_LWP_THEME_ID));
    }

    public static NounoursSettings getDreamSettings(Context context) {
        return new SharedPreferenceSettings(context, PREFIX_DREAM, context.getString(R.string.DEFAULT_LWP_THEME_ID));
    }

    private SharedPreferenceSettings(Context context, String preferencePrefix, String defaultThemeId) {
        mContext = context;
        mPreferencePrefix = preferencePrefix;
        mDefaultThemeId = defaultThemeId;
    }

    @Override
    public boolean isSoundEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mPreferencePrefix + PREF_SOUND_AND_VIBRATE, true);
    }

    @Override
    @SuppressWarnings("SameParameterValue")
    public void setEnableSound(boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putBoolean(mPreferencePrefix + PREF_SOUND_AND_VIBRATE, enabled).commit();
    }

    @Override
    public boolean isImageDimmed() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mPreferencePrefix + PREF_DIM, false);
    }

    @Override
    public boolean isGrayscale() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(mPreferencePrefix + PREF_GRAYSCALE, false);
    }

    @Override
    public long getIdleTimeout() {
        return Long.valueOf(PreferenceManager.getDefaultSharedPreferences(mContext).getString(mPreferencePrefix + PREF_IDLE_TIMEOUT, "30000"));
    }

    @Override
    public String getThemeId() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getString(mPreferencePrefix + PREF_THEME, mDefaultThemeId);
    }

    @Override
    public int getBackgroundColor() {
        return PreferenceManager.getDefaultSharedPreferences(mContext).getInt(mPreferencePrefix + PREF_BACKGROUND_COLOR, ResourcesCompat.getColor(mContext, android.R.color.black));
    }

}
