package com.teiwm.lawnmowerremote;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import java.util.regex.Pattern;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener{
    private static final Pattern PARTIAl_IP_ADDRESS =
            Pattern.compile("^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"+
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])){0,1}$");

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case "key_settings_ip":
                if (!PARTIAl_IP_ADDRESS.matcher(newValue.toString()).matches()) {
                    ShowToast(getString(R.string.toast_text_wrong_ip));
                    return false;
                }
                break;
            case "key_settings_port":
                int portnumber = Integer.parseInt(newValue.toString());
                if( portnumber <= 1024 ) {
                    ShowToast(getString(R.string.toast_text_wrong_port));
                    return false;
                }
                break;
            default:
                if ( !preference.toString().equals("") )
                    preference.setSummary(newValue.toString());
                break;
        }


        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
        }
    }

    private void ShowToast(String str) {
        Context context = getApplicationContext();

        Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
        toast.show();
    }
}
