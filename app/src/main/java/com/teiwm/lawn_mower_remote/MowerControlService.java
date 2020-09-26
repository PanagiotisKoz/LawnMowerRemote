package com.teiwm.lawn_mower_remote;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.StringTokenizer;

public class MowerControlService extends Service {
    private final String mNotifyChannelId = "Mower_notify_channel_01";
    private final String LOG_TAG = "Mower service";

    // Prevent sending same data over and over again.
    private final TCPClient mClient = new TCPClient();
    private boolean mServerOk = true; // If server is up and execute command without errors is true.
    private Thread mConnectivityThread;
    private String mLastCommand = "";

    private final IEventHandler mOnMove = new IEventHandler() {
        @Override
        public void callback(Event event) {
            EventMove evnt = ( EventMove ) event;

            if ( evnt.getPower() == 0 )
                mLastCommand = "";

            if ( !SendRemoteCmd( evnt.serialize() ) ) {
                EventDispatcher.getInstance().riseEvent( new EventIgnored( event ) );
            }
        }
    };

    private final IEventHandler mOnSetProperty = new IEventHandler() {
        @Override
        public void callback(Event event) {
            EventSetProperty evnt = ( EventSetProperty ) event;

            if ( !SendRemoteCmd( evnt.serialize() ) ) {
                EventDispatcher.getInstance().riseEvent( new EventIgnored( event ) );
            }
        }
    };

    private final IEventHandler mOnGetProperty = new IEventHandler() {
        @Override
        public void callback(Event event) {
            EventGetProperty evnt = ( EventGetProperty ) event;

            mLastCommand = "";
            if ( !SendRemoteCmd( evnt.serialize() ) ) {
                EventDispatcher.getInstance().riseEvent( new EventIgnored( event ) );
            }
        }
    };

    private final IEventHandler mOnFatalError = new IEventHandler() {
        @Override
        public void callback( Event event ) {
            onDestroy();
            stopSelf();
        }
    };

    private boolean SendRemoteCmd( String command ) {
        if ( !mClient.isConnected() || !mServerOk )
            return false;

        if ( mLastCommand.equals( command ) || command.equals( "" ) )
            return false;

        mClient.send( command );
        mLastCommand = command;
        mServerOk = false;
        return true;
    }

    private final BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive( Context context, Intent intent ) {
            int WifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN);
            switch ( WifiStateExtra ) {
                case WifiManager.WIFI_STATE_ENABLED:
                    mConnectivityThread.start();
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    if ( mConnectivityThread.isAlive() )
                        mConnectivityThread.interrupt();
                    mClient.disconnect();
                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                    PostToast( getString(R.string.msg_wifi_not_connected) );
                    break;
            }
        }
    };

    private class DetectConnectivity implements Runnable {

        final ConnectivityManager cm
                = (ConnectivityManager) getSystemService( CONNECTIVITY_SERVICE );

        final SharedPreferences shared_pref =
                PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        final String ip = shared_pref.getString(
                getString( R.string.pref_ip_key ),
                getString( R.string.pref_default_ip_value ) );
        int port;

        private DetectConnectivity() {
            try {
                port = Integer.parseInt( shared_pref.getString(
                        getString( R.string.pref_port_key ),
                        getString( R.string.pref_default_port_value ) ) );
            } catch (NumberFormatException nfe) {
                Log.e( LOG_TAG, nfe.getMessage() );
                port = 8080;
            }
        }

        @Override
        public void run() {
            boolean wifiConnected = false;
            while ( !Thread.currentThread().isInterrupted() && !wifiConnected) {
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiConnected = true;
                    }
                }
                else {
                    try {
                        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                        wifiConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                    }
                    catch ( Exception e ) {
                        Log.e(LOG_TAG, e.getMessage() );
                        return;
                    }
                }
                try {
                    Thread.sleep( 2000 );
                } catch ( InterruptedException e ) {
                    Log.e( LOG_TAG, e.getMessage() );
                    return;
                }
            }
            mClient.connect(ip, port);
        }
    }

    private void HandleResponse( String response ) {
        if ( response.isEmpty() )
            return;

        StringTokenizer tokens = new StringTokenizer( response, " ", false );
        int mowerState;

        mowerState = Integer.valueOf( tokens.nextToken() );

        mServerOk = true;
        Event event = new Event( -1 );
        switch( mowerState ) {
            case EventOk.id:
                event =  new EventOk();
                break;
            case Mower_event_ids.mower_response_ids.command_unknow:
            case Mower_event_ids.mower_response_ids.property_unknow:
                Log.e( LOG_TAG, "Protocol error. Last command was: " + mLastCommand );
                break;
            case EventPropertyReturn.id:
                if ( tokens.countTokens() == 2 ) {
                    int propertyId = Integer.valueOf( tokens.nextToken() );
                    int value = Integer.valueOf( tokens.nextToken() );
                    event = new EventPropertyReturn( propertyId, value );
                }
                else
                    Log.e( LOG_TAG, "Protocol error. response was: " + response );
                break;
            case EventLowBatt.id:
                event = new EventLowBatt();
                break;
            case EventBattCharging.id:
                event = new EventBattCharging();
                break;
        }
        if ( event.getEventId() != -1 )
            EventDispatcher.getInstance().riseEvent( event );
    }

    private void postNotify( boolean isConnected) {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ) {
            CharSequence name = getString( R.string.notification_channel_name );
            String description = getString( R.string.notification_channel_description );
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel( mNotifyChannelId, name, importance );
            channel.setDescription( description );
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder( this, mNotifyChannelId );
        builder.setContentTitle( LOG_TAG )
                .setPriority( NotificationCompat.PRIORITY_DEFAULT );
        if ( isConnected )
            builder.setSmallIcon( R.drawable.ic_stat_connected )
                    .setContentText( "Connected." );
        else
            builder.setSmallIcon( R.drawable.ic_stat_disconnected )
                    .setContentText( "Disconnected." );


        NotificationManagerCompat nm = NotificationManagerCompat.from( this );
        nm.notify( 1, builder.build());
    }

    private void PostToast( final String message ) {
        new Handler(Looper.getMainLooper()).post( new Runnable() {
            @Override
            public void run() {
                Toast toast1 = Toast.makeText( getBaseContext(), message, Toast.LENGTH_LONG );
                toast1.show();
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public void onCreate() {
        IntentFilter intentFilter = new IntentFilter( WifiManager.WIFI_STATE_CHANGED_ACTION );
        registerReceiver( mWifiStateReceiver, intentFilter);

        DetectConnectivity connectivity = new DetectConnectivity();
        mConnectivityThread = new Thread( connectivity );

        mClient.SetOnConnectListener(new TCPClient.OnConnectListener() {
            @Override
            public void onConnect() {
                postNotify( true );
                EventDispatcher.getInstance().riseEvent( new EventConnected() );
                PostToast( getString( R.string.msg_client_connected ) );
            }
        });

        mClient.SetOnDisconnectListener(new TCPClient.OnDisconnectListener() {
            @Override
            public void onDisconnect(String errorMsg) {
                postNotify( false );
                if ( !errorMsg.isEmpty() ) {
                    EventDispatcher.getInstance().riseEvent( new EventDisconnected() );
                    PostToast( errorMsg );
                }
                else {
                    EventDispatcher.getInstance().riseEvent( new EventDisconnected() );
                }
            }
        });

        mClient.SetOnReceiveListener(new TCPClient.OnReceiveListener() {
            @Override
            public void onReceive( String response ) {
                HandleResponse( response );
            }
        });

        EventDispatcher.getInstance().addEventListener( EventMove.id, mOnMove );
        EventDispatcher.getInstance().addEventListener( EventSetProperty.id, mOnSetProperty );
        EventDispatcher.getInstance().addEventListener( EventGetProperty.id, mOnGetProperty );
        EventDispatcher.getInstance().addEventListener( EventFatalError.id, mOnFatalError );
    }

    @Override
    public void onDestroy() {
        mClient.RemoveOnDisconnectListener();
        mClient.disconnect();
        unregisterReceiver( mWifiStateReceiver );

        EventDispatcher.getInstance().removeEventListener( mOnMove );
        EventDispatcher.getInstance().removeEventListener( mOnSetProperty );
        EventDispatcher.getInstance().removeEventListener( mOnGetProperty );
        EventDispatcher.getInstance().removeEventListener( mOnFatalError );

        NotificationManagerCompat nm = NotificationManagerCompat.from(this);
        nm.deleteNotificationChannel( mNotifyChannelId );
    }
}
