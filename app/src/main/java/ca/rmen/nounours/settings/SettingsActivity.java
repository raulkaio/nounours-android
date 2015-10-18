/*
 *   Copyright (c) 2009 - 2015 Carmen Alvarez
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

package ca.rmen.nounours.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import ca.rmen.nounours.R;
import ca.rmen.nounours.compat.ActivityCompat;
import ca.rmen.nounours.compat.ApiHelper;


/**
 * A {@link PreferenceActivity} that presents a set of application app_settings. On
 * handset devices, app_settings are presented as a single list. On tablets,
 * app_settings are split by category, with category headers shown to the left of
 * the list of app_settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {

    private static final String EXTRA_PREFERENCE_XML_RES_ID = "nounours_preference_xml_res_id";
    private static final String PREF_LAUNCH_WALLPAPER_SETTINGS = "launch_wallpaper_settings";

    public static void startAppSettingsActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(EXTRA_PREFERENCE_XML_RES_ID, R.xml.app_settings);
        context.startActivity(intent);
    }

    public static void startLwpSettingsActivity(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        intent.putExtra(EXTRA_PREFERENCE_XML_RES_ID, R.xml.lwp_settings);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.setDisplayHomeAsUpEnabled(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        int xmlResId = getIntent().getIntExtra(EXTRA_PREFERENCE_XML_RES_ID, R.xml.lwp_settings);

        //noinspection deprecation
        addPreferencesFromResource(xmlResId);

        List<Preference> preferencesToHide = new ArrayList<>();
        // Some preferences are not relevant, and we must remove them.
        //noinspection deprecation
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (preference instanceof ListPreference) {
                // If we have only one theme, there's no point in showing the theme preference.
                ListPreference listPreference = (ListPreference) preference;
                if (listPreference.getEntries().length == 1) {
                    preferencesToHide.add(preference);
                }
                bindPreferenceSummaryToValue(preference);
            }
            // The wallpaper feature isn't available on older devices.
            else if (PREF_LAUNCH_WALLPAPER_SETTINGS.equals(preference.getKey())) {
                if (ApiHelper.getAPILevel() < Build.VERSION_CODES.ECLAIR_MR1) {
                    preferencesToHide.add(preference);
                }
            }
        }
        for (Preference preference : preferencesToHide) {
            preferenceScreen.removePreference(preference);
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
