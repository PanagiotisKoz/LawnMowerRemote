package com.teiwm.lawn_mower_remote;

class Event {
    private final int mEventId;

    Event( int type ) {
        mEventId = type;
    }

    int getEventId() {
        return mEventId;
    }

    String serialize() { return  mEventId + " "; }
}

class EventConnected extends Event {
    static final int id = Local_event_ids.connected;
    EventConnected ( ) {
        super( id );
    }
}

class EventDisconnected extends Event {
    static final int id = Local_event_ids.disconnected;

    EventDisconnected ( ) {
        super( id );
    }
}

class EventOk extends Event {
    static final int id = Mower_event_ids.mower_response_ids.ok;

    EventOk ( ) {
        super( id );
    }
}

class EventLowBatt extends Event {
    static final int id = Mower_event_ids.mower_response_ids.low_batt_alert;

    EventLowBatt ( ) {
        super( id );
    }
}

class EventBattCharging extends Event {
    static final int id = Mower_event_ids.mower_response_ids.batt_charging_alert;

    EventBattCharging ( ) {
        super( id );
    }
}

class EventFatalError extends Event {
    static final int id = Local_event_ids.event_fatal_error;

    EventFatalError ( ) {
        super( id );
    }
}

class EventIgnored extends Event {
    static final int id = Local_event_ids.event_ignored;
    private final Event mEvent;
    EventIgnored ( Event ignored_event )
    {
        super( id );
        mEvent = ignored_event;
    }

    Event getIgnoredEvent() { return mEvent; }
}

class EventMove extends Event {
    static final int id = Mower_event_ids.general_ids.move;
    private final Direction mDirection;
    private final int mPower;

    enum Direction {
        forward,
        backward,
        left,
        right,
        fr,
        fl,
        br,
        bl
    }

    EventMove ( Direction direction, int power ) {
        super( id );
        mDirection = direction;
        mPower = power;
    }

    private int getDirectionId() {
        int id = 0;
        switch ( mDirection ){
            case forward:
                id = Mower_event_ids.move_ids.forward;
                break;
            case backward:
                id = Mower_event_ids.move_ids.backward;
                break;
            case left:
                id = Mower_event_ids.move_ids.left;
                break;
            case right:
                id = Mower_event_ids.move_ids.right;
                break;
            case fr:
                id = Mower_event_ids.move_ids.fr;
                break;
            case fl:
                id = Mower_event_ids.move_ids.fl;
                break;
            case br:
                id = Mower_event_ids.move_ids.br;
                break;
            case bl:
                id = Mower_event_ids.move_ids.bl;
                break;
        }
        return id;
    }

    int getPower(){ return mPower; }
    Direction getDirection() { return mDirection; }

    @Override
    String serialize() {
        return super.serialize() + getDirectionId() + " " + mPower;
    }
}

class EventSetProperty extends Event {
    static final int id = Mower_event_ids.general_ids.property_set;
    enum Properties {
        blade_rpm,
        blade_height_mm,
        camera_on
    }

    private final Properties mProperty;
    private final Object mValue;

    EventSetProperty ( Properties property, Object value ) {
        super( id );
        mProperty = property;
        mValue = value;
    }

    int getPropertyId() {
        int propertyId = 0;

        switch ( mProperty ) {
            case blade_rpm:
                propertyId = Mower_event_ids.property_ids.blade_rpm;
                break;
            case blade_height_mm:
                propertyId = Mower_event_ids.property_ids.blade_height_mm;
                break;
            case camera_on:
                propertyId = Mower_event_ids.property_ids.camera_on;
                break;
        }
        return propertyId;
    }

    @Override
    String serialize() {
        return super.serialize() + getPropertyId() + " " + mValue;
    }
}

class EventGetProperty extends Event {
    static final int id = Mower_event_ids.general_ids.property_get;
    enum Properties {
        blade_rpm,
        blade_height_mm,
        camera_on,
        current,
        voltage,
        power,
        batt_percentage,
        temp
    }

    private final Properties mProperty;

    EventGetProperty ( Properties property ) {
        super( id );
        mProperty = property;
    }

    private int getPropertyId() {
        int propertyId = 0;

        switch ( mProperty ) {
            case blade_rpm:
                propertyId = Mower_event_ids.property_ids.blade_rpm;
                break;
            case blade_height_mm:
                propertyId = Mower_event_ids.property_ids.blade_height_mm;
                break;
            case camera_on:
                propertyId = Mower_event_ids.property_ids.camera_on;
                break;
            case current:
                propertyId = Mower_event_ids.property_ids.current;
                break;
            case voltage:
                propertyId = Mower_event_ids.property_ids.voltage;
                break;
            case power:
                propertyId = Mower_event_ids.property_ids.power;
                break;
            case batt_percentage:
                propertyId = Mower_event_ids.property_ids.batt_percentage;
                break;
            case temp:
                propertyId = Mower_event_ids.property_ids.temp;
                break;
        }
        return propertyId;
    }

    @Override
    String serialize() {
        return super.serialize() + getPropertyId();
    }
}

class EventPropertyReturn extends Event {
    static final int id = Mower_event_ids.mower_response_ids.property_return;
    enum Properties {
        blade_rpm,
        blade_height_mm,
        camera_on,
        current,
        voltage,
        power,
        batt_percentage,
        none
    }

    private final Properties mProperty;
    private final int mValue;

    EventPropertyReturn ( Properties property, int value ) {
        super( id );
        mProperty = property;
        mValue = value;
    }

    EventPropertyReturn( int property, int value ) {
        super( id );
        mValue = value;

        switch ( property ) {
            case Mower_event_ids.property_ids.blade_rpm:
                mProperty = Properties.blade_rpm;
                break;
            case Mower_event_ids.property_ids.blade_height_mm:
                mProperty = Properties.blade_height_mm;
                break;
            case Mower_event_ids.property_ids.camera_on:
                mProperty = Properties.camera_on;
                break;
            case Mower_event_ids.property_ids.current:
                mProperty = Properties.current;
                break;
            case Mower_event_ids.property_ids.voltage:
                mProperty = Properties.voltage;
                break;
            case Mower_event_ids.property_ids.power:
                mProperty = Properties.power;
                break;
            case Mower_event_ids.property_ids.batt_percentage:
                mProperty = Properties.batt_percentage;
                break;
            default:
                mProperty = Properties.none;
        }
    }

    Properties getProperty() { return mProperty; }
    int getmValue() { return mValue; }
}