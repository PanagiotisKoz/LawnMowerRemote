package com.teiwm.lawnmowerremote;

public class Mower_event_ids {
    // LM events 0000 to 10000 is for communication events.
    public enum general_event_ids {
        shutdown ( 10 ),
        propety_set ( 30 ),
        property_get ( 31 ),
        fatal_error ( 10000 );

        private int mEventID ;

        general_event_ids( int event ) {
            this.mEventID = event;
        }

        public int getID( ) { return this.mEventID; }
    }

    public enum move_event_ids {
        forward ( 100 ),
        backward ( 101 ),
        left ( 102),
        right (103),
        front_right (104 ),
        front_left ( 105 ),
        back_right ( 106 ),
        back_left (107 );

        private int mEventID ;

        move_event_ids( int direction ) {
            this.mEventID = direction;
        }

        public int getID( ) { return this.mEventID; }
    }

    public enum property_event_ids {
        blade_run ( 200 ),
        camera_on ( 201 ),
        blade_height ( 202 );

        private int mEventID ;

        property_event_ids( int direction ) {
            this.mEventID = direction;
        }

        public int getID( ) { return this.mEventID; }
    }
    // End of LM events.
}
