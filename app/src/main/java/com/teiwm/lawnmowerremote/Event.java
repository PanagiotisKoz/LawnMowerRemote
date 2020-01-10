package com.teiwm.lawnmowerremote;

public class Event {
    private int EventId = 0;
    private Object params;

    public Event( int type, Object params ) {
        EventId = type;
        this.params = params;
    }

    public int getEventId() {
        return EventId;
    }

    public Object getParams() {
        return this.params;
    }
}
