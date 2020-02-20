package com.teiwm.lawn_mower_remote;

class Event {
    private final int mEventId;

    Event( int type ) {
        mEventId = type;
    }

    public int getEventId() {
        return mEventId;
    }

    String serialize() { return  mEventId + " "; }
}

class EventConnected extends Event {
    public static final int id = Local_event_ids.connected;
    public EventConnected ( ) {
        super( id );
    }
}

class EventDisconnected extends Event {
    public static final int id = Local_event_ids.disconnected;

    public EventDisconnected ( ) {
        super( id );
    }
}

class EventOk extends Event {
    public static final int id = Mower_event_ids.mower_response_ids.ok;

    public EventOk ( ) {
        super( id );
    }
}

class EventFatalError extends Event {
    public static final int id = Local_event_ids.event_fatal_error;

    public EventFatalError ( ) {
        super( id );
    }
}

class EventIgnored extends Event {
    public static final int id = Local_event_ids.event_ignored;
    private final Event mEvent;
    public EventIgnored ( Event ignored_event )
    {
        super( id );
        mEvent = ignored_event;
    }

    Event getIgnoredEvent() { return mEvent; }
}

class EventMove extends Event {
    public static final int id = Mower_event_ids.general_ids.move;
    private final Direction mDirection;
    private final int mPower;

    public enum Direction {
        forward,
        backward,
        left,
        right,
        fr,
        fl,
        br,
        bl
    }

    public EventMove ( Direction direction, int power ) {
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

    public int getPower(){ return mPower; }
    public Direction getDirection() { return mDirection; }

    @Override
    public String serialize() {
        return super.serialize() + getDirectionId() + " " + mPower;
    }
}

class EventSetProperty extends Event {
    public static final int id = Mower_event_ids.general_ids.property_set;
    public enum Properties {
        blade_rpm,
        blade_height_mm,
        camera_on
    }

    private final Properties mProperty;
    private final Object mValue;

    public EventSetProperty ( Properties property, Object value ) {
        super( id );
        mProperty = property;
        mValue = value;
    }

    public int getPropertyId() {
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
    public String serialize() {
        return super.serialize() + getPropertyId() + " " + mValue;
    }
}

class EventGetProperty extends Event {
    private static final int id = Mower_event_ids.general_ids.property_get;
    public enum Properties {
        blade_rpm,
        blade_height_mm,
        camera_on,
        current,
        voltage,
        power
    }

    private final Properties mProperty;

    public EventGetProperty ( Properties property ) {
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
        }
        return propertyId;
    }

    @Override
    public String serialize() {
        return super.serialize() + getPropertyId();
    }
}