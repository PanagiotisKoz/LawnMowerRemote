package com.teiwm.lawnmowerremote;

public interface IEventDispatcher {

    void addEventListener( int type, IEventHandler cbInterface );
    void removeEventListener( IEventHandler handler );
    void dispatchEvent( Event event );
    void removeAllListeners();

}
