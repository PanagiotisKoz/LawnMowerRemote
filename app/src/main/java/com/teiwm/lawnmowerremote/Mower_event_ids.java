package com.teiwm.lawnmowerremote;

public class Mower_event_ids {
    // LM events 0000 to 10000 is for communication events.
    public class general_event_ids {
        public static final int propety_set = 21;
        public static final int property_get = 20;
        public static final int shutdown = 10000;
    }

    public class server_response_ids {
        public static final int ok = 8;
        public static final int fatal_error = 10;
        public static final int property_unknow = 11;
        public static final int property_return = 12;
    }

    public class move_event_ids {
        public static final int forward = 100;
        public static final int backward = 101;
        public static final int left = 102;
        public static final int right = 103;
        public static final int front_right = 104;
        public static final int front_left = 105;
        public static final int  back_right = 106;
        public static final int back_left = 107 ;
    }

    public class property_event_ids {
        public static final int blade_run = 200;
        public static final int camera_on = 201;
        public static final int blade_height = 202;
        public static final int current = 203;
        public static final int voltage = 204;
    }
    // End of LM events.
}
