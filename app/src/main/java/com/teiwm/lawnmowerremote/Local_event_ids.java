package com.teiwm.lawnmowerremote;

public class Local_event_ids {
    // LM events 10000 to 32767 is for communication events.

    public enum tcp_event_ids {
        send ( 10000 ),
        disconnected ( 10001 ),
        connected ( 10002 ),
        response ( 10003 );

        private int mEventID ;

        tcp_event_ids( int event ) {
            this.mEventID = event;
        }

        public int getID( ) { return this.mEventID; }
    }
}
