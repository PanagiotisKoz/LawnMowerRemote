package com.teiwm.lawn_mower_remote;

class Listener {
    private final IEventHandler handler;

    public Listener(  IEventHandler handler ) {
        this.handler = handler;
    }

    public IEventHandler getHandler() {
        return this.handler;
    }
}
