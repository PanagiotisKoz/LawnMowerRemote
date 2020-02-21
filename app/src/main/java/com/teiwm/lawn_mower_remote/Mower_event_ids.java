package com.teiwm.lawn_mower_remote;

class Mower_event_ids {
    // LM events 0000 to 10000 is for communication events.
    class general_ids {
        static final int property_get = 30;
        static final int property_set = 31;
        static final int move = 40;
        static final int batt_charging_alert = 50;
    }

    class mower_response_ids {
        static final int ok = 8;
        static final int command_unknow = 11;
        static final int property_unknow = 12;
        static final int property_return = 13;
    }

    class move_ids {
        static final int forward = 100;
        static final int backward = 101;
        static final int left = 102;
        static final int right = 103;
        static final int stop = 104;
    }

    class property_ids {
        static final int blade_rpm = 200;
        static final int blade_height_mm = 201;
        static final int camera_on = 210;
        static final int current = 220;
        static final int voltage = 221;
        static final int power = 222;
        static final int batt_percentance = 223;
        static final int temp = 230;
    }
    // End of LM events.
}
