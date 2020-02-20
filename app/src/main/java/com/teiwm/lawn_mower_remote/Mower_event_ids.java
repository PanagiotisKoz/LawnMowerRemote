package com.teiwm.lawn_mower_remote;

class Mower_event_ids {
    // LM events 0000 to 10000 is for communication events.
    public class general_ids {
        public static final int property_get = 30;
        public static final int property_set = 31;
        public static final int move = 40;
    }

    public class mower_response_ids {
        public static final int ok = 8;
        public static final int command_unknow = 11;
        public static final int property_unknow = 12;
        public static final int property_return = 13;
    }

    public class move_ids {
        public static final int forward = 100;
        public static final int backward = 101;
        public static final int left = 102;
        public static final int right = 103;
        public static final int fr = 104;
        public static final int fl = 105;
        public static final int br = 106;
        public static final int bl = 107;
    }

    public class property_ids {
        public static final int blade_rpm = 200;
        public static final int blade_height_mm = 201;
        public static final int camera_on = 210;
        public static final int current = 220;
        public static final int voltage = 221;
        public static final int power = 222;
        public static final int temp = 230;
    }
    // End of LM events.
}
