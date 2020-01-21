package com.teiwm.lawnmowerremote;

public class Mower_event_ids {
    // LM events 0000 to 10000 is for communication events.
    public enum general_event_ids {
        propety_set ( 21 ),
        property_get ( 20 ),
        move( 30 ),
        shutdown ( 10000 );

        private int mEventID ;

        general_event_ids( int event ) {
            this.mEventID = event;
        }

        public int getID( ) { return this.mEventID; }
    }

    public enum server_response_ids {
        ok ( 8 ),
        fatal_error ( 10 ),
        property_unknow ( 11 ),
        property_return ( 12 );

        private int mEventID ;

        server_response_ids( int event ) {
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
        blade_height ( 202 ),
        current( 203 ),
        voltage( 204 );

        private int mEventID ;

        property_event_ids( int direction ) {
            this.mEventID = direction;
        }

        public int getID( ) { return this.mEventID; }
    }
    // End of LM events.
}
