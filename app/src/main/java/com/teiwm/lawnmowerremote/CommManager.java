package com.teiwm.lawnmowerremote;

// Lawn mower event forwaring class
public class CommManager {
    private static String LOG_TAG = "RPI events forwarder";

    // Prevent sending same data over and over again.
    private int mLastMoveDir = 0;
    private int mLastMoveStrength = 0;

    public void Move ( int angle, int strength ){

        int move_dir = Mower_event_ids.move_event_ids.forward.getID();

        if ( angle >= 337 || angle <= 22 )
            move_dir = Mower_event_ids.move_event_ids.right.getID();
        if ( angle > 22 && angle <= 67 )
            move_dir = Mower_event_ids.move_event_ids.front_right.getID();

        /* Skip this check, because is default value
           if ( angle > 67 && angle <= 112 )
              move_dir = Mower_events_ids.Move_event_ids.forward;
         */
        if ( angle > 112 && angle <= 157 )
            move_dir = Mower_event_ids.move_event_ids.front_left.getID();
        if ( angle > 157 && angle <= 202 )
            move_dir = Mower_event_ids.move_event_ids.left.getID();
        if ( angle > 202 && angle <= 247 )
            move_dir = Mower_event_ids.move_event_ids.back_left.getID();
        if ( angle > 247 && angle <= 292 )
            move_dir = Mower_event_ids.move_event_ids.backward.getID();
        if ( angle > 292 && angle <= 337 )
            move_dir = Mower_event_ids.move_event_ids.back_right.getID();

        if ( ( mLastMoveDir == move_dir ) && ( mLastMoveStrength == strength ) )
            return;

        mLastMoveDir = move_dir;
        mLastMoveStrength = strength;

        String event_data = move_dir + " " + strength;
        Event event = new Event( Local_event_ids.tcp_event_ids.send.getID(),
                    event_data );

        EventDispatcher.getInstance().dispatchEvent( event );
    }

    void RunBlade( boolean enable ) {
        String event_data = Mower_event_ids.general_event_ids.propety_set.getID() + " "
                + Mower_event_ids.property_event_ids.blade_run.getID() + " " + enable;
        Event event = new Event( Local_event_ids.tcp_event_ids.send.getID(),
                event_data );

        EventDispatcher.getInstance().dispatchEvent( event );
    }
}
