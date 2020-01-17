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
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.Switch;;
import android.widget.TextView;
import android.widget.Toast;

import com.jaygoo.widget.RangeSeekBar;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private static String LOG_TAG = "RPI client app";
    private TCPClient m_client = new TCPClient();
    private CommManager m_mower = new CommManager();

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

    private Switch m_btn_switch_cut;
    private JoystickView m_jstck_move_vehicle;
    private RangeSeekBar m_blade_height;
    private ActionBar m_ActionBar;
    private TextView m_seek_description;

    private BroadcastReceiver m_WifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            int WifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            switch ( WifiStateExtra ) {
                case WifiManager.WIFI_STATE_ENABLED:
                    DetectConnectivity ConnectRunnable = new DetectConnectivity();
                    Thread DetectThread = new Thread(ConnectRunnable);
                    DetectThread.start();
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    if (m_client.isConnected()) {
                        m_client.Disconnect();
                    }
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    ShowMessageBox(getString(R.string.msg_wifi_not_connected));
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

        m_btn_switch_cut = findViewById(R.id.button_switch_cut);
        m_jstck_move_vehicle = findViewById( R.id.joystickView);

        m_ActionBar = getSupportActionBar();
        m_blade_height = findViewById( R.id.seekbar_set_height);
        m_seek_description = findViewById( R.id.seek_bar_description );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide( 100 );
    }

    public void PostToast(final String message) {
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

        m_jstck_move_vehicle.setOnMoveListener(new JoystickView.OnMoveListener()
        {
            public void onMove( int angle, int strength ) {
                if ( !m_jstck_move_vehicle.isEnabled() )
                    return;
                m_mower.Move( angle, strength);
            }
        }, 200);

        m_btn_switch_cut.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                m_mower.RunBlade( isChecked );
            }
        });
        m_blade_height.setIndicatorTextDecimalFormat("0");

        SetEnableControls(false);
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

        EventDispatcher.getInstance().addEventListener(
                Local_event_ids.tcp_event_ids.connected.getID(),
                new IEventHandler() {
                    @Override
                    public void callback(Event event) {
                            SetEnableControls( true );
                            PostToast( getString( R.string.msg_client_connected ) );
                    }
                });

        EventDispatcher.getInstance().addEventListener(
                Local_event_ids.tcp_event_ids.disconnected.getID(),
                new IEventHandler() {
                    @Override
                    public void callback(Event event) {
                        SetEnableControls( false );
                        if ( event.getParams() != null )
                            PostToast( event.getParams().toString() );
                        else
                            PostToast( getString( R.string.msg_client_disconnected ) );
                    }
                });
        IntentFilter intentFilter = new IntentFilter( WifiManager.WIFI_STATE_CHANGED_ACTION );
        registerReceiver( m_WifiStateReceiver, intentFilter);

        // TODO: Add functionality to read settings value for enabling video feed.

        SharedPreferences shared_pref = getSharedPreferences(getPackageName() +
                "_preferences", MODE_PRIVATE );

        if ( shared_pref.getBoolean("key_settings_mirror_controls", false) ) {
            ((FrameLayout.LayoutParams) m_btn_switch_cut.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) m_blade_height.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) m_seek_description.getLayoutParams()).gravity =
                    ( Gravity.TOP | Gravity.START );

            ((FrameLayout.LayoutParams) m_jstck_move_vehicle.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );

        } else {
            ((FrameLayout.LayoutParams) m_jstck_move_vehicle.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) m_btn_switch_cut.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );

            ((FrameLayout.LayoutParams) m_blade_height.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );
            ((FrameLayout.LayoutParams) m_seek_description.getLayoutParams()).gravity =
                    ( Gravity.TOP | Gravity.END );
        }

        //m_jstck_move_vehicle.setLayoutParams( joystickparams );
       // m_btn_switch_cut.setLayoutParams( togglebtnparams );
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(m_WifiStateReceiver);
        EventDispatcher.getInstance().removeAllListeners();

        if ( m_client.isConnected() )
            m_client.Disconnect();
    }

    @Override
    protected void onDestroy() {
        if ( m_client.isConnected() )
            m_client.Disconnect();

        super.onDestroy();
    }

   private void SetEnableControls(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( enabled ) {
                    m_btn_switch_cut.setEnabled( true );
                    m_jstck_move_vehicle.setEnabled( true );
                    m_jstck_move_vehicle.setButtonColor(
                            getResources().getColor( R.color.colorControlEnabled ) );
                    m_jstck_move_vehicle.setBorderColor(
                            getResources().getColor( R.color.colorControlEnabled ) );
                    m_blade_height.setEnabled( true );

                } else {
                    m_btn_switch_cut.setChecked( false );
                    m_btn_switch_cut.setEnabled( false );
                    m_jstck_move_vehicle.setEnabled( false );
                    m_jstck_move_vehicle.setButtonColor(
                            getResources().getColor( R.color.colorControlDisabled ) );
                    m_jstck_move_vehicle.setBorderColor(
                            getResources().getColor( R.color.colorControlDisabled ) );
                    m_blade_height.setEnabled( false );
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

    public class DetectConnectivity implements Runnable {

        ConnectivityManager cm
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        SharedPreferences shared_pref = getSharedPreferences(getPackageName() +
                "_preferences", MODE_PRIVATE );
        final String ip = shared_pref.getString("key_settings_ip",
                "@string/pref_default_ip_value");
        int port;

        public DetectConnectivity() {
            try {
                port = Integer.parseInt(shared_pref.getString("key_settings_port",
                        "@string/pref_default_port_value"));
                if (port == 0)
                    port = 8080;

            } catch (NumberFormatException nfe) {
                ShowToast(getString(R.string.msg_wrong_wifi_port));
                port = 8080;
            }
        }

        @Override
        public void run() {
            boolean connected = false;
            int tries = 0;
            while ( !connected && ( tries <= 4 ) ) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                connected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

                if ( connected ) {
                     m_client.Connect( ip, port );
                }
                try {
                    Thread.sleep( 2000 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace( System.err );
                }
                tries++;
            }
            if ( !connected ) {
                ShowMessageBox( getString( R.string.msg_wifi_not_connected ) );
            }
        }
    }


}

