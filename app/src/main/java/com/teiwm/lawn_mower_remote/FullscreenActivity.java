package com.teiwm.lawn_mower_remote;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.github.controlwear.virtual.joystick.android.JoystickView;

/**
 * An full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    private final String LOG_TAG = "Mower client activity";

    /**
     * Battery update status intervals in seconds.
     */
    private final int BATT_UPDATE_INTERVALL = 1;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private final int UI_ANIMATION_DELAY = 300;

    private View mContentView;
    private Switch mBtnSwitchCut;
    private JoystickView mJstckMoveVehicle;
    private RangeSeekBar mBladeHeight;
    private ActionBar mActionBar;
    private TextView mSeekDescription;
    private ImageView mBattIcon;
    private TextView mBattPwrValue;
    private float mLastBladeHeight = 0;
    private boolean mLastSwitchValue = false;

    private Timer mScheduler = new Timer( false );
    private TimerTask mUpdateBattValue = new TimerTask() {
        @Override
        public void run() {
            EventDispatcher.getInstance().riseEvent(
                    new EventGetProperty( EventGetProperty.Properties.batt_percentage ) );
        }
    };

    private final IEventHandler mOnConnected =  new IEventHandler() {
        @Override
        public void callback(Event event) {
            setEnableControls( true );
            mScheduler.schedule( mUpdateBattValue, 0, TimeUnit.SECONDS.toMillis( BATT_UPDATE_INTERVALL ) );
        }
    };

    private final IEventHandler mOnDisconnected = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            resetControls();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBattIcon.setImageResource( R.drawable.ic_battery_unknown_black_24dp );
                    mBattPwrValue.setText( getString( R.string.msg_batt_unknow ) );
                }
            });
            setEnableControls( false );
            mScheduler.cancel();
        }
    };

    private final IEventHandler mOnLowBatt = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            resetControls();
            setEnableControls( false );
            mScheduler.cancel();

            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    mBattIcon.setImageResource( R.drawable.ic_battery_alert_red_24dp );
                    mBattPwrValue.setText( getString( R.string.msg_low_batt ) );
                }
            });
        }
    };

    private final IEventHandler mOnCharge = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            resetControls();
            setEnableControls( false );
            mScheduler.cancel();

            runOnUiThread( new Runnable() {
                @Override
                public void run() {
                    mBattIcon.setImageResource( R.drawable.ic_battery_charging_full_black_24dp );
                    mBattPwrValue.setText( getString( R.string.msg_batt_charging ) );
                }
            });
        }
    };

    private final IEventHandler mOnPropertyReturn = new IEventHandler() {
        @Override
        public void callback(Event event) {
            final EventPropertyReturn evnt = ( EventPropertyReturn ) event;

            switch ( evnt.getProperty() ) {
                case batt_percentage:
                    setEnableControls( true );
                    setBattIcon( evnt.getmValue() );
                    break;
                case none:
                    runOnUiThread(  new Runnable() {
                        @Override
                        public void run() {
                            mBattIcon.setImageResource( R.drawable.ic_battery_unknown_black_24dp );
                            mBattPwrValue.setText( getString( R.string.msg_batt_unknow ) );
                        }
                    });
                    break;
            }
        }
    };

    private final IEventHandler mOnEventIgnored = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            EventIgnored evnt = ( EventIgnored ) event;

            switch ( evnt.getIgnoredEvent().getEventId() ) {
                case EventSetProperty.id:
                    PostToast( getString( R.string.msg_server_busy ) );
                    EventSetProperty e = ( EventSetProperty ) evnt.getIgnoredEvent();
                    switch ( e.getPropertyId() ) {
                        case Mower_event_ids.property_ids.blade_rpm:
                            mBtnSwitchCut.setChecked( mLastSwitchValue );
                            break;
                        case Mower_event_ids.property_ids.blade_height_mm:
                            mBladeHeight.setProgress( mLastBladeHeight );
                            break;
                    }
                    break;
            }
            mContentView.invalidate();
        }
    };

    private final IEventHandler mOnOk = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            mLastBladeHeight = mBladeHeight.getLeftSeekBar().getProgress();
            mLastSwitchValue = mBtnSwitchCut.isChecked();
        }
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread arg0, Throwable arg1) {
                EventDispatcher.getInstance().riseEvent( new EventFatalError() );
                android.os.Process.killProcess( android.os.Process.myPid() );
            }
        });

        setContentView( R.layout.activity_fullscreen );

        mContentView = findViewById( R.id.fullscreen_content );
        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                if ( mActionBar.isShowing() ) {
                    // Hide UI first
                    if ( mActionBar != null ) {
                        mActionBar.hide();
                    }

                    // Schedule a runnable to remove the status and navigation bar after a delay
                    mHideHandler.removeCallbacks( mShowPart2Runnable );
                    mHideHandler.postDelayed( mHidePart2Runnable, UI_ANIMATION_DELAY );

                } else {
                    // Show the system bar
                    mContentView.setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION );

                    // Schedule a runnable to display UI elements after a delay
                    mHideHandler.removeCallbacks( mHidePart2Runnable );
                    mHideHandler.postDelayed( mShowPart2Runnable, UI_ANIMATION_DELAY );
                }
            }
        });

        mBtnSwitchCut = findViewById(R.id.button_switch_cut);
        mBattIcon = findViewById( R.id.battery_info_icon );
        mBattPwrValue = findViewById( R.id.batt_info_value );
        mLastSwitchValue = mBtnSwitchCut.isChecked();
        mJstckMoveVehicle = findViewById( R.id.joystickView );
        mActionBar = getSupportActionBar();
        mBladeHeight = findViewById( R.id.seekbar_set_height);
        mLastBladeHeight = mBladeHeight.getMaxProgress();
        mBladeHeight.setProgress( mLastBladeHeight );
        mSeekDescription = findViewById( R.id.seek_bar_description );
    }

    @Override
    protected void onPostCreate( Bundle savedInstanceState ) {
        super.onPostCreate( savedInstanceState );

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide( 100 );
    }

    private void PostToast( final String message ) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText( getBaseContext(), message, Toast.LENGTH_LONG ).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        setEnableControls(false);

        mJstckMoveVehicle.setOnMoveListener(new JoystickView.OnMoveListener()
        {
            public void onMove( int angle, int strength ) {
                EventMove.Direction move_dir = EventMove.Direction.forward;

                if ( angle > 340 || angle <= 20 )
                    move_dir = EventMove.Direction.right;
                if ( angle > 20 && angle <= 70 )
                    move_dir = EventMove.Direction.fr;
                /* Not check this because already set.
                if ( angle > 70 && angle <= 110 )
                    move_dir = EventMove.Direction.forward;*/
                if ( angle > 110 && angle <= 160 )
                    move_dir = EventMove.Direction.fl;
                if ( angle > 160 && angle <= 200 )
                    move_dir = EventMove.Direction.left;
                if ( angle > 200 && angle <= 250 )
                    move_dir = EventMove.Direction.bl;
                if ( angle > 250 && angle <= 290 )
                    move_dir = EventMove.Direction.backward;
                if ( angle > 290 && angle <= 340 )
                    move_dir = EventMove.Direction.br;

                EventDispatcher.getInstance().riseEvent( new EventMove( move_dir, strength ) );
            }
        }, 300);

        mBtnSwitchCut.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                int rpm = 0;

                if ( isChecked ) {
                    SharedPreferences shared_pref =
                            PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
                    try {
                        rpm = Integer.parseInt(shared_pref.getString(
                                getString( R.string.pref_blade_speed_key ),
                                getString( R.string.pref_blade_default_speed ) ) );
                    } catch (NumberFormatException nfe) {
                        Log.e( LOG_TAG, nfe.getMessage() );
                        rpm = 3200;
                    }
                }
                EventSetProperty event =
                        new EventSetProperty( EventSetProperty.Properties.blade_rpm, rpm );
                EventDispatcher.getInstance().riseEvent( event );
            }
        });

        mBladeHeight.setIndicatorTextDecimalFormat("0.0");
        mBladeHeight.setOnRangeChangedListener(new OnRangeChangedListener() {
            float height = 0;

            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                height = leftValue;
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                EventSetProperty event =
                        new EventSetProperty( EventSetProperty.Properties.blade_height_mm, height );
                EventDispatcher.getInstance().riseEvent( event );
            }
        });
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
            // Hide UI first
            if ( mActionBar != null ) {
                mActionBar.hide();
            }

            // Schedule a runnable to remove the status and navigation bar after a delay
            mHideHandler.removeCallbacks( mShowPart2Runnable );
            mHideHandler.postDelayed( mHidePart2Runnable, UI_ANIMATION_DELAY );
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
            Intent intent = new Intent(FullscreenActivity.this,
                    SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected( item );
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, MowerControlService.class);
        startService(intent);


        EventDispatcher.getInstance().addEventListener( EventConnected.id, mOnConnected );
        EventDispatcher.getInstance().addEventListener( EventDisconnected.id, mOnDisconnected );
        EventDispatcher.getInstance().addEventListener( EventPropertyReturn.id, mOnPropertyReturn );
        EventDispatcher.getInstance().addEventListener( EventBattCharging.id, mOnCharge );
        EventDispatcher.getInstance().addEventListener( EventLowBatt.id, mOnLowBatt );
        EventDispatcher.getInstance().addEventListener( EventIgnored.id, mOnEventIgnored );
        EventDispatcher.getInstance().addEventListener( EventOk.id, mOnOk );

        // TODO: Add functionality to read settings value for enabling video feed.

        SharedPreferences shared_pref =
                PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

        if ( shared_pref.getBoolean( getString( R.string.pref_mirror_controls_key ),
                                    false) ) {
            ((FrameLayout.LayoutParams) mBtnSwitchCut.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) mBladeHeight.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) mSeekDescription.getLayoutParams()).gravity =
                    ( Gravity.TOP | Gravity.START );

            ((FrameLayout.LayoutParams) mJstckMoveVehicle.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );

        } else {
            ((FrameLayout.LayoutParams) mJstckMoveVehicle.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.START );

            ((FrameLayout.LayoutParams) mBtnSwitchCut.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );

            ((FrameLayout.LayoutParams) mBladeHeight.getLayoutParams()).gravity =
                    ( Gravity.BOTTOM | Gravity.END );
            ((FrameLayout.LayoutParams) mSeekDescription.getLayoutParams()).gravity =
                    ( Gravity.TOP | Gravity.END );
        }

        this.mContentView.invalidate();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Intent intent = new Intent(this, MowerControlService.class);
        stopService(intent);

        EventDispatcher.getInstance().removeEventListener( mOnConnected );
        EventDispatcher.getInstance().removeEventListener( mOnDisconnected );
        EventDispatcher.getInstance().removeEventListener( mOnPropertyReturn );
        EventDispatcher.getInstance().removeEventListener( mOnCharge );
        EventDispatcher.getInstance().removeEventListener( mOnLowBatt );
        EventDispatcher.getInstance().removeEventListener( mOnEventIgnored );
        EventDispatcher.getInstance().removeEventListener( mOnOk );
    }

    @Override
    protected void onDestroy() {
       super.onDestroy();

       EventDispatcher.getInstance().removeAllListeners();
    }

   private void setEnableControls(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( enabled ) {
                    mBtnSwitchCut.setEnabled( true );
                    mJstckMoveVehicle.setEnabled( true );
                    mJstckMoveVehicle.setButtonColor(
                            getResources().getColor( R.color.colorControlEnabled ) );
                    mJstckMoveVehicle.setBorderColor(
                            getResources().getColor( R.color.colorControlEnabled ) );
                    mBladeHeight.setEnabled( true );
                    mBladeHeight.setStepsColor(
                            getResources().getColor( R.color.colorControlEnabled ) );

                } else {
                    mBtnSwitchCut.setChecked( false );
                    mBtnSwitchCut.setEnabled( false );
                    mJstckMoveVehicle.setEnabled( false );
                    mJstckMoveVehicle.setButtonColor(
                            getResources().getColor( R.color.colorControlDisabled ) );
                    mJstckMoveVehicle.setBorderColor(
                            getResources().getColor( R.color.colorControlDisabled ) );
                    mBladeHeight.setEnabled( false );
                    mBladeHeight.setStepsColor(
                            getResources().getColor( R.color.colorControlDisabled ) );
                }
            }
        });
    }

    private void resetControls(){
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                if ( mBtnSwitchCut.isChecked() )
                    mBtnSwitchCut.setChecked( false );

                mJstckMoveVehicle.resetButtonPosition();
                mBladeHeight.setProgress( mBladeHeight.getMaxProgress() );
                mContentView.invalidate();
            }
        } );
    }

    private void setBattIcon( final int value ) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                mBattPwrValue.setText( value + "%" );

                if ( value <= 10 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_alert_red_24dp );
                else if ( value < 25 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_20_black_24dp );
                else if ( value < 35 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_30_black_24dp );
                else if ( value < 55 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_50_black_24dp );
                else if ( value < 65 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_60_black_24dp );
                else if ( value < 85 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_80_black_24dp );
                else if ( value < 95 )
                    mBattIcon.setImageResource( R.drawable.ic_battery_90_black_24dp );
                else
                    mBattIcon.setImageResource( R.drawable.ic_battery_full_black_24dp );
            }
        } );
    }
}

