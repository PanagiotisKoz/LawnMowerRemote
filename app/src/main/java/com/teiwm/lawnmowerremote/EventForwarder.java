package com.teiwm.lawnmowerremote;

import android.os.ConditionVariable;
import android.util.Log;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.StringTokenizer;

// Lawn mower event forwarder helper class
public class EventForwarder {
    private static String LOG_TAG = "RPI events forwarder";

    // Prevent sending same data over and over again.
    private int mLastMoveDir = 0;
    private int mLastMoveStrength = 0;
    private ConditionVariable m_ready_to_send = new ConditionVariable();
    private Thread m_event_forwarder = new Thread(new  ForwardEvent() );
    private Queue< String > m_event_queue = new LinkedList<>();

    EventForwarder() {
        EventDispatcher.getInstance().addEventListener(
                Local_event_ids.tcp_event_ids.response,
                new IEventHandler() {
                    @Override
                    public void callback(Event event) {
                        if ( event.getParams() != null ) {
                            AnalyzeResponse( event.getParams().toString() );
                        }
                    }
                });
        m_event_forwarder.start();
    }

    private class ForwardEvent implements Runnable {
        @Override
        public void run() {
            while ( !Thread.currentThread().isInterrupted() ) {
                m_ready_to_send.block();
                if (!m_event_queue.isEmpty()) {
                    Event event = new Event(Local_event_ids.tcp_event_ids.send,
                            m_event_queue.poll() );
                    EventDispatcher.getInstance().dispatchEvent(event);
                    m_ready_to_send.close();
                }
            }
        }
    }

    private void AnalyzeResponse( String response ) {
        try {
            StringTokenizer tokens = new StringTokenizer( response );
            switch ( Integer.parseInt( tokens.nextToken().trim() ) ) {
                case Mower_event_ids.server_response_ids.ok:
                    m_ready_to_send.open();
                    break;
            }
        } catch ( NumberFormatException nfe ) {
            Log.d( LOG_TAG,"Could not parse " + nfe );
        }
    }

    public void Move ( int angle, int strength ){

        int move_dir = Mower_event_ids.move_event_ids.forward;

        if ( angle >= 337 || angle <= 22 )
            move_dir = Mower_event_ids.move_event_ids.right;
        if ( angle > 22 && angle <= 67 )
            move_dir = Mower_event_ids.move_event_ids.front_right;

        /* Skip this check, because is default value
           if ( angle > 67 && angle <= 112 )
              move_dir = Mower_events_ids.Move_event_ids.forward;
         */
        if ( angle > 112 && angle <= 157 )
            move_dir = Mower_event_ids.move_event_ids.front_left;
        if ( angle > 157 && angle <= 202 )
            move_dir = Mower_event_ids.move_event_ids.left;
        if ( angle > 202 && angle <= 247 )
            move_dir = Mower_event_ids.move_event_ids.back_left;
        if ( angle > 247 && angle <= 292 )
            move_dir = Mower_event_ids.move_event_ids.backward;
        if ( angle > 292 && angle <= 337 )
            move_dir = Mower_event_ids.move_event_ids.back_right;

        if ( ( mLastMoveDir == move_dir ) && ( mLastMoveStrength == strength ) )
            return;

        mLastMoveDir = move_dir;
        mLastMoveStrength = strength;
        m_event_queue.add( move_dir + " " + strength );
    }

    void RunBlade( boolean enable ) {
        String properties = Mower_event_ids.general_event_ids.propety_set + " "
                + Mower_event_ids.property_event_ids.blade_run + " " + enable;
        Event event = new Event( Local_event_ids.tcp_event_ids.send, properties );

        EventDispatcher.getInstance().dispatchEvent( event );
    }

    void SetBladeHeight( float height ){
        String msg = Mower_event_ids.general_event_ids.propety_set + " "
                + Mower_event_ids.property_event_ids.blade_height + " "
                + String.format( Locale.getDefault(), "%.1f", height);
        m_event_queue.add( msg );
    }
}
