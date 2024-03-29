package com.teiwm.lawn_mower_remote;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.appcompat.app.AppCompatActivity;

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
            try {
                setPreferencesFromResource( R.xml.pref_general, rootKey );
                final EditTextPreference ipPreference =
                        findPreference( getText( R.string.pref_ip_key ) );
                final EditTextPreference portPreference =
                        findPreference( getText( R.string.pref_port_key ) );
                final EditTextPreference rpmSpeed = findPreference( getText( R.string.pref_blade_speed_key ) );
                ipPreference.setSummary( ipPreference.getText() );
                portPreference.setSummary( portPreference.getText() );
                rpmSpeed.setSummary( rpmSpeed.getText() + " RPM" );

                ipPreference.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        ipPreference.setSummary( newVal.toString() );
                        return true;
                    }
                });

                portPreference.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newVal) {
                        portPreference.setSummary( newVal.toString() );
                        return true;
                    }
                });

                rpmSpeed.setOnPreferenceChangeListener( new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange( Preference preference, Object newValue ) {
                        int val = Integer.parseInt( newValue.toString() );
                        if ( val > 2500 || val < 500 ) {
                           Toast.makeText( getActivity().getBaseContext(),
                                   getString( R.string.msg_wrong_max_speed ),
                                   Toast.LENGTH_LONG ).show();
                           return false;
                        }
                        rpmSpeed.setSummary( newValue.toString() + " RPM" );
                        return true;
                    }
                } );

            } catch ( NullPointerException e ) {
                Log.d( "Settings activity", e.getMessage() );
            }
        }
    }
}
