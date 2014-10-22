package com.hiit.steps;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {
    public static class SettingsFragment extends PreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        public static final String KEY_PREF_WEBSOCKETURL = "pref_webSocketUrl";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences sharedPreferences =
                    getPreferenceScreen().getSharedPreferences();
            updateSummary(sharedPreferences, KEY_PREF_WEBSOCKETURL);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(
                SharedPreferences sharedPreferences, String key) {
            updateSummary(sharedPreferences, key);
        }

        private void updateSummary(SharedPreferences sharedPreferences,
                                   String key) {
            if (key.equals(KEY_PREF_WEBSOCKETURL)) {
                Preference preference = findPreference(key);
                String value = sharedPreferences.getString(key, "");
                // Set summary to be the user-description for the selected value
                preference.setSummary(value);
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
