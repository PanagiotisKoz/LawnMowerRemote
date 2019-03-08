package com.teiwm.lawnmowerremote;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private TCPClient m_Client;
    private int m_joy_angle = 0;
    private int m_joy_strength = 0;
    private int m_max_cutting_speed = 80;
    private boolean m_cutting_active = false;
    private BroadcastReceiver m_ConnectivityReceiver = null;

    private Button m_btn_reset_joy = null;
    private ToggleButton m_btn_toggle_cut = null;
    private JoystickView m_jstck_move_vihicle = null;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private View mContentView;
    private boolean mVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);


        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        Initialize();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //some code....
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(FullscreenActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean CheckWifiState() {
        NetworkInfo mWifi;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if ( connManager != null )
            mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        else return false;

        return mWifi.isConnected();
    }

    protected void Initialize() {

        m_jstck_move_vihicle = (JoystickView) findViewById(R.id.joystickView_Left);
        m_jstck_move_vihicle.setOnMoveListener(new JoystickView.OnMoveListener()
        {
            @Override
            public void onMove(int angle, int strength) {
                m_joy_angle = angle;

                if (angle > 180)
                    m_joy_strength = -strength;
                else
                    m_joy_strength = strength;

                if (m_cutting_active && m_joy_strength > m_max_cutting_speed )
                    m_joy_strength = m_max_cutting_speed;
            }
        });

        m_btn_toggle_cut = (ToggleButton) findViewById(R.id.button_toggle_cut);
        m_btn_toggle_cut.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                m_cutting_active = isChecked;
            }
        });

        m_btn_reset_joy = (Button) findViewById(R.id.button_reset_joy);
        m_btn_reset_joy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                m_jstck_move_vihicle.resetButtonPosition();
                m_jstck_move_vihicle.invalidate();
                m_joy_angle = 0;
                m_joy_strength = 0;
            }
        });

        m_ConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                CheckWifiState();
            }
        };
        this.registerReceiver(this.m_ConnectivityReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPref = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        // TODO: Add functionality to read settings value for enabling video feed.

        if ( CheckWifiState() ) {
            ShowToast(getString(R.string.msg_wifi_connected));
            String ip = sharedPref.getString("key_settings_ip", "127.0.0.1");
            int port = 0;
            try {
                port = Integer.parseInt(sharedPref.getString("key_settings_port", "@string/pref_default_port_value"));
            } catch (NumberFormatException nfe) {
                port = 8080;
            }

            m_Client = new TCPClient();
            m_Client.Connect(ip, port);
        }
        else {
            ShowToast(getString(R.string.msg_wifi_not_connected));
        }


        FrameLayout.LayoutParams joystickparams = (FrameLayout.LayoutParams) m_jstck_move_vihicle.getLayoutParams();
        FrameLayout.LayoutParams togglebtnparams = (FrameLayout.LayoutParams) m_btn_toggle_cut.getLayoutParams();


        if (sharedPref.getBoolean("key_settings_mirror_controls", false)) {
            joystickparams.gravity = (Gravity.BOTTOM | Gravity.END);
            togglebtnparams.gravity = (Gravity.BOTTOM | Gravity.START);
        } else {
            joystickparams.gravity = (Gravity.BOTTOM | Gravity.START);
            togglebtnparams.gravity = (Gravity.BOTTOM | Gravity.END);
        }

        m_jstck_move_vihicle.setLayoutParams(joystickparams);
        m_btn_toggle_cut.setLayoutParams(togglebtnparams);



        if (!m_Client.isConnected()) {
            SetEnableControls(false); // Disable controls when raspberry not found.
            ShowMessageBox(getString(R.string.msg_server_not_found));
        }
        else {
            SetEnableControls(true); //  Enable controls when raspberry found.
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        m_Client.Disconnect();
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(m_ConnectivityReceiver);
        m_Client.Disconnect();
        super.onDestroy();
    }

    private void SetEnableControls(boolean state) {
        if (state){
            m_btn_reset_joy.setEnabled(true);
            m_btn_toggle_cut.setEnabled(true);
            m_jstck_move_vihicle.setEnabled(true);
        }
        else {
            m_btn_reset_joy.setEnabled(false);
            m_btn_toggle_cut.setEnabled(false);
            m_jstck_move_vihicle.setEnabled(false);
        }

    }

    private void ShowToast(String str) {
        Context context = getApplicationContext();

        Toast toast = Toast.makeText(context, str, Toast.LENGTH_LONG);
        toast.show();
    }

    private void ShowMessageBox(String str) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Add the buttons
        builder.setPositiveButton(R.string.msg_box_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setMessage(str);
        dialog.show();
    }
}

