package com.teiwm.lawnmowerremote;

public class Local_event_ids {
    // LM events 10000 to 32767 is for communication events.

    public class tcp_event_ids {
        public static final int send = 10000;
        public static final int disconnected = 10001;
        public static final int connected = 10002;
        public static final int response = 10003;
    }
}
