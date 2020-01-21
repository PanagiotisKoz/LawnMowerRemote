package com.teiwm.lawnmowerremote;

public class Listener {
    private IEventHandler handler;

    public Listener(  IEventHandler handler ) {
        this.handler = handler;
    }

    public IEventHandler getHandler() {
        return this.handler;
    }
}
