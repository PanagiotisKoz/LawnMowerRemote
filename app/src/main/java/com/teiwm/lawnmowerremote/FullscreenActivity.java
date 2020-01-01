package com.teiwm.lawnmowerremote;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
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
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private TCPClient m_client = new TCPClient();

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

    private ScrollView m_infoScroll;
    private Switch m_btn_switch_cut;
    private JoystickView m_jstck_move_vihicle;

    private ActionBar m_ActionBar;
    private TextView m_infoView;

    @Override
    protected void onStop() {
        super.onStop();
    }

    private BroadcastReceiver m_WifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            int WifiStateExtra = intent.getIntExtra( WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN );

            SharedPreferences shared_pref = getSharedPreferences(getPackageName() +
                    "_preferences", MODE_PRIVATE );

            switch ( WifiStateExtra ) {
                case WifiManager.WIFI_STATE_ENABLED:
                    final ConnectivityManager cm =
                            (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

                    final String ip = shared_pref.getString("key_settings_ip",
                            "@string/pref_default_ip_value");
                    int port;
                    try {
                        port = Integer.parseInt(shared_pref.getString("key_settings_port",
                                "@string/pref_default_port_value"));
                        if ( port == 0 )
                            port = 8080;

                    } catch (NumberFormatException nfe) {
                        ShowToast(getString(R.string.msg_wrong_wifi_port));
                        port = 8080;
                    }

                    final int final_port = port;
                    // Need to wait a bit for the SSID to get picked up;
                    // if done immediately all we'll get is null
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                            boolean isConnected = activeNetwork != null &&
                                    activeNetwork.isConnectedOrConnecting();
                            if ( isConnected ) {
                                m_client.Connect(ip, final_port);
                            }
                            else
                                ShowMessageBox( getString( R.string.msg_wifi_not_connected ) );
                        }
                    }, 10000);
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    ShowMessageBox( getString( R.string.msg_wifi_not_connected ) );
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    if ( m_client.isConnected() )
                        m_client.Disconnect();
                    break;
            }
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_fullscreen );

        mContentView = findViewById( R.id.fullscreen_content );
        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                toggle();
            }
        });

        m_infoScroll = findViewById( R.id.infoScroll );
        m_btn_switch_cut = findViewById(R.id.button_switch_cut);
        m_jstck_move_vihicle = findViewById( R.id.joystickView_Left );

        m_ActionBar = getSupportActionBar();
        m_infoView = findViewById( R.id.infoView );
        SetEnableControls(false);
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide( 100 );
    }

    public void Post_Toast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShowToast( message );
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        m_jstck_move_vihicle.setOnMoveListener(new JoystickView.OnMoveListener()
        {
            public void onMove( int angle, int strength ) {

                String move_dir = getString( R.string.move_cmd_forward );

                if ( angle >= 337 || angle <= 22 )
                    move_dir = getString( R.string.move_cmd_right );
                if ( angle > 22 && angle <= 67 )
                    move_dir = getString( R.string.move_cmd_fr );
                if ( angle > 67 && angle <= 112 )
                    move_dir = getString( R.string.move_cmd_forward );
                if ( angle > 112 && angle <= 157 )
                    move_dir = getString( R.string.move_cmd_fl );
                if ( angle > 157 && angle <= 202 )
                    move_dir = getString( R.string.move_cmd_left );
                if ( angle > 202 && angle <= 247 )
                    move_dir = getString( R.string.move_cmd_bl );
                if ( angle > 247 && angle <= 292 )
                    move_dir = getString( R.string.move_cmd_backward );
                if ( angle > 292 && angle <= 337 )
                    move_dir = getString( R.string.move_cmd_br );

                String data =  move_dir;
                data += Byte.toString( ( byte ) strength );
                m_client.WriteData( data.getBytes() );
            }
        }, 500);

        m_client.setOnConnectListener( new TCPClient.OnConnectListener() {
            @Override
            public void onConnect(String error) {
                if ( error.isEmpty() ) {
                    SetEnableControls(true); //  Enable controls when raspberry found.
                    Post_Toast( getString(R.string.msg_client_connected) );
                }
                else {
                    SetEnableControls(false); // Disable controls when raspberry not found.
                    Post_Toast( error );
                }
            }
        });

        m_client.setOnDisconnectListener(new TCPClient.OnDisconnectListener() {
            @Override
            public void onDisconnect(String error) {
                SetEnableControls(false); // Disable controls when raspberry not found.

                if ( error.isEmpty() ) {
                    Post_Toast(getString(R.string.msg_server_disconnected));
                }
                else
                    Post_Toast( error );
            }
        });

        m_btn_switch_cut.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                String data =  getString( R.string.property_blade_run );
                data += Boolean.toString( isChecked );
                m_client.WriteData( data.getBytes() );
            }
        });
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch( View view, MotionEvent motionEvent ) {
            switch ( motionEvent.getAction() ) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            if ( AUTO_HIDE ) {
                delayedHide( AUTO_HIDE_DELAY_MILLIS );
            }
            return false;
        }
    };

    private void toggle() {
        if ( m_ActionBar.isShowing() ) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        if ( m_ActionBar != null ) {
            m_ActionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks( mShowPart2Runnable );
        mHideHandler.postDelayed( mHidePart2Runnable, UI_ANIMATION_DELAY );
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint( "InlinedApi" )
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
        mContentView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION );

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks( mHidePart2Runnable );
        mHideHandler.postDelayed( mShowPart2Runnable, UI_ANIMATION_DELAY );
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if ( actionBar != null ) {
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
    private void delayedHide( int delayMillis ) {
        mHideHandler.removeCallbacks( mHideRunnable );
        mHideHandler.postDelayed( mHideRunnable, delayMillis );
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate( R.menu.main_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected( MenuItem item ) {
        int id = item.getItemId();
        if ( id == R.id.action_settings ) {
            Intent intent = new Intent(FullscreenActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter( WifiManager.WIFI_STATE_CHANGED_ACTION );
        registerReceiver( m_WifiStateReceiver, intentFilter);

        // TODO: Add functionality to read settings value for enabling video feed.

        FrameLayout.LayoutParams joystickparams = (FrameLayout.LayoutParams) m_jstck_move_vihicle.getLayoutParams();
        FrameLayout.LayoutParams togglebtnparams = (FrameLayout.LayoutParams) m_btn_switch_cut.getLayoutParams();

        SharedPreferences shared_pref = getSharedPreferences(getPackageName() +
                "_preferences", MODE_PRIVATE );

        if ( shared_pref.getBoolean("key_settings_mirror_controls", false) ) {
            joystickparams.gravity = ( Gravity.BOTTOM | Gravity.END );
            togglebtnparams.gravity = ( Gravity.BOTTOM | Gravity.START );
        } else {
            joystickparams.gravity = ( Gravity.BOTTOM | Gravity.START );
            togglebtnparams.gravity = ( Gravity.BOTTOM | Gravity.END );
        }

        m_jstck_move_vihicle.setLayoutParams( joystickparams );
        m_btn_switch_cut.setLayoutParams( togglebtnparams );
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(m_WifiStateReceiver);
    }

    @Override
    protected void onDestroy() {
        if ( m_client.isConnected() )
            m_client.Disconnect();

        super.onDestroy();
    }

    private void SetEnableControls(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( state ) {
                    m_btn_switch_cut.setEnabled(true);
                    m_jstck_move_vihicle.setEnabled(true);
                } else {
                    m_btn_switch_cut.setEnabled(false);
                    m_jstck_move_vihicle.setEnabled(false);
                }
            }
        });
    }

    private void ShowToast( String str ) {
        Context context = getApplicationContext();

        Toast toast = Toast.makeText( context, str, Toast.LENGTH_LONG );
        toast.show();
    }

    private void ShowMessageBox( String str ) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this );

        // Add the buttons
        builder.setPositiveButton( R.string.msg_box_ok, new DialogInterface.OnClickListener() {
            public void onClick( DialogInterface dialog, int id ) {
                dialog.dismiss();
            }
        });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setMessage( str );
        dialog.show();
    }

}

