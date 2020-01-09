package com.teiwm.lawnmowerremote;

public class Listener {
    private int type;
    private IEventHandler handler;

    public Listener( int type, IEventHandler handler ) {
        this.type = type;
        this.handler = handler;
    }

    public int getType() {
        return this.type;
    }

    public IEventHandler getHandler() {
        return this.handler;
    }
}
