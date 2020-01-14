package com.teiwm.lawnmowerremote;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * A {@link AppCompatActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,
                new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences( Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource( R.xml.pref_general, rootKey );
            EditTextPreference ipPreference =
                    (EditTextPreference) findPreference("key_settings_ip");
            EditTextPreference portPreference =
                    (EditTextPreference) findPreference("key_settings_port");
            ipPreference.setSummary( ipPreference.getText() );
            portPreference.setSummary( portPreference.getText() );
        }
    }
}
